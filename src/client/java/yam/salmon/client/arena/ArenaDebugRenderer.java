package yam.salmon.client.arena;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import yam.salmon.Salmon;

/**
 * クライアント側のアリーナデバッグ表示レンダラー。
 * MC 26.2 の StagedVertexBuffer + RenderPipeline API (抽出/描画2フェーズ) を使用する。
 *
 * Stage A: 固定座標の半透明塗りつぶしボックスを表示（デバッグキャッシュ無視）
 */
public class ArenaDebugRenderer {

    private static final ArenaDebugRenderer INSTANCE = new ArenaDebugRenderer();

    private static final double DEBUG_RENDER_DISTANCE = 128.0;

    // ワイヤーフレームの辺の太さ (ブロック単位)
    private static final float EDGE_THICKNESS = 0.02f;

    // マーカー色: 固定
    private static final float MARKER_A_R = 1.0f;
    private static final float MARKER_A_G = 0.0f;
    private static final float MARKER_A_B = 0.0f;
    private static final float MARKER_A_ALPHA = 0.8f;

    private static final float MARKER_B_R = 0.0f;
    private static final float MARKER_B_G = 0.0f;
    private static final float MARKER_B_B = 1.0f;
    private static final float MARKER_B_ALPHA = 0.8f;

    private static final float MARKER_FILL_ALPHA_FACTOR = 0.5f;

    // --- RenderPipeline: 壁越し描画用カスタムパイプライン ---
    // DEBUG_FILLED_SNIPPET = POSITION_COLOR フォーマットのシェーダ (QUADS向け)。
    // withDepthStencilState(Optional.empty()) で深度テスト無効 → 壁越し表示。
    private static final RenderPipeline FILLED_THROUGH_WALLS;

    // --- 動的ユニフォーム書き込み用の共通オブジェクト ---
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    // --- ステージングバッファ ---
    private static final StagedVertexBuffer STAGED_BUFFER;

    static {
        FILLED_THROUGH_WALLS = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                        .withLocation(Salmon.id("pipeline/arena_filled"))
                        .withDepthStencilState(Optional.empty()) // 深度テスト無効 = 壁越し表示
                        .build());
        STAGED_BUFFER = new StagedVertexBuffer(
                () -> "Arena Debug Buffer",
                RenderType.SMALL_BUFFER_SIZE);
    }

    // --- アリーナキャッシュ (Stage C以降で使用) ---
    private final Map<ResourceKey<Level>, Map<UUID, CachedArena>> arenaCache = new ConcurrentHashMap<>();
    private boolean fullSyncInProgress = false;
    private final List<CachedArena> fullSyncBuffer = Collections.synchronizedList(new ArrayList<>());

    // --- 抽出フェーズでキャプチャしたレンダーステート ---
    private ArenaRenderState arenaRenderState;
    private boolean debugEnabled = false;

    // --- 診断用 ---
    private boolean renderInvokedOnce = false;
    private boolean extractInvokedOnce = false;
    private long lastDiagnosticLogTime = 0;

    private ArenaDebugRenderer() {
    }

    public static ArenaDebugRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * マーカーブロックを手に持っているかチェックする。
     */
    private boolean isHoldingMarkerBlock(Minecraft mc) {
        if (mc.player == null) return false;
        return mc.player.getMainHandItem().is(net.minecraft.world.item.ItemStack.EMPTY.getItem())
                ? false
                : mc.player.getMainHandItem().is(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Salmon.id("ink_area_marker")))
                || mc.player.getOffhandItem().is(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Salmon.id("ink_area_marker")));
    }

    /**
     * UUID から決定論的に鮮やかな色を生成する (HSL → RGB)。
     * 戻り値: float[3] = {r, g, b} (0..1)
     */
    private static float[] colorFromUuid(UUID uuid) {
        // 上位ビットと下位ビットを XOR してハッシュ化
        long hash = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        // 色相: 0..360 を 12分割して割り当て（見分けやすい）
        float hue = Math.abs(hash % 12) / 12.0f * 360.0f;
        // 彩度: 0.7..1.0
        float saturation = 0.7f + (Math.abs(hash >> 4) % 4) / 10.0f;
        // 明度: 0.5..0.8
        float lightness = 0.5f + (Math.abs(hash >> 8) % 4) / 10.0f;

        return hslToRgb(hue / 360.0f, saturation, lightness);
    }

    /**
     * HSL → RGB 変換。
     */
    private static float[] hslToRgb(float h, float s, float l) {
        float r, g, b;
        if (s == 0f) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1f + s) : l + s - l * s;
            float p = 2f * l - q;
            r = hueToRgb(p, q, h + 1f / 3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f / 3f);
        }
        return new float[] { r, g, b };
    }

    private static float hueToRgb(float p, float q, float t) {
        if (t < 0f) t += 1f;
        if (t > 1f) t -= 1f;
        if (t < 1f / 6f) return p + (q - p) * 6f * t;
        if (t < 1f / 2f) return q;
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f;
        return p;
    }

    /**
     * デバッグ表示のON/OFFを設定する（サーバーからの指示。現在はクライアント側の所持チェックに置き換え）。
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        Salmon.LOGGER.info("[Client] Debug display {}", enabled ? "ON" : "OFF");
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    // =======================================================================
    // ネットワーク受信ハンドラ
    // =======================================================================

    public void handlePayload(int action, int arenaNumber, UUID arenaUuid,
                              BlockPos min, BlockPos max,
                              BlockPos cornerA, BlockPos cornerB,
                              UUID markerAId, UUID markerBId,
                              ResourceKey<Level> dimension) {
        switch (action) {
            case 0: // FULL_SYNC
                if (arenaNumber == -1) {
                    // FULL_SYNC 開始マーカー: デバッグON
                    debugEnabled = true;
                    fullSyncInProgress = true;
                    fullSyncBuffer.clear();
                } else if (arenaNumber == -2) {
                    fullSyncInProgress = false;
                    Map<UUID, CachedArena> dimMap = arenaCache.computeIfAbsent(dimension,
                            k -> new ConcurrentHashMap<>());
                    dimMap.clear();
                    for (CachedArena ca : fullSyncBuffer) {
                        dimMap.put(ca.arenaUuid, ca);
                    }
                    fullSyncBuffer.clear();
                    Salmon.LOGGER.info("[Client] Arena cache updated: count={}", dimMap.size());
                } else {
                    CachedArena ca = new CachedArena(arenaNumber, arenaUuid, min, max,
                            cornerA, cornerB, markerAId, markerBId, dimension);
                    if (fullSyncInProgress) {
                        fullSyncBuffer.add(ca);
                    }
                }
                break;
            case 1: // ADD
            {
                CachedArena ca = new CachedArena(arenaNumber, arenaUuid, min, max,
                        cornerA, cornerB, markerAId, markerBId, dimension);
                Map<UUID, CachedArena> dimMap = arenaCache.computeIfAbsent(dimension,
                        k -> new ConcurrentHashMap<>());
                dimMap.put(arenaUuid, ca);
            }
                break;
            case 2: // REMOVE
            {
                Map<UUID, CachedArena> dimMap = arenaCache.get(dimension);
                if (dimMap != null) {
                    dimMap.remove(arenaUuid);
                }
            }
                break;
            case 3: // CLEAR
                debugEnabled = false;
                arenaCache.clear();
                fullSyncBuffer.clear();
                fullSyncInProgress = false;
                break;
        }
    }

    // =======================================================================
    // 抽出フェーズ (END_EXTRACTION)
    // =======================================================================

    public void extractDebugState(LevelExtractionContext context) {
        if (!extractInvokedOnce) {
            extractInvokedOnce = true;
            Salmon.LOGGER.info("[Client] Extract phase invoked at least once");
        }

        // マーカーブロックを所持している時のみデバッグ表示を有効にする
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            this.arenaRenderState = ArenaRenderState.empty();
            return;
        }

        if (!isHoldingMarkerBlock(mc)) {
            this.arenaRenderState = ArenaRenderState.empty();
            return;
        }

        ResourceKey<Level> currentDim = mc.level.dimension();
        Map<UUID, CachedArena> dimArenas = arenaCache.get(currentDim);
        if (dimArenas == null || dimArenas.isEmpty()) {
            this.arenaRenderState = ArenaRenderState.empty();
            return;
        }

        Vec3 camPos = mc.player.getEyePosition();

        List<ArenaRenderState.Box> boxes = new ArrayList<>();
        for (CachedArena ca : dimArenas.values()) {
            // 距離判定
            double centerX = (ca.min.getX() + ca.max.getX() + 1) / 2.0;
            double centerY = (ca.min.getY() + ca.max.getY() + 1) / 2.0;
            double centerZ = (ca.min.getZ() + ca.max.getZ() + 1) / 2.0;
            double dx = centerX - camPos.x;
            double dy = centerY - camPos.y;
            double dz = centerZ - camPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > DEBUG_RENDER_DISTANCE * DEBUG_RENDER_DISTANCE) {
                continue;
            }

            // inclusive 範囲 → max 座標に +1.0
            float minX = (float) ca.min.getX();
            float minY = (float) ca.min.getY();
            float minZ = (float) ca.min.getZ();
            float maxX = (float) (ca.max.getX() + 1.0);
            float maxY = (float) (ca.max.getY() + 1.0);
            float maxZ = (float) (ca.max.getZ() + 1.0);

            // アリーナごとのランダム色 (UUID から決定論的に生成)
            float[] arenaColor = colorFromUuid(ca.arenaUuid);

            // アリーナ本体: 薄い塗りつぶし (背景)
            boxes.add(new ArenaRenderState.Box(minX, minY, minZ, maxX, maxY, maxZ,
                    arenaColor[0], arenaColor[1], arenaColor[2], 0.1f));

            // アリーナのワイヤーフレーム: 12辺 × 細い直方体
            ArenaRenderState.addWireframeBox(boxes, minX, minY, minZ, maxX, maxY, maxZ,
                    arenaColor[0], arenaColor[1], arenaColor[2], 0.7f);

            // マーカーA: 赤色の小塗りつぶしボックス + ワイヤーフレーム
            float maX = (float) (ca.cornerA.getX() + 0.5);
            float maY = (float) (ca.cornerA.getY() + 0.5);
            float maZ = (float) (ca.cornerA.getZ() + 0.5);
            float s = 0.3f;
            boxes.add(new ArenaRenderState.Box(maX - s, maY - s, maZ - s, maX + s, maY + s, maZ + s,
                    MARKER_A_R, MARKER_A_G, MARKER_A_B, MARKER_A_ALPHA * MARKER_FILL_ALPHA_FACTOR));
            ArenaRenderState.addWireframeBox(boxes, maX - s, maY - s, maZ - s, maX + s, maY + s, maZ + s,
                    MARKER_A_R, MARKER_A_G, MARKER_A_B, MARKER_A_ALPHA);

            // マーカーB: 青色の小塗りつぶしボックス + ワイヤーフレーム
            float mbX = (float) (ca.cornerB.getX() + 0.5);
            float mbY = (float) (ca.cornerB.getY() + 0.5);
            float mbZ = (float) (ca.cornerB.getZ() + 0.5);
            boxes.add(new ArenaRenderState.Box(mbX - s, mbY - s, mbZ - s, mbX + s, mbY + s, mbZ + s,
                    MARKER_B_R, MARKER_B_G, MARKER_B_B, MARKER_B_ALPHA * MARKER_FILL_ALPHA_FACTOR));
            ArenaRenderState.addWireframeBox(boxes, mbX - s, mbY - s, mbZ - s, mbX + s, mbY + s, mbZ + s,
                    MARKER_B_R, MARKER_B_G, MARKER_B_B, MARKER_B_ALPHA);
        }

        this.arenaRenderState = new ArenaRenderState(boxes);
    }

    // =======================================================================
    // 描画フェーズ (AFTER_TRANSLUCENT_TERRAIN)
    // =======================================================================

    public void renderAndDrawDebugState(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (!renderInvokedOnce) {
            renderInvokedOnce = true;
            Salmon.LOGGER.info("[Client] Render callback invoked at least once");
        }

        ArenaRenderState state = this.arenaRenderState;
        if (state == null || state.boxes.isEmpty()) {
            return;
        }

        // --- パイプライン情報を取得 ---
        RenderPipeline pipeline = FILLED_THROUGH_WALLS;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) {
            Salmon.LOGGER.warn("[Client] formatBinding is null");
            return;
        }

        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();

        // --- 診断ログ (1秒に1回以下) ---
        long now = System.currentTimeMillis();
        boolean shouldLog = (now - lastDiagnosticLogTime > 1000);
        if (shouldLog) {
            lastDiagnosticLogTime = now;
            Salmon.LOGGER.info("[Client] render event invoked, pipeline topology={}, vertex format={}",
                    primitive, formatBinding);
        }

        // --- 描画バッチを開始（pipeline の topology に従う） ---
        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(
                formatBinding,
                primitive,
                primitive == PrimitiveTopology.QUADS
                        ? RenderSystem.getProjectionType().vertexSorting()
                        : null);

        if (shouldLog) {
            Salmon.LOGGER.info("[Client] draw created, vertex count before build: pending");
        }

        // --- 頂点の構築 ---
        this.renderArenas(context, draw, state);

        // --- アップロード ---
        STAGED_BUFFER.upload();

        if (shouldLog) {
            Salmon.LOGGER.info("[Client] upload completed");
        }

        // --- DrawCall 発行 ---
        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);

        if (shouldLog) {
            Salmon.LOGGER.info("[Client] ExecuteInfo null={}, indexCount={}, firstIndex={}, baseVertex={}",
                    info == null,
                    info != null ? info.indexCount() : -1,
                    info != null ? info.firstIndex() : -1,
                    info != null ? info.baseVertex() : -1);
        }

        if (info != null && info.indexCount() > 0) {
            draw(Minecraft.getInstance(), info, pipeline);
            if (shouldLog) {
                Salmon.LOGGER.info("[Client] drawIndexed called");
            }
        } else {
            if (info == null) {
                Salmon.LOGGER.warn("[Client] ExecuteInfo is null, skipping draw");
            } else {
                Salmon.LOGGER.warn("[Client] indexCount is 0, skipping draw");
            }
        }

        // --- フレーム終了処理 ---
        STAGED_BUFFER.endFrame();
    }

    // =======================================================================
    // 頂点構築ヘルパー
    // =======================================================================

    private void renderArenas(LevelRenderContext context, StagedVertexBuffer.Draw draw, ArenaRenderState state) {
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        Matrix4fc pose = matrices.last().pose();
        VertexConsumer builder = STAGED_BUFFER.getVertexBuilder(draw);

        int renderedCount = 0;
        for (ArenaRenderState.Box box : state.boxes) {
            renderFilledBox(pose, builder,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    box.r, box.g, box.b, box.a);
            renderedCount++;
        }

        matrices.popPose();

        long now = System.currentTimeMillis();
        if (now - lastDiagnosticLogTime > 1000) {
            Salmon.LOGGER.info("[Client] Rendered boxes={}", renderedCount);
        }
    }

    /**
     * 6面 QUADS の塗りつぶしボックスを描画。
     * 公式サンプル CustomRenderPipeline.renderFilledBox() と同等。
     */
    private void renderFilledBox(Matrix4fc positionMatrix, VertexConsumer buffer,
                                  float minX, float minY, float minZ,
                                  float maxX, float maxY, float maxZ,
                                  float red, float green, float blue, float alpha) {
        // Front Face (Z+)
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);

        // Back face (Z-)
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);

        // Left face (X-)
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);

        // Right face (X+)
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);

        // Top face (Y+)
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);

        // Bottom face (Y-)
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
    }

    // =======================================================================
    // DrawCall 発行
    // =======================================================================

    /**
     * StagedVertexBuffer の ExecuteInfo を使ってカスタムパイプラインで描画する。
     * 公式サンプル CustomRenderPipeline.draw() と同等。
     */
    private static void draw(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET,
                        TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();

        if (colorTexture == null) {
            Salmon.LOGGER.warn("[Client] colorTexture is null");
            return;
        }

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "salmon_arena_debug",
                        colorTexture,
                        Optional.empty(),
                        mainTarget.getDepthTextureView(),
                        OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    /**
     * リソース解放。GameRenderer#close から呼ぶこと。
     */
    public static void close() {
        STAGED_BUFFER.close();
    }

    // =======================================================================
    // 不変レンダーステート (抽出フェーズ → 描画フェーズ)
    // =======================================================================

    private static class ArenaRenderState {
        final List<Box> boxes;

        ArenaRenderState(List<Box> boxes) {
            this.boxes = Collections.unmodifiableList(boxes);
        }

        /**
         * 空のレンダーステート。
         */
        static ArenaRenderState empty() {
            return new ArenaRenderState(List.of());
        }

        /**
         * 単一の塗りつぶしボックスを持つレンダーステートを作成。
         */
        static ArenaRenderState fixedBox(float minX, float minY, float minZ,
                                          float maxX, float maxY, float maxZ,
                                          float r, float g, float b, float a) {
            return new ArenaRenderState(List.of(new Box(minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a)));
        }

        /**
         * 直方体 range (min..max) の12辺を、太さ EDGE_THICKNESS の細い filled cuboid として boxes に追加する。
         * 各辺は、その中心線を挟むように直方体を生成する。
         */
        static void addWireframeBox(List<Box> boxes,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ,
                                     float r, float g, float b, float a) {
            float t = EDGE_THICKNESS / 2.0f; // 辺の太さの半分 = 中心からのオフセット

            // 底面4辺 (y = minY)
            addEdgeCuboid(boxes, minX, minY, minZ, maxX, minY, minZ, t, r, g, b, a); // X軸方向
            addEdgeCuboid(boxes, maxX, minY, minZ, maxX, minY, maxZ, t, r, g, b, a); // Z軸方向
            addEdgeCuboid(boxes, maxX, minY, maxZ, minX, minY, maxZ, t, r, g, b, a); // X軸方向(逆)
            addEdgeCuboid(boxes, minX, minY, maxZ, minX, minY, minZ, t, r, g, b, a); // Z軸方向(逆)

            // 上面4辺 (y = maxY)
            addEdgeCuboid(boxes, minX, maxY, minZ, maxX, maxY, minZ, t, r, g, b, a);
            addEdgeCuboid(boxes, maxX, maxY, minZ, maxX, maxY, maxZ, t, r, g, b, a);
            addEdgeCuboid(boxes, maxX, maxY, maxZ, minX, maxY, maxZ, t, r, g, b, a);
            addEdgeCuboid(boxes, minX, maxY, maxZ, minX, maxY, minZ, t, r, g, b, a);

            // 縦4辺 (X=minX, X=maxX, Z=minZ, Z=maxZ)
            addEdgeCuboid(boxes, minX, minY, minZ, minX, maxY, minZ, t, r, g, b, a);
            addEdgeCuboid(boxes, maxX, minY, minZ, maxX, maxY, minZ, t, r, g, b, a);
            addEdgeCuboid(boxes, maxX, minY, maxZ, maxX, maxY, maxZ, t, r, g, b, a);
            addEdgeCuboid(boxes, minX, minY, maxZ, minX, maxY, maxZ, t, r, g, b, a);
        }

        /**
         * 点 (x1,y1,z1) から (x2,y2,z2) を結ぶ辺を、太さ 2*thickness の細い直方体として追加する。
         * 2点から軸方向を判定し、その方向にはそのまま、直交2軸は ±thickness の範囲で直方体を生成する。
         */
        private static void addEdgeCuboid(List<Box> boxes,
                                           float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           float thickness,
                                           float r, float g, float b, float a) {
            float eminX = Math.min(x1, x2);
            float eminY = Math.min(y1, y2);
            float eminZ = Math.min(z1, z2);
            float emaxX = Math.max(x1, x2);
            float emaxY = Math.max(y1, y2);
            float emaxZ = Math.max(z1, z2);

            // 軸判定: 差分が最も大きい方向が主軸
            float dx = emaxX - eminX;
            float dy = emaxY - eminY;
            float dz = emaxZ - eminZ;

            if (dx >= dy && dx >= dz) {
                // X軸方向が主軸: Y,Z 方向に厚み
                boxes.add(new Box(eminX, eminY - thickness, eminZ - thickness,
                        emaxX, emaxY + thickness, emaxZ + thickness,
                        r, g, b, a));
            } else if (dy >= dz) {
                // Y軸方向が主軸: X,Z 方向に厚み
                boxes.add(new Box(eminX - thickness, eminY, eminZ - thickness,
                        emaxX + thickness, emaxY, emaxZ + thickness,
                        r, g, b, a));
            } else {
                // Z軸方向が主軸: X,Y 方向に厚み
                boxes.add(new Box(eminX - thickness, eminY - thickness, eminZ,
                        emaxX + thickness, emaxY + thickness, emaxZ,
                        r, g, b, a));
            }
        }

        record Box(float minX, float minY, float minZ,
                    float maxX, float maxY, float maxZ,
                    float r, float g, float b, float a) {
        }
    }

    // =======================================================================
    // 内部データクラス (ネットワークキャッシュ用)
    // =======================================================================

    public static class CachedArena {
        public final int arenaNumber;
        public final UUID arenaUuid;
        public final BlockPos min;
        public final BlockPos max;
        public final BlockPos cornerA;
        public final BlockPos cornerB;
        public final UUID markerAId;
        public final UUID markerBId;
        public final ResourceKey<Level> dimension;

        public CachedArena(int arenaNumber, UUID arenaUuid,
                           BlockPos min, BlockPos max,
                           BlockPos cornerA, BlockPos cornerB,
                           UUID markerAId, UUID markerBId,
                           ResourceKey<Level> dimension) {
            this.arenaNumber = arenaNumber;
            this.arenaUuid = arenaUuid;
            this.min = min;
            this.max = max;
            this.cornerA = cornerA;
            this.cornerB = cornerB;
            this.markerAId = markerAId;
            this.markerBId = markerBId;
            this.dimension = dimension;
        }
    }
}
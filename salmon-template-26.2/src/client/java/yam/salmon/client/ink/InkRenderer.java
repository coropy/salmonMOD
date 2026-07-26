package yam.salmon.client.ink;

import java.util.*;

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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import yam.salmon.Salmon;
import yam.salmon.ink.InkCellGeometry;
import yam.salmon.ink.InkFaceData;
import yam.salmon.ink.InkTeam;
import yam.salmon.block.ModBlockTags;

/**
 * クライアント側のインク描画レンダラー。
 * MC 26.2 の StagedVertexBuffer + RenderPipeline API を使用する。
 *
 * <p>抽出/描画の2フェーズパターンを使用し、
 * アリーナ境界デバッグレンダラーと同様のアーキテクチャを踏襲する。</p>
 *
 * <p>深度テスト有効、不透明描画、ブロック表面からわずかにオフセット。</p>
 */
public class InkRenderer {

    private static final InkRenderer INSTANCE = new InkRenderer();

    /** 描画距離（ブロック） */
    private static final double INK_RENDER_DISTANCE = 128.0;

    // --- RenderPipeline: 深度テスト有効な不透明描画 ---
    private static final RenderPipeline INK_PIPELINE;

    // --- 動的ユニフォーム ---
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    // --- ステージングバッファ ---
    private static final StagedVertexBuffer STAGED_BUFFER;

    static {
        // DEBUG_FILLED_SNIPPET ベースで深度テスト有効
        INK_PIPELINE = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                        .withLocation(Salmon.id("pipeline/ink_filled"))
                        .build());
        STAGED_BUFFER = new StagedVertexBuffer(
                () -> "Ink Render Buffer",
                RenderType.SMALL_BUFFER_SIZE);
    }

    // --- 抽出フェーズでキャプチャしたレンダーステート ---
    private InkRenderState renderState;

    // --- 診断用 ---
    private boolean renderInvokedOnce = false;
    private boolean extractInvokedOnce = false;
    private long lastDiagnosticLogTime = 0;

    private InkRenderer() {}

    public static InkRenderer getInstance() {
        return INSTANCE;
    }

    // ===================================================================
    // 抽出フェーズ (END_EXTRACTION)
    // ===================================================================

    public void extractInkState(LevelExtractionContext context) {
        if (!extractInvokedOnce) {
            extractInvokedOnce = true;
            Salmon.LOGGER.info("[Client] Ink extract phase invoked at least once");
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            this.renderState = InkRenderState.empty();
            return;
        }

        Identifier currentDim = mc.level.dimension().identifier();
        ClientInkCache cache = ClientInkCache.getInstance();

        // ディメンションの全サーフェスを取得
        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> allSurfaces =
                cache.getSurfacesForDimension(currentDim);
        if (allSurfaces.isEmpty()) {
            this.renderState = InkRenderState.empty();
            return;
        }

        Vec3 camPos = mc.player.getEyePosition();

        List<InkRenderState.CellBox> cellBoxes = new ArrayList<>();

        for (var arenaEntry : allSurfaces.entrySet()) {
            for (ClientInkSurface surface : arenaEntry.getValue().values()) {
                if (surface.isEmpty()) continue;

                BlockPos blockPos = surface.blockPos();
                Direction face = surface.face();

                // 距離判定（面のブロック中心で判定）
                double cx = blockPos.getX() + 0.5;
                double cy = blockPos.getY() + 0.5;
                double cz = blockPos.getZ() + 0.5;
                double dx = cx - camPos.x;
                double dy = cy - camPos.y;
                double dz = cz - camPos.z;
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > INK_RENDER_DISTANCE * INK_RENDER_DISTANCE) {
                    continue;
                }

                // チャンクロードチェック
                if (!mc.level.isLoaded(blockPos)) {
                    continue;
                }

                // ブロックが ink_paintable か確認
                if (!mc.level.getBlockState(blockPos).is(ModBlockTags.INK_PAINTABLE)) {
                    continue;
                }

                // 各セルをチェック
                byte[] cells = surface.cells();
                for (int v = 0; v < InkFaceData.GRID_SIZE; v++) {
                    for (int u = 0; u < InkFaceData.GRID_SIZE; u++) {
                        byte team = cells[v * InkFaceData.GRID_SIZE + u];
                        if (team == InkTeam.NONE) continue;

                        float[] color = ClientInkColors.getColor(team);
                        if (color == null) continue;

                        double[] bounds = InkCellGeometry.getCellBounds(blockPos, face, u, v);

                        cellBoxes.add(new InkRenderState.CellBox(
                                (float) bounds[0], (float) bounds[1], (float) bounds[2],
                                (float) bounds[3], (float) bounds[4], (float) bounds[5],
                                color[0], color[1], color[2], color[3]));
                    }
                }
            }
        }

        this.renderState = new InkRenderState(cellBoxes);
    }

    // ===================================================================
    // 描画フェーズ (AFTER_TRANSLUCENT_TERRAIN)
    // ===================================================================

    public void renderAndDrawInkState(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (!renderInvokedOnce) {
            renderInvokedOnce = true;
            Salmon.LOGGER.info("[Client] Ink render callback invoked at least once");
        }

        InkRenderState state = this.renderState;
        if (state == null || state.cellBoxes.isEmpty()) {
            return;
        }

        // --- パイプライン情報 ---
        RenderPipeline pipeline = INK_PIPELINE;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) {
            Salmon.LOGGER.warn("[Client] Ink formatBinding is null");
            return;
        }

        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();

        // --- 診断ログ (1秒に1回以下) ---
        long now = System.currentTimeMillis();
        boolean shouldLog = (now - lastDiagnosticLogTime > 1000);
        if (shouldLog) {
            lastDiagnosticLogTime = now;
            Salmon.LOGGER.info("[Client] Ink render: visibleSurfaces={} renderedCells={}",
                    state.cellBoxes.size() > 0 ? countUniqueSurfaces(state.cellBoxes) : 0,
                    state.cellBoxes.size());
        }

        // --- 描画バッチ ---
        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(
                formatBinding,
                primitive,
                primitive == PrimitiveTopology.QUADS
                        ? RenderSystem.getProjectionType().vertexSorting()
                        : null);

        // --- 頂点構築 ---
        renderCells(context, draw, state);

        // --- アップロード ---
        STAGED_BUFFER.upload();

        // --- DrawCall ---
        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null && info.indexCount() > 0) {
            drawInk(mc, info, pipeline);
        }

        // --- フレーム終了 ---
        STAGED_BUFFER.endFrame();
    }

    private int countUniqueSurfaces(List<InkRenderState.CellBox> boxes) {
        // 簡易：BlockPos+Directionのペアでカウント
        Set<String> keys = new HashSet<>();
        for (var box : boxes) {
            // 座標から逆算する代わりに、近似的に
            keys.add(String.format("%.0f,%.0f,%.0f", box.minX, box.minY, box.minZ));
        }
        return keys.size();
    }

    // ===================================================================
    // 頂点構築
    // ===================================================================

    private void renderCells(LevelRenderContext context, StagedVertexBuffer.Draw draw, InkRenderState state) {
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        Matrix4fc pose = matrices.last().pose();
        VertexConsumer builder = STAGED_BUFFER.getVertexBuilder(draw);

        for (InkRenderState.CellBox box : state.cellBoxes) {
            renderFilledBox(pose, builder,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    box.r, box.g, box.b, box.a);
        }

        matrices.popPose();
    }

    /**
     * 6面 QUADS の塗りつぶしボックスを描画。
     * ArenaDebugRenderer.renderFilledBox() と同等。
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

    // ===================================================================
    // DrawCall 発行
    // ===================================================================

    /**
     * StagedVertexBuffer の ExecuteInfo を使ってカスタムパイプラインで描画する。
     * ArenaDebugRenderer.draw() と同等。
     */
    private static void drawInk(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET,
                        TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();

        if (colorTexture == null) {
            Salmon.LOGGER.warn("[Client] Ink colorTexture is null");
            return;
        }

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "salmon_ink",
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

    // ===================================================================
    // 不変レンダーステート
    // ===================================================================

    private static class InkRenderState {
        final List<CellBox> cellBoxes;

        InkRenderState(List<CellBox> cellBoxes) {
            this.cellBoxes = Collections.unmodifiableList(cellBoxes);
        }

        static InkRenderState empty() {
            return new InkRenderState(List.of());
        }

        record CellBox(float minX, float minY, float minZ,
                        float maxX, float maxY, float maxZ,
                        float r, float g, float b, float a) {}
    }
}
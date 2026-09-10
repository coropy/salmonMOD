package yam.salmon.client.ink.ika;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import yam.salmon.Salmon;
import yam.salmon.client.ink.ClientInkCache;
import yam.salmon.client.ink.ClientInkColors;
import yam.salmon.client.ink.ClientInkSurface;
import yam.salmon.client.ink.ClientInkSurfaceKey;
import yam.salmon.client.state.ClientPlayerInkState;
import yam.salmon.ink.InkCellGeometry;
import yam.salmon.ink.InkSurfacePatchId;
import yam.salmon.ink.PlayerInkState;

/**
 * イカ状態（{@link PlayerInkState#CROUCHING_ON_OWN_INK}）のプレイヤーが
 * 潜伏している位置を示す「薄い正方形」を床インク面上に描画する。
 *
 * <p>実インクデータ（InkStorage）には一切触れない、
 * 専用のクライアント描画。色は {@link ClientInkCache} の足元セル
 * （既存のチームカラー定義 {@link ClientInkColors}）から取得する。</p>
 *
 * <p>描画は世界空間（AFTER_TRANSLUCENT_TERRAIN）で行うため、
 * 自分・他プレイヤー問わず同一条件で見える。</p>
 */
public final class SquidInkMarkRenderer {

    private static final SquidInkMarkRenderer INSTANCE = new SquidInkMarkRenderer();

    /** 潜伏マークの一辺サイズ（ブロック）。プレイヤーの足元程度。 */
    public static final float SQUID_INK_MARK_SIZE = 0.6f;

    /** 潜伏マークの不透明度（非常に薄く）。 */
    public static final float SQUID_INK_MARK_ALPHA = 0.35f;

    /** 床インク面からの追加オフセット（Z-fighting回避）。 */
    public static final float SQUID_INK_MARK_Y_OFFSET = 0.01f;

    private static final RenderPipeline MARK_PIPELINE;

    private static final StagedVertexBuffer STAGED_BUFFER;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    static {
        MARK_PIPELINE = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                        .withLocation(Salmon.id("pipeline/squid_ink_mark"))
                        .build());
        STAGED_BUFFER = new StagedVertexBuffer(
                () -> "Squid Ink Mark Render Buffer",
                RenderType.SMALL_BUFFER_SIZE);
    }

    private SquidInkMarkRenderer() {}

    public static SquidInkMarkRenderer getInstance() {
        return INSTANCE;
    }

    /** 現フレームで描画するマーク一覧（抽出フェーズで更新）。 */
    private List<Mark> marks = List.of();

    // ===================================================================
    // 抽出フェーズ (END_EXTRACTION)
    // ===================================================================

    /**
     * イカ状態のプレイヤーごとに潜伏マークの位置と色を抽出する。
     */
    public void extractMarkState(LevelExtractionContext context) {
        ClientLevel level = context.level();
        if (level == null) {
            this.marks = List.of();
            return;
        }

        float partialTick = context.deltaTracker().getGameTimeDeltaPartialTick(true);
        List<Mark> extracted = new ArrayList<>();

        for (AbstractClientPlayer player : level.players()) {
            if (ClientPlayerInkState.getInstance().get(player.getUUID())
                    != PlayerInkState.CROUCHING_ON_OWN_INK) {
                continue;
            }

            Vec3 pos = player.getPosition(partialTick);
            extracted.add(resolveMark(level, pos));
        }

        this.marks = List.copyOf(extracted);
    }

    /**
     * プレイヤーの足元のインク面を解決し、マーク情報を構築する。
     *
     * <p>インクパッチが見つからない場合（同期未完了など）は
     * 足元ブロックの上面＋既定チーム色へフォールバックする。</p>
     */
    private static Mark resolveMark(ClientLevel level, Vec3 pos) {
        BlockPos belowBlock = BlockPos.containing(pos.x, Math.floor(pos.y) - 1.0, pos.z);

        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> surfaces =
                ClientInkCache.getInstance().getSurfacesForDimension(level.dimension().identifier());

        ClientInkSurface best = null;
        for (Map<ClientInkSurfaceKey, ClientInkSurface> arenaSurfaces : surfaces.values()) {
            for (ClientInkSurface surface : arenaSurfaces.values()) {
                if (!surface.blockPos().equals(belowBlock)) {
                    continue;
                }
                InkSurfacePatchId patchId = surface.patchId();
                if (patchId.normal() != Direction.UP) {
                    continue;
                }
                if (best == null || patchId.plane() > best.patchId().plane()) {
                    best = surface;
                }
            }
        }

        if (best != null) {
            InkSurfacePatchId patchId = best.patchId();
            double localX = pos.x - belowBlock.getX();
            double localZ = pos.z - belowBlock.getZ();

            // パッチ範囲内のセルのチーム色を取得
            float[] color = resolveCellColor(best, localX, localZ);
            double markY = belowBlock.getY()
                    + patchId.plane() / (double) InkSurfacePatchId.BLOCK_RESOLUTION
                    + InkCellGeometry.PATCH_NORMAL_OFFSET
                    + SQUID_INK_MARK_Y_OFFSET;
            return new Mark(pos.x, pos.z, markY, color[0], color[1], color[2], SQUID_INK_MARK_ALPHA);
        }

        // フォールバック: 足元ブロック上面に既定チーム色で表示
        float[] fallback = ClientInkColors.TEAM_A;
        double markY = belowBlock.getY() + 1.0
                + InkCellGeometry.PATCH_NORMAL_OFFSET
                + SQUID_INK_MARK_Y_OFFSET;
        return new Mark(pos.x, pos.z, markY, fallback[0], fallback[1], fallback[2], SQUID_INK_MARK_ALPHA);
    }

    private static float[] resolveCellColor(ClientInkSurface surface, double localX, double localZ) {
        int cellU = (int) Math.floor(localX * InkSurfacePatchId.BLOCK_RESOLUTION);
        int cellV = (int) Math.floor(localZ * InkSurfacePatchId.BLOCK_RESOLUTION);
        cellU = Math.min(Math.max(cellU, 0), 15);
        cellV = Math.min(Math.max(cellV, 0), 15);
        byte team = surface.getCell(cellU, cellV);
        float[] color = ClientInkColors.getColor(team);
        return color != null ? color : ClientInkColors.TEAM_A;
    }

    // ===================================================================
    // 描画フェーズ (AFTER_TRANSLUCENT_TERRAIN)
    // ===================================================================

    /**
     * 抽出済みマークを床面に平行な薄い正方形として描画する。
     */
    public void renderAndDrawMarkState(LevelRenderContext context) {
        List<Mark> current = this.marks;
        if (current.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack matrices = context.poseStack();

        RenderPipeline pipeline = MARK_PIPELINE;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) {
            return;
        }

        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();

        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(
                formatBinding,
                primitive,
                primitive == PrimitiveTopology.QUADS
                        ? RenderSystem.getProjectionType().vertexSorting()
                        : null);

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        Matrix4fc pose = matrices.last().pose();
        VertexConsumer builder = STAGED_BUFFER.getVertexBuilder(draw);

        float half = SQUID_INK_MARK_SIZE / 2.0f;

        for (Mark mark : current) {
            float y = (float) mark.y();
            float minX = (float) mark.x() - half;
            float maxX = (float) mark.x() + half;
            float minZ = (float) mark.z() - half;
            float maxZ = (float) mark.z() + half;

            // 上から見てCCW（法線+Y）
            builder.addVertex(pose, minX, y, minZ).setColor(mark.r(), mark.g(), mark.b(), mark.a());
            builder.addVertex(pose, maxX, y, minZ).setColor(mark.r(), mark.g(), mark.b(), mark.a());
            builder.addVertex(pose, maxX, y, maxZ).setColor(mark.r(), mark.g(), mark.b(), mark.a());
            builder.addVertex(pose, minX, y, maxZ).setColor(mark.r(), mark.g(), mark.b(), mark.a());
        }

        matrices.popPose();

        STAGED_BUFFER.upload();

        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null && info.indexCount() > 0) {
            drawMark(mc, info, pipeline);
        }

        STAGED_BUFFER.endFrame();
    }


    /**
     * DrawCall 発行（InkRenderer.drawInk() と同等）。
     */
    private static void drawMark(Minecraft client, StagedVertexBuffer.ExecuteInfo info,
                                 RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET,
                        TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        if (colorTexture == null) {
            return;
        }

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "salmon_squid_ink_mark",
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

    /** 1プレイヤー分のマーク情報（XZ位置＋床面Y＋色）。 */
    private record Mark(double x, double z, double y,
                        float r, float g, float b, float a) {}
}

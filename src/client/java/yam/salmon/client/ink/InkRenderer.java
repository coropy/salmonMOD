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
import yam.salmon.ink.InkCellQuad;
import yam.salmon.ink.InkFaceData;
import yam.salmon.ink.InkSurfacePatchId;
import yam.salmon.ink.InkTeam;

/**
 * クライアント側のインク描画レンダラー。
 *
 * <p>Phase 9: Quadベースの表面描画に完全移行。renderFilledBox() は廃止。
 * 同一パッチ内の同色セルを greedy meshing で矩形へ結合し、
 * 1枚の表面Quadとして描画する。</p>
 *
 * <p>深度テスト有効、不透明描画、ブロック表面からわずかにオフセット。</p>
 */
public class InkRenderer {

    private static final InkRenderer INSTANCE = new InkRenderer();

    private static final double INK_RENDER_DISTANCE = 128.0;

    private static final RenderPipeline INK_PIPELINE;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static final StagedVertexBuffer STAGED_BUFFER;

    static {
        INK_PIPELINE = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                        .withLocation(Salmon.id("pipeline/ink_filled"))
                        .build());
        STAGED_BUFFER = new StagedVertexBuffer(
                () -> "Ink Render Buffer",
                RenderType.SMALL_BUFFER_SIZE);
    }

    private InkRenderState renderState;

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

        Map<UUID, Map<ClientInkSurfaceKey, ClientInkSurface>> allSurfaces =
                cache.getSurfacesForDimension(currentDim);
        if (allSurfaces.isEmpty()) {
            this.renderState = InkRenderState.empty();
            return;
        }

        Vec3 camPos = mc.player.getEyePosition();

        List<InkRenderState.ColoredQuad> quads = new ArrayList<>();

        for (var arenaEntry : allSurfaces.entrySet()) {
            for (ClientInkSurface surface : arenaEntry.getValue().values()) {
                if (surface.isEmpty()) continue;

                BlockPos blockPos = surface.blockPos();
                InkSurfacePatchId patchId = surface.patchId();

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

                if (!mc.level.isLoaded(blockPos)) {
                    continue;
                }

                // Greedy meshing で同色セルを矩形化
                byte[] cells = surface.cells();
                boolean[][] visited = new boolean[InkFaceData.GRID_SIZE][InkFaceData.GRID_SIZE];

                for (int v = 0; v < InkFaceData.GRID_SIZE; v++) {
                    for (int u = 0; u < InkFaceData.GRID_SIZE; u++) {
                        if (visited[v][u]) continue;

                        byte team = cells[v * InkFaceData.GRID_SIZE + u];
                        if (team == InkTeam.NONE) continue;

                        float[] color = ClientInkColors.getColor(team);
                        if (color == null) continue;

                        // 行方向に連続領域を確保
                        int maxU = u;
                        while (maxU + 1 < InkFaceData.GRID_SIZE
                                && cells[v * InkFaceData.GRID_SIZE + (maxU + 1)] == team
                                && !visited[v][maxU + 1]) {
                            maxU++;
                        }

                        // 下方向に拡張可能か確認
                        int maxV = v;
                        while (maxV + 1 < InkFaceData.GRID_SIZE) {
                            boolean canExtend = true;
                            for (int cu = u; cu <= maxU; cu++) {
                                if (cells[(maxV + 1) * InkFaceData.GRID_SIZE + cu] != team
                                        || visited[maxV + 1][cu]) {
                                    canExtend = false;
                                    break;
                                }
                            }
                            if (canExtend) {
                                maxV++;
                            } else {
                                break;
                            }
                        }

                        // visited マーク
                        for (int cv = v; cv <= maxV; cv++) {
                            for (int cu = u; cu <= maxU; cu++) {
                                visited[cv][cu] = true;
                            }
                        }

                        // Quad生成
                        InkCellQuad quad = InkCellGeometry.getCellQuadForPatch(
                                blockPos, patchId,
                                u, v, maxU + 1, maxV + 1,
                                InkCellGeometry.PATCH_NORMAL_OFFSET);

                        quads.add(new InkRenderState.ColoredQuad(
                                quad, color[0], color[1], color[2], color[3]));
                    }
                }
            }
        }

        this.renderState = new InkRenderState(quads);
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
        if (state == null || state.quads.isEmpty()) {
            return;
        }

        RenderPipeline pipeline = INK_PIPELINE;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) {
            Salmon.LOGGER.warn("[Client] Ink formatBinding is null");
            return;
        }

        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();

        long now = System.currentTimeMillis();
        boolean shouldLog = (now - lastDiagnosticLogTime > 1000);
        if (shouldLog) {
            lastDiagnosticLogTime = now;
            Salmon.LOGGER.info("[Client] Ink render: quads={}", state.quads.size());
        }

        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(
                formatBinding,
                primitive,
                primitive == PrimitiveTopology.QUADS
                        ? RenderSystem.getProjectionType().vertexSorting()
                        : null);

        renderQuads(context, draw, state);

        STAGED_BUFFER.upload();

        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null && info.indexCount() > 0) {
            drawInk(mc, info, pipeline);
        }

        STAGED_BUFFER.endFrame();
    }

    // ===================================================================
    // Quad頂点構築
    // ===================================================================

    private void renderQuads(LevelRenderContext context, StagedVertexBuffer.Draw draw,
                             InkRenderState state) {
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        Matrix4fc pose = matrices.last().pose();
        VertexConsumer builder = STAGED_BUFFER.getVertexBuilder(draw);

        for (InkRenderState.ColoredQuad cq : state.quads) {
            InkCellQuad quad = cq.quad;
            float r = cq.r, g = cq.g, b = cq.b, a = cq.a;

            // QUADS の頂点順（法線から見てCCW）: p00, p10, p11, p01
            builder.addVertex(pose, quad.p00x(), quad.p00y(), quad.p00z()).setColor(r, g, b, a);
            builder.addVertex(pose, quad.p10x(), quad.p10y(), quad.p10z()).setColor(r, g, b, a);
            builder.addVertex(pose, quad.p11x(), quad.p11y(), quad.p11z()).setColor(r, g, b, a);
            builder.addVertex(pose, quad.p01x(), quad.p01y(), quad.p01z()).setColor(r, g, b, a);
        }

        matrices.popPose();
    }

    // ===================================================================
    // DrawCall 発行
    // ===================================================================

    private static void drawInk(Minecraft client, StagedVertexBuffer.ExecuteInfo info,
                                RenderPipeline pipeline) {
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

    public static void close() {
        STAGED_BUFFER.close();
    }

    // ===================================================================
    // 不変レンダーステート
    // ===================================================================

    private static class InkRenderState {
        final List<ColoredQuad> quads;

        InkRenderState(List<ColoredQuad> quads) {
            this.quads = Collections.unmodifiableList(quads);
        }

        static InkRenderState empty() {
            return new InkRenderState(List.of());
        }

        record ColoredQuad(InkCellQuad quad, float r, float g, float b, float a) {}
    }
}
package yam.salmon.client.shot;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

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
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import yam.salmon.Salmon;
import yam.salmon.client.shot.ClientInkTrailDrop;

/**
 * クライアント側の視覚弾道レンダラー。
 *
 * <p>MC 26.2 の StagedVertexBuffer + RenderPipeline API を使用し、
 * 小さな立方体で弾を描画する。</p>
 */
public final class InkShotRenderer {
    private static final InkShotRenderer INSTANCE = new InkShotRenderer();

    private static final RenderPipeline SHOT_PIPELINE;

    private static final StagedVertexBuffer STAGED_BUFFER;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    static {
        SHOT_PIPELINE = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                        .withLocation(Salmon.id("pipeline/ink_shot"))
                        .build());
        STAGED_BUFFER = new StagedVertexBuffer(
                () -> "Ink Shot Render Buffer",
                RenderType.SMALL_BUFFER_SIZE);
    }

    private InkShotRenderer() {}

    public static InkShotRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * 描画フェーズ。LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN などから呼ぶ。
     */
    public void render(List<ClientInkShot> shots, LevelRenderContext context, float partialTick) {
        if (shots.isEmpty()) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack matrices = context.poseStack();

        RenderPipeline pipeline = SHOT_PIPELINE;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) return;

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

        for (ClientInkShot shot : shots) {
            Vec3 worldPos = shot.getRenderPosition(partialTick);
            float s = shot.size();
            int rgb = shot.colorRgb();
            float r = ((rgb >> 16) & 0xFF) / 255f;
            float g = ((rgb >> 8) & 0xFF) / 255f;
            float b = (rgb & 0xFF) / 255f;
            float a = 0.9f;

            renderFilledBox(pose, builder,
                    (float)(worldPos.x - s), (float)(worldPos.y - s), (float)(worldPos.z - s),
                    (float)(worldPos.x + s), (float)(worldPos.y + s), (float)(worldPos.z + s),
                    r, g, b, a);
        }

        matrices.popPose();

        STAGED_BUFFER.upload();

        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null && info.indexCount() > 0) {
            drawShot(info, pipeline);
        }

        STAGED_BUFFER.endFrame();
    }

    /**
     * 6面 QUADS の塗りつぶしボックス（InkRendererと同等）。
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

    /**
     * DrawCall 発行（InkRenderer.drawInk() と同等）。
     */
    private void drawShot(StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        Minecraft client = Minecraft.getInstance();

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET,
                        TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();

        if (colorTexture == null) return;

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "salmon_ink_shot",
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
     * トレイル滴の描画。
     */
    public void renderDrops(List<ClientInkTrailDrop> drops, LevelRenderContext context, float partialTick) {
        if (drops.isEmpty()) return;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack matrices = context.poseStack();

        RenderPipeline pipeline = SHOT_PIPELINE;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) return;

        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();

        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(
                formatBinding, primitive,
                primitive == PrimitiveTopology.QUADS
                        ? RenderSystem.getProjectionType().vertexSorting() : null);

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        Matrix4fc pose = matrices.last().pose();
        VertexConsumer builder = STAGED_BUFFER.getVertexBuilder(draw);

        for (ClientInkTrailDrop drop : drops) {
            Vec3 worldPos = drop.getRenderPosition(partialTick);
            float s = drop.size();
            int rgb = drop.colorRgb();
            float r = ((rgb >> 16) & 0xFF) / 255f;
            float g = ((rgb >> 8) & 0xFF) / 255f;
            float b = (rgb & 0xFF) / 255f;
            float a = 0.7f;

            renderFilledBox(pose, builder,
                    (float)(worldPos.x - s), (float)(worldPos.y - s), (float)(worldPos.z - s),
                    (float)(worldPos.x + s), (float)(worldPos.y + s), (float)(worldPos.z + s),
                    r, g, b, a);
        }

        matrices.popPose();

        STAGED_BUFFER.upload();

        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null && info.indexCount() > 0) {
            drawShot(info, pipeline);
        }

        STAGED_BUFFER.endFrame();
    }

    /**
     * リソース解放。GameRenderer#close から呼ぶこと。
     */
    public static void close() {
        STAGED_BUFFER.close();
    }
}

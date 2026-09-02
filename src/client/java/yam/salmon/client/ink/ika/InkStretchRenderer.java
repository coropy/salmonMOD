package yam.salmon.client.ink.ika;

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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import yam.salmon.Salmon;

/**
 * 「インク伸び」演出のレンダラー。
 *
 * <p>MC 26.2 の StagedVertexBuffer + RenderPipeline API を使用し、
 * 最後にいた自チームインクの位置からイカ状態解除位置へ、短時間でフェードする
 * 帯状のインクを描画する。{@link InkStretchTracker} が生成したエフェクトのみを
 * 描画し、地面インクデータには一切触れない。</p>
 */
public final class InkStretchRenderer {
    private static final InkStretchRenderer INSTANCE = new InkStretchRenderer();

    private static final RenderPipeline STRETCH_PIPELINE;

    private static final StagedVertexBuffer STAGED_BUFFER;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    /** 帯の始点側の半幅（ブロック）。 */
    private static final float HALF_WIDTH_BASE = 0.3f;
    /** 帯の終点側の半幅（ブロック）。 */
    private static final float HALF_WIDTH_TIP = 0.12f;
    /** 地面インクよりわずかに上へ浮かせるオフセット。 */
    private static final float GROUND_OFFSET = 0.02f;
    /** 基本アルファ。 */
    private static final float BASE_ALPHA = 0.85f;

    static {
        STRETCH_PIPELINE = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                        .withLocation(Salmon.id("pipeline/ink_stretch"))
                        .build());
        STAGED_BUFFER = new StagedVertexBuffer(
                () -> "Ink Stretch Render Buffer",
                RenderType.SMALL_BUFFER_SIZE);
    }

    private InkStretchRenderer() {}

    public static InkStretchRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * 描画フェーズ。LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN から呼ぶ。
     */
    public void render(LevelRenderContext context) {
        List<InkStretchTracker.StretchEffect> effects =
                InkStretchTracker.getInstance().getActiveEffects();
        if (effects.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        long now = client.level.getGameTime();

        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack matrices = context.poseStack();

        RenderPipeline pipeline = STRETCH_PIPELINE;
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

        for (InkStretchTracker.StretchEffect effect : effects) {
            float progress = Mth.clamp(
                    (now - effect.startGameTime()) / (float) InkStretchTracker.EFFECT_DURATION_TICKS,
                    0.0f, 1.0f);
            float alpha = BASE_ALPHA * (1.0f - progress);
            if (alpha <= 0.01f) continue;

            int rgb = effect.colorArgb();
            float r = ((rgb >> 16) & 0xFF) / 255f;
            float g = ((rgb >> 8) & 0xFF) / 255f;
            float b = (rgb & 0xFF) / 255f;

            drawRibbon(pose, builder, effect.start(), effect.end(), r, g, b, alpha);
        }

        matrices.popPose();

        STAGED_BUFFER.upload();

        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null && info.indexCount() > 0) {
            drawStretch(info, pipeline);
        }

        STAGED_BUFFER.endFrame();
    }

    /**
     * 始点が太く終点が細いテーパー付きの帯（両面 QUADS）を描く。
     */
    private static void drawRibbon(Matrix4fc pose, VertexConsumer buffer,
                                   Vec3 start, Vec3 end,
                                   float r, float g, float b, float alpha) {
        float startX = (float) start.x;
        float startZ = (float) start.z;
        float endX = (float) end.x;
        float endZ = (float) end.z;
        float y = (float) start.y + GROUND_OFFSET;

        float dx = endX - startX;
        float dz = endZ - startZ;
        float lenSq = dx * dx + dz * dz;
        float nx, nz;
        if (lenSq < 1.0e-6f) {
            // ほぼ真上への移動など水平成分がない場合は固定方向の短い帯にする
            nx = 1.0f;
            nz = 0.0f;
        } else {
            float len = (float) Math.sqrt(lenSq);
            nx = -dz / len;
            nz = dx / len;
        }

        float ax = startX + nx * HALF_WIDTH_BASE;
        float az = startZ + nz * HALF_WIDTH_BASE;
        float bx = startX - nx * HALF_WIDTH_BASE;
        float bz = startZ - nz * HALF_WIDTH_BASE;
        float cx = endX - nx * HALF_WIDTH_TIP;
        float cz = endZ - nz * HALF_WIDTH_TIP;
        float ex = endX + nx * HALF_WIDTH_TIP;
        float ez = endZ + nz * HALF_WIDTH_TIP;

        // 表面
        buffer.addVertex(pose, ax, y, az).setColor(r, g, b, alpha);
        buffer.addVertex(pose, bx, y, bz).setColor(r, g, b, alpha);
        buffer.addVertex(pose, cx, y, cz).setColor(r, g, b, alpha);
        buffer.addVertex(pose, ex, y, ez).setColor(r, g, b, alpha);

        // 裏面（頂点順序反転）
        buffer.addVertex(pose, ex, y, ez).setColor(r, g, b, alpha);
        buffer.addVertex(pose, cx, y, cz).setColor(r, g, b, alpha);
        buffer.addVertex(pose, bx, y, bz).setColor(r, g, b, alpha);
        buffer.addVertex(pose, ax, y, az).setColor(r, g, b, alpha);
    }

    /**
     * DrawCall 発行（InkShotRenderer.drawShot() と同等）。
     */
    private void drawStretch(StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
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
                        () -> "salmon_ink_stretch",
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
}

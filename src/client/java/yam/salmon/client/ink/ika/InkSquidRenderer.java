package yam.salmon.client.ink.ika;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import yam.salmon.client.state.ClientPlayerInkState;
import yam.salmon.ink.PlayerInkState;

/**
 * イカ状態のプレイヤーをバニラのイカモデルで描画する。
 *
 * <p>将来の本物イカモデルへ差し替えやすいよう、モデル生成と描画をこのクラスに集約する。</p>
 */
public final class InkSquidRenderer {

    private static final InkSquidRenderer INSTANCE = new InkSquidRenderer();

    private static final Identifier SQUID_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/squid/squid.png");

    private SquidModel model;

    private InkSquidRenderer() {}

    public static InkSquidRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * {@link AvatarRenderState#id} からプレイヤーを解決し、イカ状態なら描画する。
     *
     * @return イカ描画を行った場合 {@code true}
     */
    public boolean tryRender(
            AvatarRenderState avatarState,
            PoseStack poseStack,
            SubmitNodeCollector collector) {
        Player player = resolvePlayer(avatarState.id);
        if (player == null) {
            return false;
        }
        if (ClientPlayerInkState.getInstance().get(player.getUUID()) != PlayerInkState.CROUCHING_ON_OWN_INK) {
            return false;
        }

        ensureModel();
        renderSquid(avatarState, poseStack, collector);
        return true;
    }

    private static Player resolvePlayer(int entityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        Entity entity = client.level.getEntity(entityId);
        return entity instanceof Player player ? player : null;
    }

    private void ensureModel() {
        if (model != null) {
            return;
        }
        var root = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SQUID);
        model = new SquidModel(root);
    }

    private void renderSquid(
            AvatarRenderState avatarState,
            PoseStack poseStack,
            SubmitNodeCollector collector) {
        SquidRenderState squidState = buildSquidState(avatarState);

        poseStack.pushPose();

        float scale = avatarState.scale;
        poseStack.scale(scale, scale, scale);
        setupSquidRotations(squidState, poseStack, avatarState.bodyRot, scale);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);

        model.setupAnim(squidState);

        RenderType renderType = model.renderType(SQUID_TEXTURE);
        int overlay = LivingEntityRenderer.getOverlayCoords(squidState, 0.0f);
        collector.submitModel(
                model,
                squidState,
                poseStack,
                renderType,
                squidState.lightCoords,
                overlay,
                -1,
                null,
                squidState.outlineColor,
                null);

        poseStack.popPose();
    }

    private static SquidRenderState buildSquidState(AvatarRenderState avatarState) {
        SquidRenderState squidState = new SquidRenderState();

        squidState.scale = avatarState.scale;
        squidState.ageScale = avatarState.ageScale;
        squidState.bodyRot = avatarState.bodyRot;
        squidState.yRot = avatarState.yRot;
        squidState.xRot = avatarState.xRot;
        squidState.ageInTicks = avatarState.ageInTicks;
        squidState.lightCoords = avatarState.lightCoords;
        squidState.outlineColor = avatarState.outlineColor;
        squidState.isInvisible = avatarState.isInvisible;
        squidState.isInvisibleToPlayer = avatarState.isInvisibleToPlayer;
        squidState.isBaby = false;

        float tentaclePhase = avatarState.ageInTicks * 0.15f;
        squidState.tentacleAngle = Mth.sin(tentaclePhase) * 0.25f;
        squidState.xBodyRot = 0.0f;
        squidState.zBodyRot = 0.0f;

        return squidState;
    }

    /**
     * {@link net.minecraft.client.renderer.entity.SquidRenderer#setupRotations} と同じ回転を適用する。
     */
    private static void setupSquidRotations(
            SquidRenderState state,
            PoseStack poseStack,
            float yRot,
            float scale) {
        float bodyOffset = state.isBaby ? 0.25f : 0.5f;
        poseStack.translate(0.0f, bodyOffset * scale, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xBodyRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.zBodyRot));
    }
}

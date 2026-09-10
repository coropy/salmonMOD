package yam.salmon.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yam.salmon.client.ink.ika.InkSquidRenderer;

/**
 * イカ状態のプレイヤー描画を差し替える。
 *
 * <p>{@link AvatarRenderer} は {@code submit(...)} を自身で宣言せず
 * {@link LivingEntityRenderer} から継承するため、こちらにフックする。
 * 状態判定は {@link yam.salmon.client.state.ClientPlayerInkState} のみを参照する。</p>
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void salmon$submitIka(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        if (!(((Object) this) instanceof AvatarRenderer)) {
            return;
        }
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }
        if (InkSquidRenderer.getInstance().shouldHide(avatarState)) {
            ci.cancel();
        }
    }
}

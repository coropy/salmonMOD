package yam.salmon.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.AbstractClientPlayer;

import yam.salmon.client.state.ClientPlayerInkState;
import yam.salmon.ink.PlayerInkState;

/**
 * イカ状態中の速度由来FOV補正を抑制する。
 *
 * <p>MC 26.2 では {@code Camera.tickFov()} が
 * {@link AbstractClientPlayer#getFieldOfViewModifier} を呼び出し、
 * {@code MOVEMENT_SPEED} 属性値と基準歩行速度の比でFOVを広げる。
 * イカ状態中は {@link yam.salmon.state.SquidSpeedHandler} の
 * 速度モディファイヤでこの比が約4倍になり、FOVが極端に広がる。</p>
 *
 * <p>イカ状態中はモディファイヤを常に {@code 1.0f}（無補正）として返す。
 * {@code Options.fov()} の設定値は一切変更しないため、
 * 通常プレイ時のFOV・設定値には影響しない。</p>
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {

    @Inject(method = "getFieldOfViewModifier(ZF)F", at = @At("HEAD"), cancellable = true)
    private void salmon$suppressSquidFov(boolean firstPerson, float fovEffectScale,
                                         CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        if (ClientPlayerInkState.getInstance().get(self.getUUID())
                == PlayerInkState.CROUCHING_ON_OWN_INK) {
            cir.setReturnValue(1.0f);
        }
    }
}

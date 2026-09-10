package yam.salmon.client.ink.ika;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import yam.salmon.client.state.ClientPlayerInkState;
import yam.salmon.ink.PlayerInkState;

/**
 * イカ状態（{@link PlayerInkState#CROUCHING_ON_OWN_INK}）のプレイヤーの
 * 通常描画（スキン・モデル・防具・手持ちアイテム）を完全に隠す。
 *
 * <p>潜伏中の視覚的手掛かりは {@link SquidInkMarkRenderer} が描画する
 * 床インク面上の薄い正方形であり、本クラスはモデルを一切submitしない。</p>
 */
public final class InkSquidRenderer {

    private static final InkSquidRenderer INSTANCE = new InkSquidRenderer();

    private InkSquidRenderer() {}

    public static InkSquidRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * {@link AvatarRenderState#id} からプレイヤーを解決し、イカ状態なら
     * 通常のプレイヤー描画をキャンセルすべきかを返す。
     *
     * @return イカ状態の場合 {@code true}（呼び出し側はバニラ描画をcancelする）
     */
    public boolean shouldHide(
            AvatarRenderState avatarState) {
        Player player = resolvePlayer(avatarState.id);
        if (player == null) {
            return false;
        }
        return ClientPlayerInkState.getInstance().get(player.getUUID())
                == PlayerInkState.CROUCHING_ON_OWN_INK;
    }

    private static Player resolvePlayer(int entityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        Entity entity = client.level.getEntity(entityId);
        return entity instanceof Player player ? player : null;
    }
}

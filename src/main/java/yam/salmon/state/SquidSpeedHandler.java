package yam.salmon.state;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import yam.salmon.Salmon;
import yam.salmon.ink.PlayerInkState;

/**
 * イカ状態（{@link PlayerInkState#CROUCHING_ON_OWN_INK}）中の移動速度を制御する。
 *
 * <p>Minecraft 26.2 の {@link Attributes#MOVEMENT_SPEED} に対して
 * {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL} の一時モディファイヤを
 * 状態に応じて付け外しする。スニーク速度補正（{@code generic.sneaking_speed}、
 * 既定 0.3）はこの属性値に対して別途乗算されるため、イカ状態中の実効速度は
 * 「通常速度 × (1 + {@link #SQUID_SPEED_MULTIPLIER}) × スニーク補正」となり、
 * 通常歩行より明確に速くなる。</p>
 *
 * <p>{@code MOVEMENT_SPEED} はクライアント同期属性のため、サーバー側で
 * モディファイヤを付け外しするだけでクライアントの移動予測にも反映される。
 * モディファイヤは {@link #MODIFIER_ID} で一意に識別し、二重追加しない。</p>
 */
public final class SquidSpeedHandler {

    /**
     * イカ状態の移動速度倍率（{@code ADD_MULTIPLIED_TOTAL}）。
     * 3.0 の場合、スニーク補正込みで通常歩行の約1.2倍・通常スニークの約4倍になる。
     */
    public static final double SQUID_SPEED_MULTIPLIER = 3.0;

    /** モディファイヤの一意ID（冪等な付け外し・二重追加防止に使用）。 */
    private static final Identifier MODIFIER_ID = Salmon.id("squid_speed");

    private SquidSpeedHandler() {}

    /**
     * 状態に応じて移動速度モディファイヤを付け外しする。
     *
     * <p>毎tick呼び出し可能で冪等。リスポーン・次元移動で属性が
     * リセットされても次のtickで自己修復される。</p>
     *
     * @param player 対象プレイヤー
     * @param state  現在のインク状態
     */
    public static void apply(ServerPlayer player, PlayerInkState state) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        boolean squid = state == PlayerInkState.CROUCHING_ON_OWN_INK;
        if (squid) {
            if (!attribute.hasModifier(MODIFIER_ID)) {
                attribute.addOrUpdateTransientModifier(new AttributeModifier(
                        MODIFIER_ID,
                        SQUID_SPEED_MULTIPLIER,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else if (attribute.hasModifier(MODIFIER_ID)) {
            attribute.removeModifier(MODIFIER_ID);
        }
    }
}

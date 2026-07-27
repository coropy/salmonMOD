package yam.salmon.ink;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * インク所有者の解決を行う。
 *
 * <p>現段階ではプレイヤーUUIDをそのままインク所有者として返す。
 * 将来のPhaseでチームIDや色IDへ切り替える際は、このクラスだけを修正すればよい。</p>
 */
public final class InkSourceResolver {

    private InkSourceResolver() {}

    /**
     * プレイヤーからインク所有者UUIDを解決する。
     *
     * @param player サーバープレイヤー
     * @return インク所有者UUID（現段階ではプレイヤーUUIDと同一）
     */
    public static UUID resolveInkOwner(ServerPlayer player) {
        return player.getUUID();
    }
}
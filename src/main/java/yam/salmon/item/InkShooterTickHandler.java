package yam.salmon.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import yam.salmon.weapon.InkWeaponConfig;
import yam.salmon.weapon.InkWeaponRegistry;

/**
 * インクシューターのサーバーtickハンドラー。
 *
 * <p>毎サーバーtick、インクシューターを使用中の全プレイヤーを検査し、
 * 射撃間隔が経過していたら射撃を実行する。</p>
 *
 * <p>これにより、Item クラスの onUseTick のシグネチャ変更に依存せず、
 * 安定した連射制御を実現する。</p>
 */
public final class InkShooterTickHandler {

    private InkShooterTickHandler() {}

    /**
     * サーバーtickイベントに登録する。
     * ModInitializer から呼び出すこと。
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long serverTick = server.getTickCount();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (InkShooterItem.isUsingShooter(player)) {
                    InkShooterItem item = (InkShooterItem) player.getUseItem().getItem();
                    InkWeaponConfig config = item.getConfig();
                    InkShooterItem.fire(player, config, serverTick);
                }
            }
        });
    }
}

package yam.salmon.arena;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;

/**
 * アリーナ設定の権限チェックを一元管理するクラス。
 * 将来の権限システム変更に対応しやすいように1か所にまとめる。
 */
public class ArenaPermission {

    /**
     * 指定されたプレイヤーがアリーナの設定（マーカー選択・登録・解除）を行えるか判定する。
     * 初期版ではクリエイティブモードかつ権限レベル2以上が必要。
     */
    public static boolean canConfigure(Player player) {
        if (!player.isCreative()) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.permissions().hasPermission(
                    new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS));
        }
        return false;
    }
}
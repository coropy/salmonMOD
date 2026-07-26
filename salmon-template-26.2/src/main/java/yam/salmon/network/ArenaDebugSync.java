package yam.salmon.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;

import java.util.Collection;

/**
 * アリーナデバッグ表示用のサーバー→クライアント同期を担当する。
 */
public class ArenaDebugSync {

    /**
     * 指定プレイヤーに現在ディメンションの全アリーナ情報を送信する（完全同期）。
     */
    public static void sendFullSync(ServerPlayer player) {
        ServerLevel level = player.level();
        Collection<InkArena> arenas = InkArenaManager.getInstance().getArenas(level);

        Salmon.LOGGER.info("[Server] Sending arena snapshot: dimension={}, count={}",
                level.dimension().identifier(), arenas.size());

        // BEGIN マーカー
        ServerPlayNetworking.send(player, ArenaDebugPayload.fullSyncBegin());

        for (InkArena arena : arenas) {
            ServerPlayNetworking.send(player, new ArenaDebugPayload(
                    ArenaDebugPayload.ACTION_FULL_SYNC,
                    arena.getArenaNumber(),
                    arena.getArenaId(),
                    arena.getMin(),
                    arena.getMax(),
                    arena.getCornerA(),
                    arena.getCornerB(),
                    arena.getMarkerAId(),
                    arena.getMarkerBId()
            ));
        }

        // END マーカー
        ServerPlayNetworking.send(player, ArenaDebugPayload.fullSyncEnd());

        Salmon.LOGGER.info("Debug full sync sent to player {}: {} arenas in dimension {}",
                player.getUUID(), arenas.size(), level.dimension().identifier());
    }

    /**
     * アリーナ追加を指定プレイヤーに通知する。
     */
    public static void sendArenaAdded(ServerPlayer player, InkArena arena) {
        if (!player.level().dimension().equals(arena.getDimension())) {
            return;
        }
        ServerPlayNetworking.send(player, new ArenaDebugPayload(
                ArenaDebugPayload.ACTION_ADD,
                arena.getArenaNumber(),
                arena.getArenaId(),
                arena.getMin(),
                arena.getMax(),
                arena.getCornerA(),
                arena.getCornerB(),
                arena.getMarkerAId(),
                arena.getMarkerBId()
        ));
    }

    /**
     * アリーナ削除を指定プレイヤーに通知する。
     */
    public static void sendArenaRemoved(ServerPlayer player, InkArena arena) {
        if (!player.level().dimension().equals(arena.getDimension())) {
            return;
        }
        ServerPlayNetworking.send(player, new ArenaDebugPayload(
                ArenaDebugPayload.ACTION_REMOVE,
                arena.getArenaNumber(),
                arena.getArenaId(),
                arena.getMin(),
                arena.getMax(),
                arena.getCornerA(),
                arena.getCornerB(),
                arena.getMarkerAId(),
                arena.getMarkerBId()
        ));
    }

    /**
     * クライアントキャッシュ消去を指示する。
     */
    public static void sendClear(ServerPlayer player) {
        ServerPlayNetworking.send(player, new ArenaDebugPayload(
                ArenaDebugPayload.ACTION_CLEAR,
                0, java.util.UUID.randomUUID(),
                net.minecraft.core.BlockPos.ZERO, net.minecraft.core.BlockPos.ZERO,
                net.minecraft.core.BlockPos.ZERO, net.minecraft.core.BlockPos.ZERO,
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID()
        ));
    }

    // -----------------------------------------------------------------------
    // ブロードキャスト（同一ディメンション内の全デバッグ有効プレイヤー向け）
    // -----------------------------------------------------------------------

    /**
     * アリーナ追加を、同じディメンションにいる全デバッグ有効プレイヤーに通知する。
     */
    public static void broadcastArenaAdded(ServerLevel world, InkArena arena) {
        InkArenaManager manager = InkArenaManager.getInstance();
        for (ServerPlayer p : world.players()) {
            if (manager.isDebugEnabled(p)) {
                sendArenaAdded(p, arena);
            }
        }
        Salmon.LOGGER.info("Debug broadcast arena added: arenaNumber={}, dim={}",
                arena.getArenaNumber(), world.dimension().identifier());
    }

    /**
     * アリーナ削除を、同じディメンションにいる全デバッグ有効プレイヤーに通知する。
     */
    public static void broadcastArenaRemoved(ServerLevel world, InkArena arena) {
        InkArenaManager manager = InkArenaManager.getInstance();
        for (ServerPlayer p : world.players()) {
            if (manager.isDebugEnabled(p)) {
                sendArenaRemoved(p, arena);
            }
        }
        Salmon.LOGGER.info("Debug broadcast arena removed: arenaNumber={}, dim={}",
                arena.getArenaNumber(), world.dimension().identifier());
    }
}
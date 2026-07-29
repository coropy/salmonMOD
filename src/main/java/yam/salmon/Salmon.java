package yam.salmon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yam.salmon.arena.InkArenaManager;
import yam.salmon.block.InkAreaMarkerBlockEntity;
import yam.salmon.block.InkAreaMarkerBlock;
import yam.salmon.block.ModBlocks;
import yam.salmon.command.SalmonCommands;
import yam.salmon.item.InkShooterTickHandler;
import yam.salmon.item.ModItems;
import yam.salmon.weapon.InkWeaponRegistry;
import yam.salmon.network.ArenaDebugPayload;
import yam.salmon.network.ArenaDebugSync;
import yam.salmon.network.InkArenaClearPayload;
import yam.salmon.network.InkFaceUpdatePayload;
import yam.salmon.network.InkShotVisualPayload;
import yam.salmon.network.InkSyncBeginPayload;
import yam.salmon.network.InkSyncEndPayload;
import yam.salmon.network.InkSyncManager;
import yam.salmon.selection.PlayerMarkerSelectionManager;

public class Salmon implements ModInitializer {
    public static final String MOD_ID = "salmon";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Salmon MOD");

        ModBlocks.register();
        ModItems.register();
        InkAreaMarkerBlockEntity.register();
        InkShooterTickHandler.register();

        // 武器設定レジストリ初期化
        InkWeaponRegistry.registerDefaults();

        // ネットワークペイロードタイプ登録（S2C: サーバー→クライアント）
        PayloadTypeRegistry.clientboundPlay().register(ArenaDebugPayload.TYPE, ArenaDebugPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InkFaceUpdatePayload.TYPE, InkFaceUpdatePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InkSyncBeginPayload.TYPE, InkSyncBeginPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InkSyncEndPayload.TYPE, InkSyncEndPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InkArenaClearPayload.TYPE, InkArenaClearPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InkShotVisualPayload.TYPE, InkShotVisualPayload.STREAM_CODEC);

        // コマンド登録
        CommandRegistrationCallback.EVENT.register(SalmonCommands::register);

        // サーバー起動時にアリーナデータをロード
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            InkArenaManager.getInstance().onServerStarted(server);
        });

        // サーバー停止時にアリーナデータをセーブ
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            InkArenaManager.getInstance().onServerStopping();
        });

        // プレイヤー切断時に選択を解除
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PlayerMarkerSelectionManager.getInstance().clearAll();
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var playerId = handler.getPlayer().getUUID();
            PlayerMarkerSelectionManager.getInstance().clearSelection(playerId);
            // デバッグ表示も自動OFF
            InkArenaManager.getInstance().onPlayerDisconnect(playerId);
            // シューターのクールダウン情報をクリーンアップ
            yam.salmon.item.InkShooterItem.cleanup(handler.getPlayer());
        });

        // プレイヤー初回参加時にインク全量同期（AFTER_RESPAWN は初回参加時には発火しないため）
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            server.execute(() -> {
                // プレイヤーのネットワーク接続が確立した後に全量同期
                InkSyncManager.getInstance().sendFullSync(player);
                Salmon.LOGGER.info("Initial ink full sync on join: player={}, dim={}",
                        player.getUUID(), player.level().dimension().identifier());
            });
        });

        // マーカーブロック破壊制限: マーカーを持っている時のみ破壊可能
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (state.getBlock() instanceof InkAreaMarkerBlock) {
                if (!player.getMainHandItem().is(ModBlocks.INK_AREA_MARKER_BLOCK.asItem())
                        && !player.getOffhandItem().is(ModBlocks.INK_AREA_MARKER_BLOCK.asItem())) {
                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("インクエリアマーカーを持って破壊してください")
                    );
                    return false; // 破壊キャンセル
                }
            }
            return true;
        });

        // ブロック破壊時にマーカーの処理
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (blockEntity instanceof InkAreaMarkerBlockEntity marker) {
                marker.onBroken(player);
            }
        });

        // プレイヤーのリスポーン・次元移動時にデバッグ表示とインク表示を再同期
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            InkArenaManager manager = InkArenaManager.getInstance();
            if (manager.isDebugEnabled(newPlayer)) {
                ArenaDebugSync.sendFullSync(newPlayer);
                Salmon.LOGGER.info("Debug resync on respawn/world change: player={}, dim={}",
                        newPlayer.getUUID(), newPlayer.level().dimension().identifier());
            }
            // インクデータの完全同期（デバッグに関わらず常時）
            InkSyncManager.getInstance().sendFullSync(newPlayer);
        });

        LOGGER.info("Salmon MOD initialized successfully");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
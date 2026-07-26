package yam.salmon.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import yam.salmon.Salmon;
import yam.salmon.client.arena.ArenaDebugRenderer;
import yam.salmon.client.ink.ClientInkCache;
import yam.salmon.client.ink.InkRenderer;
import yam.salmon.network.ArenaDebugPayload;
import yam.salmon.network.InkArenaClearPayload;
import yam.salmon.network.InkFaceUpdatePayload;
import yam.salmon.network.InkSyncBeginPayload;
import yam.salmon.network.InkSyncEndPayload;

public class SalmonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Salmon.LOGGER.info("Salmon client initialized");

        // --- アリーナデバッグ表示用ペイロード受信登録 ---
        ClientPlayNetworking.registerGlobalReceiver(ArenaDebugPayload.TYPE, (payload, context) -> {
            var player = context.player();
            if (player == null) {
                return;
            }

            ArenaDebugRenderer.getInstance().handlePayload(
                    payload.action(),
                    payload.arenaNumber(),
                    payload.arenaUuid(),
                    payload.min(),
                    payload.max(),
                    payload.cornerA(),
                    payload.cornerB(),
                    payload.markerAId(),
                    payload.markerBId(),
                    player.level().dimension()
            );
        });

        Salmon.LOGGER.info("Arena debug networking initialized");

        // --- インク同期用ペイロード受信登録 ---

        // 完全同期開始
        ClientPlayNetworking.registerGlobalReceiver(InkSyncBeginPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientInkCache.getInstance().beginFullSync(
                        payload.dimensionId(),
                        payload.sessionId(),
                        payload.faceCount());
            });
        });

        // 面更新（差分 & 完全同期の一部）
        ClientPlayNetworking.registerGlobalReceiver(InkFaceUpdatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                var face = payload.getFace();
                if (face == null) {
                    Salmon.LOGGER.warn("[Client] Invalid face name in payload: {}", payload.faceName());
                    return;
                }
                if (payload.cells() == null || payload.cells().length != 64) {
                    Salmon.LOGGER.warn("[Client] Invalid cells in payload: {}",
                            payload.cells() != null ? payload.cells().length : 0);
                    return;
                }

                ClientInkCache.getInstance().applyFaceUpdate(
                        payload.arenaUuid(),
                        payload.arenaNumber(),
                        payload.dimensionId(),
                        payload.blockPos(),
                        face,
                        payload.cells(),
                        payload.revision());
            });
        });

        // 完全同期終了
        ClientPlayNetworking.registerGlobalReceiver(InkSyncEndPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientInkCache.getInstance().endFullSync(payload.sessionId());
            });
        });

        // アリーナクリア
        ClientPlayNetworking.registerGlobalReceiver(InkArenaClearPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientInkCache.getInstance().clearArena(
                        payload.arenaUuid(),
                        payload.dimensionId(),
                        payload.revision());
            });
        });

        Salmon.LOGGER.info("Ink networking initialized");

        // --- 抽出フェーズ ---
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            ArenaDebugRenderer.getInstance().extractDebugState(context);
            InkRenderer.getInstance().extractInkState(context);
        });

        // --- 描画フェーズ ---
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> {
            ArenaDebugRenderer.getInstance().renderAndDrawDebugState(context);
            InkRenderer.getInstance().renderAndDrawInkState(context);

            // 診断ログ（デバッグON時のみ）
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                ClientInkCache.getInstance().logDiagnostics(mc.player.level().dimension().identifier());
            }
        });

        Salmon.LOGGER.info("Arena debug renderer initialized");
        Salmon.LOGGER.info("Ink renderer initialized");
    }
}

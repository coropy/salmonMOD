package yam.salmon.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.world.phys.Vec3;
import yam.salmon.Salmon;
import yam.salmon.client.arena.ArenaDebugRenderer;
import yam.salmon.client.ink.ClientInkCache;
import yam.salmon.client.ink.InkRenderer;
import yam.salmon.client.shot.ClientInkShotManager;
import yam.salmon.client.shot.InkShotRenderer;
import yam.salmon.network.ArenaDebugPayload;
import yam.salmon.network.InkArenaClearPayload;
import yam.salmon.network.InkFaceUpdatePayload;
import yam.salmon.network.InkShotVisualPayload;
import yam.salmon.network.InkSyncBeginPayload;
import yam.salmon.network.InkSyncEndPayload;

public class SalmonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Salmon.LOGGER.info("Salmon client initialized");

        // --- アリーナデバッグ表示用ペイロード受信登録 ---
        ClientPlayNetworking.registerGlobalReceiver(ArenaDebugPayload.TYPE, (payload, context) -> {
            var player = context.player();
            if (player == null) return;

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

        // --- インク同期用ペイロード受信登録 ---
        ClientPlayNetworking.registerGlobalReceiver(InkSyncBeginPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientInkCache.getInstance().beginFullSync(
                        payload.dimensionId(), payload.sessionId(), payload.faceCount());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(InkFaceUpdatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                var face = payload.getFace();
                if (face == null) return;
                if (payload.cells() == null || payload.cells().length != 64) return;
                ClientInkCache.getInstance().applyFaceUpdate(
                        payload.arenaUuid(), payload.arenaNumber(), payload.dimensionId(),
                        payload.blockPos(), face, payload.cells(), payload.revision());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(InkSyncEndPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientInkCache.getInstance().endFullSync(payload.sessionId()));
        });

        ClientPlayNetworking.registerGlobalReceiver(InkArenaClearPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientInkCache.getInstance().clearArena(
                        payload.arenaUuid(), payload.dimensionId(), payload.revision());
            });
        });

        // --- 視覚弾道Payload受信 ---
        ClientPlayNetworking.registerGlobalReceiver(InkShotVisualPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientInkShotManager.getInstance().addFromPayload(
                        payload.start(), payload.end(),
                        payload.travelTicks(),
                        payload.colorRgb(), payload.size(), payload.hitType());
            });
        });

        Salmon.LOGGER.info("Ink networking initialized");

        // --- tick更新: 視覚弾道 ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                ClientInkShotManager.getInstance().clear();
                return;
            }
            ClientInkShotManager.getInstance().tick();
        });

        // --- 抽出フェーズ ---
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            ArenaDebugRenderer.getInstance().extractDebugState(context);
            InkRenderer.getInstance().extractInkState(context);
        });

        // --- 描画フェーズ ---
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> {
            ArenaDebugRenderer.getInstance().renderAndDrawDebugState(context);
            InkRenderer.getInstance().renderAndDrawInkState(context);

            // 視覚弾道描画
            var shots = ClientInkShotManager.getInstance().getActiveShots();
            if (!shots.isEmpty()) {
                InkShotRenderer.getInstance().render(shots, context, 1.0f);
            }

            // 診断ログ
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                ClientInkCache.getInstance().logDiagnostics(mc.player.level().dimension().identifier());
            }
        });

        Salmon.LOGGER.info("Renderers initialized");
    }
}
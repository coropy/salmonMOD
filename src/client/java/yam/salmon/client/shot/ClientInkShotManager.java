package yam.salmon.client.shot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import yam.salmon.network.InkShotImpactPayload;
import yam.salmon.network.InkShotSpawnPayload;
import yam.salmon.network.InkTrailDropImpactPayload;
import yam.salmon.network.InkTrailDropSpawnPayload;
import yam.salmon.weapon.ProjectileFinishReason;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * クライアント側の主弾・トレイル滴管理シングルトン。
 *
 * <p>Spawn Payloadで生成、毎tick物理更新、Impact Payloadで終了。
 * サーバーと同じ物理計算により、ネットワーク遅延があっても
 * 実体と視覚のタイミングが一致する。</p>
 */
public final class ClientInkShotManager {
    private static final ClientInkShotManager INSTANCE = new ClientInkShotManager();

    private static final int MAX_ACTIVE_SHOTS = 512;
    private static final int MAX_ACTIVE_DROPS = 256;

    private final List<ClientInkShot> activeShots = new ArrayList<>();
    private final List<ClientInkTrailDrop> activeDrops = new ArrayList<>();

    private ClientInkShotManager() {}

    public static ClientInkShotManager getInstance() { return INSTANCE; }

    // ===================================================================
    // Payload受信
    // ===================================================================

    public void addShotSpawn(InkShotSpawnPayload payload) {
        while (activeShots.size() >= MAX_ACTIVE_SHOTS) activeShots.remove(0);

        ClientInkShot shot = new ClientInkShot(
                payload.shotId(),
                payload.startPosition(),
                payload.initialVelocity(),
                payload.gravity(),
                payload.colorRgb(),
                payload.size());

        // サーバー時刻による遅延補正
        long clientTime = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() : 0;
        long elapsed = Math.max(0L, clientTime - payload.serverSpawnGameTime());
        for (long i = 0; i < elapsed; i++) {
            if (!shot.tick()) break;
        }

        activeShots.add(shot);
    }

    public void applyShotImpact(InkShotImpactPayload payload) {
        for (ClientInkShot shot : activeShots) {
            if (shot.shotId().equals(payload.shotId()) && !shot.isImpactReceived()) {
                shot.applyImpact(payload.impactPosition());
                if (payload.finishReason() == ProjectileFinishReason.BLOCK_HIT
                        || payload.finishReason() == ProjectileFinishReason.ENTITY_HIT) {
                    spawnArrivalEffect(payload.impactPosition());
                }
                return;
            }
        }
    }

    public void addDropSpawn(InkTrailDropSpawnPayload payload) {
        while (activeDrops.size() >= MAX_ACTIVE_DROPS) activeDrops.remove(0);

        ClientInkTrailDrop drop = new ClientInkTrailDrop(
                payload.dropId(), payload.parentShotId(),
                payload.startPosition(), payload.initialVelocity(),
                payload.gravity(), payload.size(), payload.colorRgb());

        // サーバー時刻による遅延補正
        long clientTime = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() : 0;
        long elapsed = Math.max(0L, clientTime - payload.serverSpawnGameTime());
        for (long i = 0; i < elapsed; i++) {
            if (!drop.tick()) break;
        }

        activeDrops.add(drop);
    }

    public void applyDropImpact(InkTrailDropImpactPayload payload) {
        for (ClientInkTrailDrop drop : activeDrops) {
            if (drop.dropId().equals(payload.dropId()) && !drop.isImpactReceived()) {
                drop.applyImpact(payload.impactPosition());
                spawnArrivalEffect(payload.impactPosition());
                return;
            }
        }
    }

    // ===================================================================
    // tick
    // ===================================================================

    public void tick() {
        Iterator<ClientInkShot> it = activeShots.iterator();
        while (it.hasNext()) {
            ClientInkShot shot = it.next();
            if (!shot.tick()) {
                it.remove();
            }
        }
        Iterator<ClientInkTrailDrop> dit = activeDrops.iterator();
        while (dit.hasNext()) {
            ClientInkTrailDrop drop = dit.next();
            if (!drop.tick()) {
                it.remove();
            }
        }
    }

    public void clear() {
        activeShots.clear();
        activeDrops.clear();
    }

    public List<ClientInkShot> getActiveShots() { return activeShots; }
    public List<ClientInkTrailDrop> getActiveDrops() { return activeDrops; }

    private void spawnArrivalEffect(Vec3 pos) {
        var client = Minecraft.getInstance();
        if (client.level == null) return;
        var random = client.level.getRandom();
        for (int i = 0; i < 3; i++) {
            client.level.addParticle(ParticleTypes.SPLASH,
                    pos.x, pos.y, pos.z,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1);
        }
    }
}
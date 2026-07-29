package yam.salmon.client.shot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import yam.salmon.network.InkShotVisualPayload;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ClientInkShotManager {
    private static final ClientInkShotManager INSTANCE = new ClientInkShotManager();

    private static final int MAX_ACTIVE_SHOTS = 512;
    private static final int MAX_ACTIVE_DROPS = 256;

    private final List<ClientInkShot> activeShots = new ArrayList<>();
    private final List<ClientInkTrailDrop> activeDrops = new ArrayList<>();

    private ClientInkShotManager() {}

    public static ClientInkShotManager getInstance() { return INSTANCE; }

    public void addFromPayload(InkShotVisualPayload payload) {
        // 主弾
        while (activeShots.size() >= MAX_ACTIVE_SHOTS) activeShots.remove(0);
        ClientInkShot shot = new ClientInkShot(
                payload.trajectoryPoints(), payload.totalTicks(),
                payload.colorRgb(), payload.size(), payload.hitType());
        activeShots.add(shot);

        // トレイル滴
        if (payload.trailDrops() != null) {
            for (var drop : payload.trailDrops()) {
                while (activeDrops.size() >= MAX_ACTIVE_DROPS) activeDrops.remove(0);
                ClientInkTrailDrop trailDrop = new ClientInkTrailDrop(
                        drop.start(), drop.end(), drop.travelTicks(),
                        payload.colorRgb(), drop.size());
                activeDrops.add(trailDrop);
            }
        }
    }

    @Deprecated
    public void addFromPayload(List<Vec3> trajectoryPoints, int totalTicks,
                                int colorRgb, float size, byte hitType) {
        while (activeShots.size() >= MAX_ACTIVE_SHOTS) activeShots.remove(0);
        activeShots.add(new ClientInkShot(trajectoryPoints, totalTicks, colorRgb, size, hitType));
    }

    public void tick() {
        Iterator<ClientInkShot> it = activeShots.iterator();
        while (it.hasNext()) {
            ClientInkShot shot = it.next();
            if (!shot.tick()) {
                if (shot.hitType() == 1 || shot.hitType() == 2) spawnArrivalEffect(shot.end());
                it.remove();
            }
        }
        Iterator<ClientInkTrailDrop> dit = activeDrops.iterator();
        while (dit.hasNext()) {
            ClientInkTrailDrop drop = dit.next();
            if (!drop.tick()) {
                spawnArrivalEffect(drop.end());
                dit.remove();
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

package yam.salmon.client.shot;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import yam.salmon.weapon.InkShooterVisualConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * クライアント側の視覚弾道管理マネージャー。
 *
 * <p>アクティブな弾道のリストを管理し、tick更新、ワールド切り替えクリアを行う。
 * スレッドセーフのため、リスト操作はクライアントメインスレッドからのみ行う。</p>
 */
public final class ClientInkShotManager {
    private static final ClientInkShotManager INSTANCE = new ClientInkShotManager();

    /** アクティブ弾数上限 */
    private static final int MAX_ACTIVE_SHOTS = 512;

    private final List<ClientInkShot> activeShots = new ArrayList<>();

    private ClientInkShotManager() {}

    public static ClientInkShotManager getInstance() {
        return INSTANCE;
    }

    /**
     * 視覚弾を追加する。
     * Payload 受信時に呼ばれる。
     */
    public void addFromPayload(Vec3 start, Vec3 end, int travelTicks,
                                int colorRgb, float size, byte hitType) {
        // 上限超過時は古いものから削除
        while (activeShots.size() >= MAX_ACTIVE_SHOTS) {
            activeShots.remove(0);
        }

        InkShooterVisualConfig vis = InkShooterVisualConfig.DEFAULT;
        ClientInkShot shot = new ClientInkShot(
                start, end, travelTicks,
                colorRgb, size, hitType,
                vis.arcHeight());
        activeShots.add(shot);
    }

    /**
     * 毎tick呼ばれる。全弾道を更新し、終了したものを削除する。
     */
    public void tick() {
        Iterator<ClientInkShot> it = activeShots.iterator();
        while (it.hasNext()) {
            ClientInkShot shot = it.next();
            if (!shot.tick()) {
                // 到達時の視覚着弾エフェクト（クライアント側）
                if (shot.hitType() == 1 || shot.hitType() == 2) {
                    spawnArrivalEffect(shot);
                }
                it.remove();
            }
        }
    }

    /**
     * 全弾道をクリアする（ワールド切り替え時など）。
     */
    public void clear() {
        activeShots.clear();
    }

    /**
     * アクティブな弾道リストを返す（描画用・読み取り専用）。
     */
    public List<ClientInkShot> getActiveShots() {
        return activeShots;
    }

    /**
     * クライアント側で視覚弾到達時のエフェクト（簡易パーティクル）を生成する。
     */
    private void spawnArrivalEffect(ClientInkShot shot) {
        var client = Minecraft.getInstance();
        if (client.level == null) return;

        // 着弾位置で簡易な splash パーティクル
        Vec3 pos = shot.end();
        var random = client.level.getRandom();
        for (int i = 0; i < 5; i++) {
            client.level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.SPLASH,
                    pos.x, pos.y, pos.z,
                    (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2);
        }
    }
}
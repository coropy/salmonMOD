package yam.salmon.client.shot;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * クライアント側の視覚主弾。
 *
 * <p>サーバーからSpawn Payloadを受信して生成され、毎tickクライアント側で
 * サーバーと同じ物理計算を行って位置を更新する。
 * Impact Payload受信までは表示を継続し、受信後は最低1フレーム表示してから消滅する。</p>
 */
public final class ClientInkShot {
    private static final int CLIENT_HARD_SAFETY_TICKS = 1200;

    private final UUID shotId;
    private final int colorRgb;
    private final float size;
    private final double gravity;

    private Vec3 position;
    private Vec3 previousPosition;
    private Vec3 velocity;

    private int age;
    private boolean impactReceived;
    private Vec3 impactPosition;
    private int impactGraceTicks;
    private boolean alive;

    public ClientInkShot(UUID shotId, Vec3 startPosition, Vec3 initialVelocity,
                          double gravity, int colorRgb, float size) {
        this.shotId = shotId;
        this.position = startPosition;
        this.previousPosition = startPosition;
        this.velocity = initialVelocity;
        this.gravity = gravity;
        this.colorRgb = colorRgb;
        this.size = size;
        this.age = 0;
        this.impactReceived = false;
        this.impactGraceTicks = 0;
        this.alive = true;
    }

    /**
     * 毎tick呼ばれる。false を返すと消滅。
     */
    public boolean tick() {
        if (!alive) return false;

        previousPosition = position;

        if (impactReceived) {
            impactGraceTicks--;
            if (impactGraceTicks <= 0) {
                alive = false;
                return false;
            }
            // Impact受信後は静止表示
            return true;
        }

        // サーバーと同じ物理更新
        position = position.add(velocity);
        velocity = velocity.add(0.0, -gravity, 0.0);
        age++;

        // クライアント安全上限（通信異常時の保険）
        if (age >= CLIENT_HARD_SAFETY_TICKS) {
            alive = false;
            return false;
        }

        return true;
    }

    /**
     * Impact Payloadを受信した時、最終位置を補正して終了シーケンスに入る。
     */
    public void applyImpact(Vec3 serverImpactPosition) {
        this.previousPosition = this.position;
        this.position = serverImpactPosition;
        this.impactPosition = serverImpactPosition;
        this.impactReceived = true;
        this.impactGraceTicks = 1; // 最低1フレーム表示
    }

    /**
     * partial tick を考慮した描画位置を返す。
     */
    public Vec3 getRenderPosition(float partialTick) {
        return previousPosition.lerp(position, partialTick);
    }

    public UUID shotId() { return shotId; }
    public boolean isAlive() { return alive; }
    public int age() { return age; }
    public int colorRgb() { return colorRgb; }
    public float size() { return size; }
    public Vec3 start() { return position; }
    public Vec3 end() { return impactReceived ? impactPosition : position; }
    public boolean isImpactReceived() { return impactReceived; }
}
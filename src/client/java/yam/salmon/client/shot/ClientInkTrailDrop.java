package yam.salmon.client.shot;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * クライアント側のトレイル滴ビジュアル。
 *
 * <p>サーバーからSpawn Payloadを受信して生成され、毎tickクライアント側で
 * サーバーと同じ物理計算を行って位置を更新する。
 * Impact Payload受信までは表示を継続し、受信後は最低1フレーム表示してから消滅する。</p>
 */
public final class ClientInkTrailDrop {
    private static final int CLIENT_HARD_DROP_SAFETY_TICKS = 300;

    private final UUID dropId;
    private final UUID parentShotId;
    private final float size;
    private final double gravity;
    private final int colorRgb;

    private Vec3 position;
    private Vec3 previousPosition;
    private Vec3 velocity;

    private int age;
    private boolean impactReceived;
    private int impactGraceTicks;
    private boolean alive;

    public ClientInkTrailDrop(UUID dropId, UUID parentShotId,
                               Vec3 startPosition, Vec3 initialVelocity,
                               double gravity, float size, int colorRgb) {
        this.dropId = dropId;
        this.parentShotId = parentShotId;
        this.position = startPosition;
        this.previousPosition = startPosition;
        this.velocity = initialVelocity;
        this.gravity = gravity;
        this.size = size;
        this.colorRgb = colorRgb;
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
            return true;
        }

        // サーバーと同じ物理更新
        position = position.add(velocity);
        velocity = velocity.add(0.0, -gravity, 0.0);
        age++;

        if (age >= CLIENT_HARD_DROP_SAFETY_TICKS) {
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
        this.impactReceived = true;
        this.impactGraceTicks = 1;
    }

    public Vec3 getRenderPosition(float partialTick) {
        return previousPosition.lerp(position, partialTick);
    }

    public UUID dropId() { return dropId; }
    public UUID parentShotId() { return parentShotId; }
    public boolean isAlive() { return alive; }
    public float size() { return size; }
    public int colorRgb() { return colorRgb; }
    public Vec3 end() { return position; }
    public boolean isImpactReceived() { return impactReceived; }
}
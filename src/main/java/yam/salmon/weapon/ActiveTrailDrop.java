package yam.salmon.weapon;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * サーバー側で毎tick管理されるトレイル滴の状態。
 *
 * <p>主弾の飛行中に生成され、{@link InkProjectileLifecycleManager} により
 * 毎tick物理更新・衝突判定が行われる。
 * 塗装は実際の衝突tickまで実行されない。
 * 親弾が終了しても滴は独立して管理される。</p>
 */
public final class ActiveTrailDrop {
    private final UUID dropId;
    private final UUID parentShotId;
    private final UUID shooterId;
    private final ResourceKey<Level> dimension;
    private final double gravity;
    private final long spawnGameTime;
    private final float visualSize;
    private final double paintRadius;
    private final int colorRgb;

    private Vec3 position;
    private Vec3 velocity;
    private int age;
    private boolean finished;
    private ProjectileFinishReason finishReason;

    /** 絶対安全上限（滴用） */
    static final int HARD_DROP_SAFETY_TICKS = 300;

    /** ワールド外判定マージン */
    static final int OUT_OF_WORLD_MARGIN = 64;

    ActiveTrailDrop(UUID dropId, UUID parentShotId, UUID shooterId,
                    ResourceKey<Level> dimension, Vec3 position,
                    Vec3 velocity, double gravity, long spawnGameTime,
                    float visualSize, double paintRadius, int colorRgb) {
        this.dropId = dropId;
        this.parentShotId = parentShotId;
        this.shooterId = shooterId;
        this.dimension = dimension;
        this.position = position;
        this.velocity = velocity;
        this.gravity = gravity;
        this.spawnGameTime = spawnGameTime;
        this.visualSize = visualSize;
        this.paintRadius = paintRadius;
        this.age = 0;
        this.finished = false;
        this.finishReason = ProjectileFinishReason.ALIVE;
        this.colorRgb = colorRgb;
    }

    // ---- accessors ----

    public UUID dropId() { return dropId; }
    public UUID parentShotId() { return parentShotId; }
    public UUID shooterId() { return shooterId; }
    public ResourceKey<Level> dimension() { return dimension; }
    public double gravity() { return gravity; }
    public long spawnGameTime() { return spawnGameTime; }
    public float visualSize() { return visualSize; }
    public double paintRadius() { return paintRadius; }
    public int colorRgb() { return colorRgb; }

    public Vec3 position() { return position; }
    public void setPosition(Vec3 position) { this.position = position; }

    public Vec3 velocity() { return velocity; }
    public void setVelocity(Vec3 velocity) { this.velocity = velocity; }

    public int age() { return age; }
    public void incrementAge() { this.age++; }

    public boolean isFinished() { return finished; }
    public void markFinished() { this.finished = true; }

    public ProjectileFinishReason finishReason() { return finishReason; }
    public void setFinishReason(ProjectileFinishReason reason) { this.finishReason = reason; }
}
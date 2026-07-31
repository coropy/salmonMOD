package yam.salmon.weapon;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * サーバー側で毎tick管理される主弾の状態。
 *
 * <p>発射時に作成され、{@link InkProjectileLifecycleManager} により
 * 毎tick物理更新・衝突判定が行われる。
 * 塗装やEntityダメージは実際の衝突tickまで実行されない。</p>
 */
public final class ActiveInkShot {
    private final UUID shotId;
    private final UUID shooterId;
    private final ResourceKey<Level> dimension;
    private final InkWeaponConfig config;
    private final double gravity;
    private final long spawnGameTime;
    private final RandomSource random;
    private final int substepsPerTick;

    private Vec3 position;
    private Vec3 velocity;
    private int age;
    private boolean finished;
    private int generatedDropCount;
    private ProjectileFinishReason finishReason;

    /** サーバー時刻による遅延補正に使える、クライアント表示用フィールド */
    private Vec3 spawnPosition;
    private Vec3 initialVelocity;
    private int visualColorRgb;

    ActiveInkShot(UUID shotId, UUID shooterId, ResourceKey<Level> dimension,
                  Vec3 position, Vec3 velocity, InkWeaponConfig config,
                  long spawnGameTime, RandomSource random, int visualColorRgb) {
        this.shotId = shotId;
        this.shooterId = shooterId;
        this.dimension = dimension;
        this.config = config;
        this.position = position;
        this.velocity = velocity;
        this.gravity = config.gravityPerTick();
        this.spawnGameTime = spawnGameTime;
        this.random = random;
        this.substepsPerTick = config.trajectorySubstepsPerTick();
        this.age = 0;
        this.finished = false;
        this.generatedDropCount = 0;
        this.finishReason = ProjectileFinishReason.ALIVE;
        this.spawnPosition = position;
        this.initialVelocity = velocity;
        this.visualColorRgb = visualColorRgb;
    }

    // ---- accessors ----

    public UUID shotId() { return shotId; }
    public UUID shooterId() { return shooterId; }
    public ResourceKey<Level> dimension() { return dimension; }
    public InkWeaponConfig config() { return config; }
    public double gravity() { return gravity; }
    public long spawnGameTime() { return spawnGameTime; }
    public RandomSource random() { return random; }
    public int substepsPerTick() { return substepsPerTick; }

    public Vec3 position() { return position; }
    public void setPosition(Vec3 position) { this.position = position; }

    public Vec3 velocity() { return velocity; }
    public void setVelocity(Vec3 velocity) { this.velocity = velocity; }

    public int age() { return age; }
    public void incrementAge() { this.age++; }

    public boolean isFinished() { return finished; }
    public void markFinished() { this.finished = true; }

    public int generatedDropCount() { return generatedDropCount; }
    public void incrementDropCount() { this.generatedDropCount++; }

    public ProjectileFinishReason finishReason() { return finishReason; }
    public void setFinishReason(ProjectileFinishReason reason) { this.finishReason = reason; }

    public Vec3 spawnPosition() { return spawnPosition; }
    public Vec3 initialVelocity() { return initialVelocity; }
    public int visualColorRgb() { return visualColorRgb; }

    /** 安全上限tick数 */
    public int hardSafetyMaxTicks() {
        return Math.min(config.maxFlightTicks(), 600);
    }
}
package yam.salmon.client.shot;

import net.minecraft.world.phys.Vec3;

/**
 * クライアント側の視覚弾道1発分のデータ。
 *
 * <p>tickごとに位置を更新し、到達後は消滅する。</p>
 */
public final class ClientInkShot {
    private final Vec3 start;
    private final Vec3 end;
    private final int totalTicks;
    private final int colorRgb;
    private final float size;
    private final byte hitType;
    private final double arcHeight;

    private int age;
    private Vec3 previousPosition;
    private Vec3 currentPosition;
    private boolean alive = true;

    public ClientInkShot(Vec3 start, Vec3 end, int totalTicks, int colorRgb,
                          float size, byte hitType, double arcHeight) {
        this.start = start;
        this.end = end;
        this.totalTicks = totalTicks;
        this.colorRgb = colorRgb;
        this.size = size;
        this.hitType = hitType;
        this.arcHeight = arcHeight;
        this.age = 0;
        this.currentPosition = start;
        this.previousPosition = start;
    }

    /**
     * 毎tick呼ばれる。false を返すと消滅。
     */
    public boolean tick() {
        if (!alive) return false;

        previousPosition = currentPosition;
        age++;

        if (age >= totalTicks) {
            alive = false;
            return false;
        }

        double progress = (double) age / totalTicks;
        Vec3 linear = start.lerp(end, progress);
        double arc = 4.0 * progress * (1.0 - progress) * arcHeight;
        currentPosition = linear.add(0.0, arc, 0.0);

        return true;
    }

    /**
     * partial tick を考慮した描画位置を返す。
     */
    public Vec3 getRenderPosition(float partialTick) {
        return previousPosition.lerp(currentPosition, partialTick);
    }

    public boolean isAlive() { return alive; }
    public int age() { return age; }
    public int totalTicks() { return totalTicks; }
    public int colorRgb() { return colorRgb; }
    public float size() { return size; }
    public byte hitType() { return hitType; }
    public Vec3 start() { return start; }
    public Vec3 end() { return end; }
    public double arcHeight() { return arcHeight; }
}
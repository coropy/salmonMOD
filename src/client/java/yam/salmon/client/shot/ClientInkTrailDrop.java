package yam.salmon.client.shot;

import net.minecraft.world.phys.Vec3;

/**
 * クライアント側のトレイル滴ビジュアル。
 *
 * <p>開始位置から着弾位置まで加速落下する短命な視覚滴。
 * 主弾とは独立して描画される。</p>
 */
public final class ClientInkTrailDrop {
    private final Vec3 start;
    private final Vec3 end;
    private final int totalTicks;
    private final int colorRgb;
    private final float size;

    private int age;
    private Vec3 previousPosition;
    private Vec3 currentPosition;
    private boolean alive = true;

    public ClientInkTrailDrop(Vec3 start, Vec3 end, int totalTicks, int colorRgb, float size) {
        this.start = start;
        this.end = end;
        this.totalTicks = Math.max(1, totalTicks);
        this.colorRgb = colorRgb;
        this.size = size;
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

        // 加速落下: progress^2 で easing
        double progress = (double) age / totalTicks;
        double eased = progress * progress;
        currentPosition = start.lerp(end, eased);

        return true;
    }

    public Vec3 getRenderPosition(float partialTick) {
        return previousPosition.lerp(currentPosition, partialTick);
    }

    public boolean isAlive() { return alive; }
    public int colorRgb() { return colorRgb; }
    public float size() { return size; }
    public Vec3 end() { return end; }
}
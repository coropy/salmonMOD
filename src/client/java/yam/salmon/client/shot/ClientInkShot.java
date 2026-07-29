package yam.salmon.client.shot;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * クライアント側の視覚弾道1発分のデータ。
 *
 * <p>サーバーから受信した軌道制御点を線形補間して表示する。
 * tickごとに軌道に沿って位置を更新し、到達後は消滅する。</p>
 */
public final class ClientInkShot {
    private final List<Vec3> trajectoryPoints;
    private final int totalTicks;
    private final int colorRgb;
    private final float size;
    private final byte hitType;

    private int age;
    private Vec3 previousPosition;
    private Vec3 currentPosition;
    private boolean alive = true;

    /** 制御点間の各区間に対応するtick境界を事前計算 */
    private final double[] segmentEndProgress; // [0..1] の各制御点に対応する進捗

    public ClientInkShot(List<Vec3> trajectoryPoints, int totalTicks, int colorRgb,
                          float size, byte hitType) {
        this.trajectoryPoints = trajectoryPoints;
        this.totalTicks = Math.max(1, totalTicks);
        this.colorRgb = colorRgb;
        this.size = size;
        this.hitType = hitType;
        this.age = 0;
        this.currentPosition = trajectoryPoints.isEmpty() ? Vec3.ZERO : trajectoryPoints.get(0);
        this.previousPosition = this.currentPosition;

        // 制御点間の進捗を計算（等分割）
        this.segmentEndProgress = computeProgressFromPoints(trajectoryPoints);
    }

    /**
     * 軌道制御点の累積距離から各ポイントの進捗 [0..1] を計算する。
     */
    private static double[] computeProgressFromPoints(List<Vec3> points) {
        int n = points.size();
        if (n <= 1) return new double[]{1.0};

        // 累積距離を計算
        double[] cumulativeDist = new double[n];
        cumulativeDist[0] = 0.0;
        for (int i = 1; i < n; i++) {
            cumulativeDist[i] = cumulativeDist[i - 1] + points.get(i - 1).distanceTo(points.get(i));
        }

        double totalDist = cumulativeDist[n - 1];
        if (totalDist <= 0.0) {
            // 距離がゼロなら等分割
            double[] equal = new double[n];
            for (int i = 0; i < n; i++) {
                equal[i] = (double) (i + 1) / n;
            }
            return equal;
        }

        double[] progress = new double[n];
        for (int i = 0; i < n; i++) {
            progress[i] = cumulativeDist[i] / totalDist;
        }
        return progress;
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

        // 軌道制御点に沿った位置を計算
        currentPosition = interpolateAlongTrajectory(progress);

        return true;
    }

    /**
     * 進捗 [0..1] に対応する軌道上の位置を線形補間で計算する。
     */
    private Vec3 interpolateAlongTrajectory(double progress) {
        int n = trajectoryPoints.size();
        if (n == 0) return Vec3.ZERO;
        if (n == 1) return trajectoryPoints.get(0);

        // progress がセグメント境界を超えた場合の処理
        progress = Math.max(0.0, Math.min(1.0, progress));

        // どのセグメントかを特定
        for (int i = 1; i < n; i++) {
            if (progress <= segmentEndProgress[i]) {
                double segStart = segmentEndProgress[i - 1];
                double segEnd = segmentEndProgress[i];
                double localProgress = (segEnd - segStart > 0.0)
                        ? (progress - segStart) / (segEnd - segStart)
                        : 0.0;
                localProgress = Math.max(0.0, Math.min(1.0, localProgress));

                Vec3 p0 = trajectoryPoints.get(i - 1);
                Vec3 p1 = trajectoryPoints.get(i);
                return p0.lerp(p1, localProgress);
            }
        }

        // 最終制御点（通常ここには到達しない）
        return trajectoryPoints.get(n - 1);
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
    public Vec3 start() { return trajectoryPoints.isEmpty() ? Vec3.ZERO : trajectoryPoints.get(0); }
    public Vec3 end() { return trajectoryPoints.isEmpty() ? Vec3.ZERO : trajectoryPoints.get(trajectoryPoints.size() - 1); }
}
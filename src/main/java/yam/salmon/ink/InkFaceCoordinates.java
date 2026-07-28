package yam.salmon.ink;

import net.minecraft.core.Direction;

/**
 * ブロック面へのヒット座標から UV座標・セル座標への変換ユーティリティ。
 *
 * <p>6面すべてで一貫した UV方向を定義する。
 * Uは右方向、Vは上方向を基本とする（面を正面から見たとき）。</p>
 *
 * @param u        UV座標 U (0.0..1.0)
 * @param v        UV座標 V (0.0..1.0)
 * @param cellU    セル座標 U (0..7)
 * @param cellV    セル座標 V (0..7)
 * @param cellIndex セルインデックス (0..63)
 */
public record InkFaceCoordinates(
        double u,
        double v,
        int cellU,
        int cellV,
        int cellIndex
) {
    public static final int GRID_SIZE = InkFaceData.GRID_SIZE;

    /**
     * BlockHitResult のローカル座標から面座標を計算する。
     *
     * @param face   ヒットした面
     * @param localX ブロック内ローカル X (0.0..1.0)
     * @param localY ブロック内ローカル Y (0.0..1.0)
     * @param localZ ブロック内ローカル Z (0.0..1.0)
     */
    public static InkFaceCoordinates fromHit(Direction face,
                                              double localX, double localY, double localZ) {
        double u, v;

        switch (face) {
            case UP -> {
                u = localX;
                v = localZ;
            }
            case DOWN -> {
                u = localX;
                v = 1.0 - localZ;
            }
            case NORTH -> {
                u = 1.0 - localX;
                v = 1.0 - localY;
            }
            case SOUTH -> {
                u = localX;
                v = 1.0 - localY;
            }
            case WEST -> {
                u = localZ;
                v = 1.0 - localY;
            }
            case EAST -> {
                u = 1.0 - localZ;
                v = 1.0 - localY;
            }
            default -> throw new IllegalArgumentException("Unknown face: " + face);
        }

        // 浮動小数点誤差対応
        u = clampToRange(u, 0.0, 1.0);
        v = clampToRange(v, 0.0, 1.0);

        int cellU = clampCell((int) Math.floor(u * GRID_SIZE));
        int cellV = clampCell((int) Math.floor(v * GRID_SIZE));
        int cellIndex = cellV * GRID_SIZE + cellU;

        return new InkFaceCoordinates(u, v, cellU, cellV, cellIndex);
    }

    private static int clampCell(int cell) {
        return Math.min(GRID_SIZE - 1, Math.max(0, cell));
    }

    private static double clampToRange(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
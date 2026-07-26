package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * ブロック面の8×8グリッドセルのワールド座標AABBを計算するユーティリティ。
 *
 * <p>サーバー側の {@link InkFaceCoordinates} と同じUV定義を使用し、
 * クライアント側描画との一貫性を保証する。</p>
 *
 * <p>ブロック表面からわずかに外側へオフセットし、Z-fightingを回避する。</p>
 */
public final class InkCellGeometry {

    /** セルの面サイズ（1/8ブロック） */
    public static final double CELL_SIZE = 1.0 / InkFaceData.GRID_SIZE;

    /** インクの厚さ（ブロック単位） */
    public static final double INK_THICKNESS = 0.008;

    /** ブロック表面からのオフセット（Z-fighting回避） */
    public static final double SURFACE_OFFSET = 0.002;

    private InkCellGeometry() {}

    /**
     * 指定された面のセル (cellU, cellV) に対応するワールド座標系AABBを返す。
     *
     * @param blockPos ブロックのワールド座標
     * @param face     面の方向
     * @param cellU    セルU座標 (0..7)
     * @param cellV    セルV座標 (0..7)
     * @return double[6] = {minX, minY, minZ, maxX, maxY, maxZ}（ワールド座標）
     */
    public static double[] getCellBounds(BlockPos blockPos, Direction face, int cellU, int cellV) {
        double baseX = blockPos.getX();
        double baseY = blockPos.getY();
        double baseZ = blockPos.getZ();

        // UV → ブロックローカル座標への変換（InkFaceCoordinates と一致）
        // Uは右方向、Vは上方向（面を正面から見たとき）
        double uMin = cellU * CELL_SIZE;
        double uMax = (cellU + 1) * CELL_SIZE;
        double vMin = cellV * CELL_SIZE;
        double vMax = (cellV + 1) * CELL_SIZE;

        return switch (face) {
            case UP -> new double[] {
                baseX + uMin,
                baseY + 1.0 + SURFACE_OFFSET,
                baseZ + vMin,
                baseX + uMax,
                baseY + 1.0 + SURFACE_OFFSET + INK_THICKNESS,
                baseZ + vMax
            };
            case DOWN -> new double[] {
                baseX + uMin,
                baseY - SURFACE_OFFSET - INK_THICKNESS,
                baseZ + 1.0 - vMax,  // DOWN: v = 1.0 - localZ の逆変換
                baseX + uMax,
                baseY - SURFACE_OFFSET,
                baseZ + 1.0 - vMin
            };
            case NORTH -> new double[] {
                baseX + 1.0 - uMax,  // NORTH: u = 1.0 - localX の逆変換
                baseY + 1.0 - vMax,  // NORTH: v = 1.0 - localY の逆変換
                baseZ - SURFACE_OFFSET - INK_THICKNESS,
                baseX + 1.0 - uMin,
                baseY + 1.0 - vMin,
                baseZ - SURFACE_OFFSET
            };
            case SOUTH -> new double[] {
                baseX + uMin,
                baseY + 1.0 - vMax,  // SOUTH: v = 1.0 - localY の逆変換
                baseZ + 1.0 + SURFACE_OFFSET,
                baseX + uMax,
                baseY + 1.0 - vMin,
                baseZ + 1.0 + SURFACE_OFFSET + INK_THICKNESS
            };
            case WEST -> new double[] {
                baseX - SURFACE_OFFSET - INK_THICKNESS,
                baseY + 1.0 - vMax,  // WEST: v = 1.0 - localY の逆変換
                baseZ + uMin,         // WEST: u = localZ
                baseX - SURFACE_OFFSET,
                baseY + 1.0 - vMin,
                baseZ + uMax
            };
            case EAST -> new double[] {
                baseX + 1.0 + SURFACE_OFFSET,
                baseY + 1.0 - vMax,     // EAST: v = 1.0 - localY の逆変換
                baseZ + 1.0 - uMax,     // EAST: u = 1.0 - localZ の逆変換
                baseX + 1.0 + SURFACE_OFFSET + INK_THICKNESS,
                baseY + 1.0 - vMin,
                baseZ + 1.0 - uMin
            };
        };
    }
}
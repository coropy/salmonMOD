package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * ブロック面に対応する連続的なワールド平面座標。
 *
 * <p>クリック面に応じて、ワールド座標（Vec3）を2次元平面座標（planeU, planeV）へ変換する。
 * 隣接ブロック境界を越えても連続した座標になり、複数ブロックにまたがる塗装を可能にする。</p>
 *
 * <p>面ごとの反転は、既存の{@link InkFaceCoordinates}のUV定義と描画方向に一致させる。
 * 6面の定義:</p>
 * <pre>
 *   UP/DOWN:    planeU=worldX,  planeV=worldZ
 *   NORTH/SOUTH: planeU=worldX,  planeV=worldY
 *   WEST/EAST:   planeU=worldZ,  planeV=worldY
 * </pre>
 *
 * @param planeU 平面U座標（ワールド単位、連続）
 * @param planeV 平面V座標（ワールド単位、連続）
 */
public record InkPlaneCoordinates(double planeU, double planeV) {

    /**
     * ワールドヒット座標から、指定面の平面座標を計算する。
     *
     * @param face     クリック面の方向
     * @param worldHit ワールド座標でのヒット位置
     * @return 平面座標
     */
    public static InkPlaneCoordinates fromWorldHit(Direction face, net.minecraft.world.phys.Vec3 worldHit) {
        return switch (face) {
            case UP, DOWN -> new InkPlaneCoordinates(worldHit.x, worldHit.z);
            case NORTH, SOUTH -> new InkPlaneCoordinates(worldHit.x, worldHit.y);
            case WEST, EAST -> new InkPlaneCoordinates(worldHit.z, worldHit.y);
        };
    }

    /**
     * この平面座標を、指定ブロック位置における面ローカルUVへ変換する。
     *
     * <p>変換は{@link InkFaceCoordinates#fromHit(Direction, double, double, double)}の
     * 逆変換で、面ごとの反転を考慮する。
     * ローカルUVは0.0〜1.0の範囲外も許容する（隣接ブロックの塗装に必要）。</p>
     *
     * @param face     面方向（クリック面と同じ）
     * @param blockPos 候補ブロック座標
     * @return 面ローカルUV（clampなし）
     */
    public LocalUV toLocalUV(Direction face, BlockPos blockPos) {
        double localU, localV;

        switch (face) {
            case UP -> {
                // u = localX, v = localZ  →  localX = planeU - blockX, localZ = planeV - blockZ
                localU = planeU - blockPos.getX();
                localV = planeV - blockPos.getZ();
            }
            case DOWN -> {
                // u = localX, v = 1.0 - localZ  →  localX = planeU - blockX, localZ = 1.0 - v
                localU = planeU - blockPos.getX();
                localV = 1.0 - (planeV - blockPos.getZ());
            }
            case NORTH -> {
                // u = 1.0 - localX, v = 1.0 - localY  →  localX = 1.0 - u, localY = 1.0 - v
                localU = 1.0 - (planeU - blockPos.getX());
                localV = 1.0 - (planeV - blockPos.getY());
            }
            case SOUTH -> {
                // u = localX, v = 1.0 - localY  →  localX = u, localY = 1.0 - v
                localU = planeU - blockPos.getX();
                localV = 1.0 - (planeV - blockPos.getY());
            }
            case WEST -> {
                // u = localZ, v = 1.0 - localY  →  localZ = u, localY = 1.0 - v
                localU = planeU - blockPos.getZ();
                localV = 1.0 - (planeV - blockPos.getY());
            }
            case EAST -> {
                // u = 1.0 - localZ, v = 1.0 - localY  →  localZ = 1.0 - u, localY = 1.0 - v
                localU = 1.0 - (planeU - blockPos.getZ());
                localV = 1.0 - (planeV - blockPos.getY());
            }
            default -> throw new IllegalArgumentException("Unknown face: " + face);
        }

        return new LocalUV(localU, localV);
    }

    /**
     * 平面座標の円AABBから、候補となるブロック座標範囲を計算する。
     *
     * @param radius 半径（ブロック単位）
     * @return {minBlockU, maxBlockU, minBlockV, maxBlockV}（各値はブロック座標、floor済み）
     */
    public int[] getCandidateBlockRange(double radius) {
        int minU = Mth.floor(planeU - radius);
        int maxU = Mth.floor(planeU + radius);
        int minV = Mth.floor(planeV - radius);
        int maxV = Mth.floor(planeV + radius);
        return new int[]{minU, maxU, minV, maxV};
    }

    /**
     * 候補ブロック範囲座標から BlockPos を生成する。
     *
     * @param face       面方向
     * @param blockU     ブロックU座標（平面上の座標）
     * @param blockV     ブロックV座標（平面上の座標）
     * @param fixedCoord 固定軸の座標（面の平面を維持）
     * @return BlockPos
     */
    public static BlockPos blockPosFromRange(Direction face, int blockU, int blockV, int fixedCoord) {
        return switch (face) {
            case UP, DOWN -> new BlockPos(blockU, fixedCoord, blockV);
            case NORTH, SOUTH -> new BlockPos(blockU, blockV, fixedCoord);
            case WEST, EAST -> new BlockPos(fixedCoord, blockV, blockU);
        };
    }

    /**
     * クリック面の同一平面を維持するための固定座標を取得する。
     *
     * @param face     面方向
     * @param clickPos クリックしたブロックの座標
     * @return 固定座標値
     */
    public static int getFixedCoord(Direction face, BlockPos clickPos) {
        return switch (face) {
            case UP, DOWN -> clickPos.getY();
            case NORTH, SOUTH -> clickPos.getZ();
            case WEST, EAST -> clickPos.getX();
        };
    }

    /**
     * 面ローカルUV座標ペア。
     */
    public record LocalUV(double u, double v) {}
}
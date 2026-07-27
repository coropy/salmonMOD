package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

/**
 * ブロック面のローカル2D座標系を定義する。
 *
 * <p>6面すべてで {@link InkFaceCoordinates#fromHit(Direction, double, double, double)} と
 * 整合する一貫したUV方向を提供する。</p>
 *
 * <p>各面の基底:
 * <pre>
 *   face   | normal   | uAxis    | vAxis
 *   -------|----------|----------|----------
 *   UP     | ( 0, 1, 0) | ( 1, 0, 0) | ( 0, 0, 1)
 *   DOWN   | ( 0,-1, 0) | ( 1, 0, 0) | ( 0, 0,-1)
 *   NORTH  | ( 0, 0,-1) | (-1, 0, 0) | ( 0, 1, 0)
 *   SOUTH  | ( 0, 0, 1) | ( 1, 0, 0) | ( 0, 1, 0)
 *   WEST   | (-1, 0, 0) | ( 0, 0, 1) | ( 0, 1, 0)
 *   EAST   | ( 1, 0, 0) | ( 0, 0,-1) | ( 0, 1, 0)
 * </pre>
 *
 * @param face   面方向
 * @param uAxis  U軸のワールド方向（面上の右方向、長さ1）
 * @param vAxis  V軸のワールド方向（面上の上方向、長さ1）
 * @param normal 面法線のワールド方向（長さ1）
 */
public record FaceBasis(Direction face, Vec3i uAxis, Vec3i vAxis, Vec3i normal) {

    private static final FaceBasis[] BASES = new FaceBasis[6];

    static {
        BASES[Direction.UP.ordinal()] = new FaceBasis(Direction.UP,
                new Vec3i(1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 1, 0));
        BASES[Direction.DOWN.ordinal()] = new FaceBasis(Direction.DOWN,
                new Vec3i(1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, -1, 0));
        BASES[Direction.NORTH.ordinal()] = new FaceBasis(Direction.NORTH,
                new Vec3i(-1, 0, 0), new Vec3i(0, 1, 0), new Vec3i(0, 0, -1));
        BASES[Direction.SOUTH.ordinal()] = new FaceBasis(Direction.SOUTH,
                new Vec3i(1, 0, 0), new Vec3i(0, 1, 0), new Vec3i(0, 0, 1));
        BASES[Direction.WEST.ordinal()] = new FaceBasis(Direction.WEST,
                new Vec3i(0, 0, 1), new Vec3i(0, 1, 0), new Vec3i(-1, 0, 0));
        BASES[Direction.EAST.ordinal()] = new FaceBasis(Direction.EAST,
                new Vec3i(0, 0, -1), new Vec3i(0, 1, 0), new Vec3i(1, 0, 0));
    }

    /**
     * 指定された面方向の FaceBasis を返す。
     */
    public static FaceBasis of(Direction face) {
        return BASES[face.ordinal()];
    }

    /**
     * 面ローカルUV座標から、ブロック面上の3Dワールド座標を計算する。
     *
     * <p>変換は {@link InkFaceCoordinates#fromHit(Direction, double, double, double)} の
     * 逆変換であり、6面すべてで一貫している。</p>
     *
     * @param blockPos ブロック座標
     * @param u        面ローカルU（0.0〜1.0が面範囲、範囲外も許容）
     * @param v        面ローカルV（0.0〜1.0が面範囲、範囲外も許容）
     * @return 面上の3Dワールド座標
     */
    public Vec3 toWorldPoint(BlockPos blockPos, double u, double v) {
        double lx, ly, lz;
        switch (face) {
            case UP:
                lx = u;
                ly = 1.0;
                lz = v;
                break;
            case DOWN:
                lx = u;
                ly = 0.0;
                lz = 1.0 - v;
                break;
            case NORTH:
                lx = 1.0 - u;
                ly = 1.0 - v;
                lz = 0.0;
                break;
            case SOUTH:
                lx = u;
                ly = 1.0 - v;
                lz = 1.0;
                break;
            case WEST:
                lx = 0.0;
                ly = 1.0 - v;
                lz = u;
                break;
            case EAST:
                lx = 1.0;
                ly = 1.0 - v;
                lz = 1.0 - u;
                break;
            default:
                throw new IllegalArgumentException("Unknown face: " + face);
        }
        return new Vec3(blockPos.getX() + lx, blockPos.getY() + ly, blockPos.getZ() + lz);
    }

    /**
     * 3Dワールド座標を、指定ブロックのこの面上のローカルUVに変換する。
     *
     * <p>変換は {@link InkFaceCoordinates#fromHit(Direction, double, double, double)} と
     * 同一で、点が面上にない場合でも計算する（法線方向座標は面位置にクランプしない）。</p>
     *
     * @param worldPoint ワールド座標
     * @param blockPos   ブロック座標
     * @return 面ローカルUV（範囲外もあり得る）
     */
    public LocalUV fromWorld(Vec3 worldPoint, BlockPos blockPos) {
        double localX = worldPoint.x - blockPos.getX();
        double localY = worldPoint.y - blockPos.getY();
        double localZ = worldPoint.z - blockPos.getZ();
        double u, v;
        switch (face) {
            case UP:
                u = localX;
                v = localZ;
                break;
            case DOWN:
                u = localX;
                v = 1.0 - localZ;
                break;
            case NORTH:
                u = 1.0 - localX;
                v = 1.0 - localY;
                break;
            case SOUTH:
                u = localX;
                v = 1.0 - localY;
                break;
            case WEST:
                u = localZ;
                v = 1.0 - localY;
                break;
            case EAST:
                u = 1.0 - localZ;
                v = 1.0 - localY;
                break;
            default:
                throw new IllegalArgumentException("Unknown face: " + face);
        }
        return new LocalUV(u, v);
    }

    /**
     * 3Dワールド座標を面の平面に投影し、面上のローカルUVに変換する。
     *
     * <p>法線方向の座標を面表面位置にクランプする。
     * これにより、面から離れた点でも面上の対応位置を計算できる。</p>
     *
     * @param worldPoint ワールド座標
     * @param blockPos   ブロック座標
     * @return 面表面に投影されたローカルUV
     */
    public LocalUV projectOntoFace(Vec3 worldPoint, BlockPos blockPos) {
        double lx = worldPoint.x - blockPos.getX();
        double ly = worldPoint.y - blockPos.getY();
        double lz = worldPoint.z - blockPos.getZ();

        // 法線方向座標を面表面にクランプ
        switch (face) {
            case UP -> ly = 1.0;
            case DOWN -> ly = 0.0;
            case NORTH -> lz = 0.0;
            case SOUTH -> lz = 1.0;
            case WEST -> lx = 0.0;
            case EAST -> lx = 1.0;
        }

        double u, v;
        switch (face) {
            case UP:
                u = lx;
                v = lz;
                break;
            case DOWN:
                u = lx;
                v = 1.0 - lz;
                break;
            case NORTH:
                u = 1.0 - lx;
                v = 1.0 - ly;
                break;
            case SOUTH:
                u = lx;
                v = 1.0 - ly;
                break;
            case WEST:
                u = lz;
                v = 1.0 - ly;
                break;
            case EAST:
                u = 1.0 - lz;
                v = 1.0 - ly;
                break;
            default:
                throw new IllegalArgumentException("Unknown face: " + face);
        }
        return new LocalUV(u, v);
    }

    /**
     * 指定ブロックのこの面の、ワールド空間での法線方向位置（表面座標）を返す。
     *
     * @param blockPos ブロック座標
     * @return 面のワールド座標系での位置（例: UP面なら blockPos.y + 1.0）
     */
    public double getSurfaceWorldCoord(BlockPos blockPos) {
        return switch (face) {
            case UP -> blockPos.getY() + 1.0;
            case DOWN -> blockPos.getY();
            case NORTH -> blockPos.getZ();
            case SOUTH -> blockPos.getZ() + 1.0;
            case WEST -> blockPos.getX();
            case EAST -> blockPos.getX() + 1.0;
        };
    }

    /**
     * 3D球がこの面の矩形領域と交差するかどうかを判定する。
     *
     * <p>面矩形のワールド座標AABBを計算し、球中心から矩形への最近点距離が
     * 半径以下なら交差と判定する。</p>
     *
     * @param sphereCenter 球の中心（ワールド座標）
     * @param radius       球の半径（ブロック単位）
     * @param blockPos     面が属するブロック座標
     * @return 交差する場合 true
     */
    public boolean sphereIntersectsFace(Vec3 sphereCenter, double radius, BlockPos blockPos) {
        double faceMinX, faceMaxX, faceMinY, faceMaxY, faceMinZ, faceMaxZ;
        double surfaceCoord = getSurfaceWorldCoord(blockPos);

        switch (face) {
            case UP, DOWN -> {
                faceMinX = blockPos.getX();
                faceMaxX = blockPos.getX() + 1.0;
                faceMinY = surfaceCoord;
                faceMaxY = surfaceCoord;
                faceMinZ = blockPos.getZ();
                faceMaxZ = blockPos.getZ() + 1.0;
            }
            case NORTH, SOUTH -> {
                faceMinX = blockPos.getX();
                faceMaxX = blockPos.getX() + 1.0;
                faceMinY = blockPos.getY();
                faceMaxY = blockPos.getY() + 1.0;
                faceMinZ = surfaceCoord;
                faceMaxZ = surfaceCoord;
            }
            case WEST, EAST -> {
                faceMinX = surfaceCoord;
                faceMaxX = surfaceCoord;
                faceMinY = blockPos.getY();
                faceMaxY = blockPos.getY() + 1.0;
                faceMinZ = blockPos.getZ();
                faceMaxZ = blockPos.getZ() + 1.0;
            }
            default -> {
                return false;
            }
        }

        double nearestX = clamp(sphereCenter.x, faceMinX, faceMaxX);
        double nearestY = clamp(sphereCenter.y, faceMinY, faceMaxY);
        double nearestZ = clamp(sphereCenter.z, faceMinZ, faceMaxZ);

        double dx = sphereCenter.x - nearestX;
        double dy = sphereCenter.y - nearestY;
        double dz = sphereCenter.z - nearestZ;

        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /**
     * 球の中心からこの面の平面までの符号付き距離を計算する。
     *
     * <p>正の値は面の表側（法線方向）にあることを示す。
     * 塗装の場合、球中心は面の表側にある（ヒット面の法線方向）ことが期待される。</p>
     *
     * @param sphereCenter 球の中心（ワールド座標）
     * @param blockPos     ブロック座標
     * @return 面平面までの距離（符号付き、ブロック単位）
     */
    public double signedDistanceToPlane(Vec3 sphereCenter, BlockPos blockPos) {
        double surfaceCoord = getSurfaceWorldCoord(blockPos);
        return switch (face) {
            case UP -> sphereCenter.y - surfaceCoord;
            case DOWN -> surfaceCoord - sphereCenter.y;
            case NORTH -> surfaceCoord - sphereCenter.z;
            case SOUTH -> sphereCenter.z - surfaceCoord;
            case WEST -> surfaceCoord - sphereCenter.x;
            case EAST -> sphereCenter.x - surfaceCoord;
        };
    }

    /**
     * 球の中心からこの面の平面までの絶対距離を計算する。
     *
     * @param sphereCenter 球の中心（ワールド座標）
     * @param blockPos     ブロック座標
     * @return 面平面までの絶対距離（ブロック単位）
     */
    public double distanceToPlane(Vec3 sphereCenter, BlockPos blockPos) {
        return Math.abs(signedDistanceToPlane(sphereCenter, blockPos));
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    /**
     * 面ローカルUV座標ペア。
     */
    public record LocalUV(double u, double v) {
        /** このUVが面範囲 [0, 1]×[0, 1] 内かを返す。 */
        public boolean isInBounds() {
            return u >= 0.0 && u <= 1.0 && v >= 0.0 && v <= 1.0;
        }
    }
}
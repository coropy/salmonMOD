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

    // ========================================================================
    // 座標変換
    // ========================================================================

    /**
     * 面ローカルUV座標から、ブロック面上の3Dワールド座標を計算する。
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
                lx = u; ly = 1.0; lz = v; break;
            case DOWN:
                lx = u; ly = 0.0; lz = 1.0 - v; break;
            case NORTH:
                lx = 1.0 - u; ly = 1.0 - v; lz = 0.0; break;
            case SOUTH:
                lx = u; ly = 1.0 - v; lz = 1.0; break;
            case WEST:
                lx = 0.0; ly = 1.0 - v; lz = u; break;
            case EAST:
                lx = 1.0; ly = 1.0 - v; lz = 1.0 - u; break;
            default:
                throw new IllegalArgumentException("Unknown face: " + face);
        }
        return new Vec3(blockPos.getX() + lx, blockPos.getY() + ly, blockPos.getZ() + lz);
    }

    /**
     * 面ローカルUV座標と法線方向位置から、ワールド座標を計算する（Patch用）。
     *
     * @param blockPos      ブロック座標
     * @param u             面ローカルU（0.0〜1.0）
     * @param v             面ローカルV（0.0〜1.0）
     * @param normalCoord   法線方向のブロック内位置（0.0〜1.0）
     * @return double[3] = {worldX, worldY, worldZ}
     */
    public double[] toWorldPointRaw(BlockPos blockPos, double u, double v, double normalCoord) {
        double lx, ly, lz;
        switch (face) {
            case UP:
                lx = u; ly = normalCoord; lz = v; break;
            case DOWN:
                lx = u; ly = normalCoord; lz = 1.0 - v; break;
            case NORTH:
                lx = 1.0 - u; ly = 1.0 - v; lz = normalCoord; break;
            case SOUTH:
                lx = u; ly = 1.0 - v; lz = normalCoord; break;
            case WEST:
                lx = normalCoord; ly = 1.0 - v; lz = u; break;
            case EAST:
                lx = normalCoord; ly = 1.0 - v; lz = 1.0 - u; break;
            default:
                throw new IllegalArgumentException("Unknown face: " + face);
        }
        return new double[] { blockPos.getX() + lx, blockPos.getY() + ly, blockPos.getZ() + lz };
    }

    /**
     * 3Dワールド座標を、指定ブロックのこの面上のローカルUVに変換する。
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
            case UP:    u = localX; v = localZ; break;
            case DOWN:  u = localX; v = 1.0 - localZ; break;
            case NORTH: u = 1.0 - localX; v = 1.0 - localY; break;
            case SOUTH: u = localX; v = 1.0 - localY; break;
            case WEST:  u = localZ; v = 1.0 - localY; break;
            case EAST:  u = 1.0 - localZ; v = 1.0 - localY; break;
            default: throw new IllegalArgumentException("Unknown face: " + face);
        }
        return new LocalUV(u, v);
    }

    /**
     * 3Dワールド座標を面の平面に投影し、面上のローカルUVに変換する。
     *
     * <p>法線方向の座標を面表面位置にクランプする。</p>
     *
     * @param worldPoint ワールド座標
     * @param blockPos   ブロック座標
     * @return 面表面に投影されたローカルUV
     */
    public LocalUV projectOntoFace(Vec3 worldPoint, BlockPos blockPos) {
        double lx = worldPoint.x - blockPos.getX();
        double ly = worldPoint.y - blockPos.getY();
        double lz = worldPoint.z - blockPos.getZ();

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
            case UP:    u = lx; v = lz; break;
            case DOWN:  u = lx; v = 1.0 - lz; break;
            case NORTH: u = 1.0 - lx; v = 1.0 - ly; break;
            case SOUTH: u = lx; v = 1.0 - ly; break;
            case WEST:  u = lz; v = 1.0 - ly; break;
            case EAST:  u = 1.0 - lz; v = 1.0 - ly; break;
            default: throw new IllegalArgumentException("Unknown face: " + face);
        }
        return new LocalUV(u, v);
    }

    /**
     * 3Dワールド座標を指定された法線方向位置の面に投影し、ローカルUVに変換する（Patch用）。
     *
     * @param worldPoint  ワールド座標
     * @param blockPos    ブロック座標
     * @param normalCoord 法線方向のブロック内位置（0.0〜1.0）
     * @return 投影されたローカルUV
     */
    public LocalUV projectOntoFaceAtCoord(Vec3 worldPoint, BlockPos blockPos, double normalCoord) {
        double lx = worldPoint.x - blockPos.getX();
        double ly = worldPoint.y - blockPos.getY();
        double lz = worldPoint.z - blockPos.getZ();

        switch (face) {
            case UP, DOWN -> ly = normalCoord;
            case NORTH, SOUTH -> lz = normalCoord;
            case WEST, EAST -> lx = normalCoord;
        }

        double u, v;
        switch (face) {
            case UP:    u = lx; v = lz; break;
            case DOWN:  u = lx; v = 1.0 - lz; break;
            case NORTH: u = 1.0 - lx; v = 1.0 - ly; break;
            case SOUTH: u = lx; v = 1.0 - ly; break;
            case WEST:  u = lz; v = 1.0 - ly; break;
            case EAST:  u = 1.0 - lz; v = 1.0 - ly; break;
            default: throw new IllegalArgumentException("Unknown face: " + face);
        }
        return new LocalUV(u, v);
    }

    /**
     * 指定ブロックのこの面の、ワールド空間での法線方向位置（表面座標）を返す。
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
     * 法線方向軸を返す（Direction.Axis）。
     */
    public Direction.Axis getPlaneAxis() {
        return face.getAxis();
    }

    /**
     * UV矩形と法線方向位置からワールド座標AABBを計算する（Patch用）。
     *
     * @param blockPos     ブロック座標
     * @param minU         ブロック面U最小値（0.0〜1.0）
     * @param minV         ブロック面V最小値（0.0〜1.0）
     * @param maxU         ブロック面U最大値（0.0〜1.0）
     * @param maxV         ブロック面V最大値（0.0〜1.0）
     * @param planeCoord   法線方向位置（0.0〜1.0）
     * @param normalOffset 法線方向の表面オフセット
     * @param thickness    厚み
     * @return double[6] = {minX, minY, minZ, maxX, maxY, maxZ}
     */
    public double[] getBoundsFromUV(BlockPos blockPos, double minU, double minV,
                                     double maxU, double maxV, double planeCoord,
                                     double normalOffset, double thickness) {
        double bx = blockPos.getX();
        double by = blockPos.getY();
        double bz = blockPos.getZ();
        double minZFace, maxZFace;

        switch (face) {
            case UP -> {
                minZFace = planeCoord + normalOffset;
                maxZFace = planeCoord + normalOffset + thickness;
                return new double[] { bx + minU, by + minZFace, bz + minV,
                                      bx + maxU, by + maxZFace, bz + maxV };
            }
            case DOWN -> {
                minZFace = planeCoord + normalOffset;
                maxZFace = planeCoord + normalOffset + thickness;
                // DOWN: v = 1.0 - localZ → localZ = 1.0 - v
                return new double[] { bx + minU, by + minZFace, bz + 1.0 - maxV,
                                      bx + maxU, by + maxZFace, bz + 1.0 - minV };
            }
            case NORTH -> {
                minZFace = planeCoord + normalOffset;
                maxZFace = planeCoord + normalOffset + thickness;
                // NORTH: u = 1.0 - localX, v = 1.0 - localY
                return new double[] { bx + 1.0 - maxU, by + 1.0 - maxV, bz + minZFace,
                                      bx + 1.0 - minU, by + 1.0 - minV, bz + maxZFace };
            }
            case SOUTH -> {
                minZFace = planeCoord + normalOffset;
                maxZFace = planeCoord + normalOffset + thickness;
                // SOUTH: u = localX, v = 1.0 - localY
                return new double[] { bx + minU, by + 1.0 - maxV, bz + minZFace,
                                      bx + maxU, by + 1.0 - minV, bz + maxZFace };
            }
            case WEST -> {
                minZFace = planeCoord + normalOffset;
                maxZFace = planeCoord + normalOffset + thickness;
                // WEST: u = localZ, v = 1.0 - localY
                return new double[] { bx + minZFace, by + 1.0 - maxV, bz + minU,
                                      bx + maxZFace, by + 1.0 - minV, bz + maxU };
            }
            case EAST -> {
                minZFace = planeCoord + normalOffset;
                maxZFace = planeCoord + normalOffset + thickness;
                // EAST: u = 1.0 - localZ, v = 1.0 - localY
                return new double[] { bx + minZFace, by + 1.0 - maxV, bz + 1.0 - maxU,
                                      bx + maxZFace, by + 1.0 - minV, bz + 1.0 - minU };
            }
            default -> throw new IllegalArgumentException("Unknown face: " + face);
        }
    }

    // ========================================================================
    // 球面交差判定
    // ========================================================================

    /**
     * 3D球がこの面の矩形領域と交差するかどうかを判定する（フル面用）。
     */
    public boolean sphereIntersectsFace(Vec3 sphereCenter, double radius, BlockPos blockPos) {
        double faceMinX, faceMaxX, faceMinY, faceMaxY, faceMinZ, faceMaxZ;
        double surfaceCoord = getSurfaceWorldCoord(blockPos);

        switch (face) {
            case UP, DOWN -> {
                faceMinX = blockPos.getX(); faceMaxX = blockPos.getX() + 1.0;
                faceMinY = surfaceCoord; faceMaxY = surfaceCoord;
                faceMinZ = blockPos.getZ(); faceMaxZ = blockPos.getZ() + 1.0;
            }
            case NORTH, SOUTH -> {
                faceMinX = blockPos.getX(); faceMaxX = blockPos.getX() + 1.0;
                faceMinY = blockPos.getY(); faceMaxY = blockPos.getY() + 1.0;
                faceMinZ = surfaceCoord; faceMaxZ = surfaceCoord;
            }
            case WEST, EAST -> {
                faceMinX = surfaceCoord; faceMaxX = surfaceCoord;
                faceMinY = blockPos.getY(); faceMaxY = blockPos.getY() + 1.0;
                faceMinZ = blockPos.getZ(); faceMaxZ = blockPos.getZ() + 1.0;
            }
            default -> { return false; }
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
     * 3D球が指定されたパッチ矩形と交差するかどうかを判定する。
     *
     * @param sphereCenter 球の中心（ワールド座標）
     * @param radius       球の半径（ブロック単位）
     * @param blockPos     ブロック座標
     * @param planeCoord   法線方向位置（0.0〜1.0）
     * @param minU         パッチU最小値（0.0〜1.0）
     * @param minV         パッチV最小値（0.0〜1.0）
     * @param maxU         パッチU最大値（0.0〜1.0）
     * @param maxV         パッチV最大値（0.0〜1.0）
     * @return 交差する場合 true
     */
    public boolean sphereIntersectsPatchRect(Vec3 sphereCenter, double radius,
                                              BlockPos blockPos, double planeCoord,
                                              double minU, double minV,
                                              double maxU, double maxV) {
        double[] bounds = getBoundsFromUV(blockPos, minU, minV, maxU, maxV,
                planeCoord, 0.0, 0.0);
        double faceMinX = bounds[0], faceMinY = bounds[1], faceMinZ = bounds[2];
        double faceMaxX = bounds[3], faceMaxY = bounds[4], faceMaxZ = bounds[5];

        double nearestX = clamp(sphereCenter.x, faceMinX, faceMaxX);
        double nearestY = clamp(sphereCenter.y, faceMinY, faceMaxY);
        double nearestZ = clamp(sphereCenter.z, faceMinZ, faceMaxZ);
        double dx = sphereCenter.x - nearestX;
        double dy = sphereCenter.y - nearestY;
        double dz = sphereCenter.z - nearestZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    // ========================================================================
    // 距離計算
    // ========================================================================

    /**
     * 球中心から面平面までの符号付き距離を計算する。
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
     * 球中心から面平面までの絶対距離を計算する。
     */
    public double distanceToPlane(Vec3 sphereCenter, BlockPos blockPos) {
        return Math.abs(signedDistanceToPlane(sphereCenter, blockPos));
    }

    /**
     * 球中心から指定された planeCoord の面平面までの絶対距離を計算する（Patch用）。
     */
    public double distanceToPatchPlane(Vec3 sphereCenter, BlockPos blockPos, double planeCoord) {
        double actualCoord = switch (face) {
            case UP, DOWN -> sphereCenter.y;
            case NORTH, SOUTH -> sphereCenter.z;
            case WEST, EAST -> sphereCenter.x;
        };
        double surfaceCoord = blockPos.get(getPlaneAxis()) + planeCoord;
        return Math.abs(actualCoord - surfaceCoord);
    }

    /**
     * ブロック内の法線方向位置をワールド座標に変換する。
     */
    public double planeToWorld(BlockPos blockPos, double planeCoord) {
        return blockPos.get(getPlaneAxis()) + planeCoord;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    // ========================================================================
    // LocalUV レコード
    // ========================================================================

    /**
     * 面ローカルUV座標ペア。
     */
    public record LocalUV(double u, double v) {
        /** このUVが面範囲 [0, 1]×[0, 1] 内かを返す。 */
        public boolean isInBounds() {
            return u >= 0.0 && u <= 1.0 && v >= 0.0 && v <= 1.0;
        }

        /** u 方向の距離 */
        public double distanceU(LocalUV other) {
            return this.u - other.u;
        }

        /** v 方向の距離 */
        public double distanceV(LocalUV other) {
            return this.v - other.v;
        }
    }
}
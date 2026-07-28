package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * ブロック内部の矩形表面パッチ。
 *
 * <p>ブロック内部を16分割したユニットで表現する。
 * フルキューブは各面に1つのパッチ（0..16全体）、
 * 階段やハーフブロックは複数の小さなパッチを持つ。</p>
 *
 * @param id    パッチを一意に識別するID
 * @param pos   所属するブロック座標
 */
public record InkSurfacePatch(
        InkSurfacePatchId id,
        BlockPos pos
) {
    /** フルキューブ面に対応するパッチを作成 */
    public static InkSurfacePatch fullFace(BlockPos pos, Direction face) {
        return new InkSurfacePatch(InkSurfacePatchId.fullFace(face), pos);
    }

    /**
     * InkSurfaceKey を生成する。
     */
    public InkSurfaceKey toSurfaceKey() {
        return new InkSurfaceKey(pos, id);
    }

    // ========================================================================
    // ワールド座標変換（FaceBasis と整合する）
    // ========================================================================

    /**
     * パッチ内のローカルUV座標（0.0〜1.0）からワールド座標を計算する。
     */
    public Vec3 toWorldPoint(double patchLocalU, double patchLocalV) {
        double blockU = id.toBlockU(patchLocalU);
        double blockV = id.toBlockV(patchLocalV);
        FaceBasis basis = FaceBasis.of(id.normal());

        // plane をワールド座標系の法線方向オフセットに変換
        double[] worldCoords = basis.toWorldPointRaw(pos, blockU, blockV,
                id.plane() / (double) InkSurfacePatchId.BLOCK_RESOLUTION);
        return new Vec3(worldCoords[0], worldCoords[1], worldCoords[2]);
    }

    /**
     * ワールド座標をパッチローカルUVに変換する。
     */
    public FaceBasis.LocalUV fromWorld(Vec3 worldPoint) {
        FaceBasis basis = FaceBasis.of(id.normal());
        FaceBasis.LocalUV blockUV = basis.fromWorld(worldPoint, pos);
        double pu = id.toPatchU(blockUV.u());
        double pv = id.toPatchV(blockUV.v());
        return new FaceBasis.LocalUV(pu, pv);
    }

    /**
     * ワールド座標をパッチ面に投影し、パッチローカルUVを返す。
     *
     * <p>部分パッチ（plane≠0/16）ではパッチ自身の平面位置を使い、
     * ブロック外周面（1.0/0.0）にクランプしない。</p>
     */
    public FaceBasis.LocalUV projectOntoPatch(Vec3 worldPoint) {
        FaceBasis basis = FaceBasis.of(id.normal());
        double planeCoord = id.plane() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
        FaceBasis.LocalUV blockUV = basis.projectOntoFaceAtCoord(worldPoint, pos, planeCoord);
        double pu = id.toPatchU(blockUV.u());
        double pv = id.toPatchV(blockUV.v());
        return new FaceBasis.LocalUV(pu, pv);
    }

    /**
     * このパッチのワールド座標系での3D AABB（法線オフセット込み）。
     * @param normalOffset 法線方向のオフセット（例: SURFACE_OFFSET）
     * @param thickness    法線方向の厚み
     * @return double[6] = {minX, minY, minZ, maxX, maxY, maxZ}
     */
    public double[] getWorldBounds(double normalOffset, double thickness) {
        FaceBasis basis = FaceBasis.of(id.normal());
        double blockMinU = id.minU() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
        double blockMaxU = id.maxU() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
        double blockMinV = id.minV() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
        double blockMaxV = id.maxV() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
        double planeCoord = id.plane() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;

        return basis.getBoundsFromUV(pos, blockMinU, blockMinV, blockMaxU, blockMaxV,
                planeCoord, normalOffset, thickness);
    }

    /**
     * ワールド座標の指定点がこのパッチの矩形範囲内にあるか（法線方向は加味しない）。
     */
    public boolean containsWorldPointUV(Vec3 worldPoint) {
        FaceBasis basis = FaceBasis.of(id.normal());
        FaceBasis.LocalUV blockUV = basis.fromWorld(worldPoint, pos);
        return id.containsUV(blockUV.u(), blockUV.v());
    }

    /**
     * 法線方向も含めて、ワールド座標の指定点がこのパッチ上にあるか。
     * epsilon はブロック単位で法線方向の許容誤差。
     */
    public boolean containsWorldPoint(Vec3 worldPoint, double epsilon) {
        if (!containsWorldPointUV(worldPoint)) return false;
        FaceBasis basis = FaceBasis.of(id.normal());
        double surfaceWorldCoord = basis.getSurfaceWorldCoord(pos);
        double planeWorldCoord = pos.get(basis.getPlaneAxis())
                + id.plane() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
        // 法線方向の差分をチェック
        double actualCoord = switch (id.normal()) {
            case UP, DOWN -> worldPoint.y;
            case NORTH, SOUTH -> worldPoint.z;
            case WEST, EAST -> worldPoint.x;
        };
        // planeWorldCoord はブロック内位置だが、実際は blockPos + plane/16
        // 実際の surfaceCoord は blockPosの0平面 + plane/16
        double expectedCoord = pos.get(basis.getPlaneAxis())
                + id.plane() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
        return Math.abs(actualCoord - expectedCoord) <= epsilon;
    }
}
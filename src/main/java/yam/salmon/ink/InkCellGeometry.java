package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * ブロック面の8×8グリッドセルのワールド座標AABBを計算するユーティリティ。
 *
 * <p>サーバー側の {@link InkFaceCoordinates} と同じUV定義を使用し、
 * クライアント側描画との一貫性を保証する。</p>
 *
 * <p>Phase 9: Quadベースの表面描画に移行。getCellQuadForPatch で
 * パッチ平面上の1枚Quadを生成する。旧Box描画用メソッドはDeprecated。</p>
 */
public final class InkCellGeometry {

    /** セルの面サイズ（1/8ブロック） */
    public static final double CELL_SIZE = 1.0 / InkFaceData.GRID_SIZE;

    /** インクの厚さ（ブロック単位） - Quad描画では使用しない */
    public static final double INK_THICKNESS = 0.008;

    /** ブロック表面からのオフセット（Z-fighting回避） */
    public static final double SURFACE_OFFSET = 0.002;

    /** パッチ描画用の法線方向オフセット */
    public static final double PATCH_NORMAL_OFFSET = 0.002;

    /** 矩形間の微小隙間対策用イプシロン（UV単位） */
    public static final double PATCH_EDGE_EPSILON = 0.0002;

    private InkCellGeometry() {}

    // ========================================================================
    // 旧フル面互換（Directionベース）- @Deprecated
    // ========================================================================

    /**
     * @deprecated 新規コードでは {@link #getCellQuadForPatch} を使用する。
     */
    @Deprecated
    public static double[] getCellBounds(BlockPos blockPos, Direction face, int cellU, int cellV) {
        double baseX = blockPos.getX();
        double baseY = blockPos.getY();
        double baseZ = blockPos.getZ();

        double uMin = cellU * CELL_SIZE;
        double uMax = (cellU + 1) * CELL_SIZE;
        double vMin = cellV * CELL_SIZE;
        double vMax = (cellV + 1) * CELL_SIZE;

        return switch (face) {
            case UP -> new double[] {
                baseX + uMin, baseY + 1.0 + SURFACE_OFFSET, baseZ + vMin,
                baseX + uMax, baseY + 1.0 + SURFACE_OFFSET + INK_THICKNESS, baseZ + vMax
            };
            case DOWN -> new double[] {
                baseX + uMin, baseY - SURFACE_OFFSET - INK_THICKNESS, baseZ + 1.0 - vMax,
                baseX + uMax, baseY - SURFACE_OFFSET, baseZ + 1.0 - vMin
            };
            case NORTH -> new double[] {
                baseX + 1.0 - uMax, baseY + 1.0 - vMax, baseZ - SURFACE_OFFSET - INK_THICKNESS,
                baseX + 1.0 - uMin, baseY + 1.0 - vMin, baseZ - SURFACE_OFFSET
            };
            case SOUTH -> new double[] {
                baseX + uMin, baseY + 1.0 - vMax, baseZ + 1.0 + SURFACE_OFFSET,
                baseX + uMax, baseY + 1.0 - vMin, baseZ + 1.0 + SURFACE_OFFSET + INK_THICKNESS
            };
            case WEST -> new double[] {
                baseX - SURFACE_OFFSET - INK_THICKNESS, baseY + 1.0 - vMax, baseZ + uMin,
                baseX - SURFACE_OFFSET, baseY + 1.0 - vMin, baseZ + uMax
            };
            case EAST -> new double[] {
                baseX + 1.0 + SURFACE_OFFSET, baseY + 1.0 - vMax, baseZ + 1.0 - uMax,
                baseX + 1.0 + SURFACE_OFFSET + INK_THICKNESS, baseY + 1.0 - vMin, baseZ + 1.0 - uMin
            };
        };
    }

    // ========================================================================
    // パッチ矩形対応 (Deprecated: Boxベース)
    // ========================================================================

    /**
     * @deprecated インク描画には {@link #getCellQuadForPatch} を使用すること。
     *             このメソッドはデバッグ表示用にのみ維持する。
     */
    @Deprecated
    public static double[] getCellBoundsForPatch(
            BlockPos blockPos, InkSurfacePatchId patchId, int cellU, int cellV) {
        float[] verts = getCellVerticesForPatch(blockPos, patchId, cellU, cellV);
        double minX = verts[0], minY = verts[1], minZ = verts[2];
        double maxX = verts[0], maxY = verts[1], maxZ = verts[2];
        for (int i = 0; i < 12; i += 3) {
            if (verts[i] < minX) minX = verts[i];
            if (verts[i + 1] < minY) minY = verts[i + 1];
            if (verts[i + 2] < minZ) minZ = verts[i + 2];
            if (verts[i] > maxX) maxX = verts[i];
            if (verts[i + 1] > maxY) maxY = verts[i + 1];
            if (verts[i + 2] > maxZ) maxZ = verts[i + 2];
        }
        Direction normal = patchId.normal();
        double thickness = INK_THICKNESS;
        minX -= Math.abs(normal.getStepX()) * thickness * 0.5;
        minY -= Math.abs(normal.getStepY()) * thickness * 0.5;
        minZ -= Math.abs(normal.getStepZ()) * thickness * 0.5;
        maxX += Math.abs(normal.getStepX()) * thickness * 0.5;
        maxY += Math.abs(normal.getStepY()) * thickness * 0.5;
        maxZ += Math.abs(normal.getStepZ()) * thickness * 0.5;
        return new double[] { minX, minY, minZ, maxX, maxY, maxZ };
    }

    /** @deprecated 内部使用のみ */
    @Deprecated
    public static float[] getCellVerticesForPatch(
            BlockPos blockPos, InkSurfacePatchId patchId, int cellU, int cellV) {
        int resolution = InkFaceData.GRID_SIZE;
        double blockRes = InkSurfacePatchId.BLOCK_RESOLUTION;
        Direction normal = patchId.normal();

        double u0 = (double) cellU / resolution;
        double u1 = (double) (cellU + 1) / resolution;
        double v0 = (double) cellV / resolution;
        double v1 = (double) (cellV + 1) / resolution;

        double planeCoord = patchId.plane() / blockRes;
        double minU = patchId.minU() / blockRes;
        double maxU = patchId.maxU() / blockRes;
        double minV = patchId.minV() / blockRes;
        double maxV = patchId.maxV() / blockRes;

        double cellU0 = minU + u0 * (maxU - minU);
        double cellU1 = minU + u1 * (maxU - minU);
        double cellV0 = minV + v0 * (maxV - minV);
        double cellV1 = minV + v1 * (maxV - minV);

        double nx = normal.getStepX() * PATCH_NORMAL_OFFSET;
        double ny = normal.getStepY() * PATCH_NORMAL_OFFSET;
        double nz = normal.getStepZ() * PATCH_NORMAL_OFFSET;

        double bx = blockPos.getX();
        double by = blockPos.getY();
        double bz = blockPos.getZ();

        double p00x, p00y, p00z, p10x, p10y, p10z, p11x, p11y, p11z, p01x, p01y, p01z;

        switch (normal) {
            case UP -> {
                p00x = bx + cellU0; p00y = by + planeCoord; p00z = bz + cellV0;
                p10x = bx + cellU1; p10y = by + planeCoord; p10z = bz + cellV0;
                p11x = bx + cellU1; p11y = by + planeCoord; p11z = bz + cellV1;
                p01x = bx + cellU0; p01y = by + planeCoord; p01z = bz + cellV1;
            }
            case DOWN -> {
                p00x = bx + cellU0; p00y = by + planeCoord; p00z = bz + 1.0 - cellV1;
                p10x = bx + cellU1; p10y = by + planeCoord; p10z = bz + 1.0 - cellV1;
                p11x = bx + cellU1; p11y = by + planeCoord; p11z = bz + 1.0 - cellV0;
                p01x = bx + cellU0; p01y = by + planeCoord; p01z = bz + 1.0 - cellV0;
            }
            case NORTH -> {
                p00x = bx + 1.0 - cellU1; p00y = by + 1.0 - cellV1; p00z = bz + planeCoord;
                p10x = bx + 1.0 - cellU0; p10y = by + 1.0 - cellV1; p10z = bz + planeCoord;
                p11x = bx + 1.0 - cellU0; p11y = by + 1.0 - cellV0; p11z = bz + planeCoord;
                p01x = bx + 1.0 - cellU1; p01y = by + 1.0 - cellV0; p01z = bz + planeCoord;
            }
            case SOUTH -> {
                p00x = bx + cellU0; p00y = by + 1.0 - cellV1; p00z = bz + planeCoord;
                p10x = bx + cellU1; p10y = by + 1.0 - cellV1; p10z = bz + planeCoord;
                p11x = bx + cellU1; p11y = by + 1.0 - cellV0; p11z = bz + planeCoord;
                p01x = bx + cellU0; p01y = by + 1.0 - cellV0; p01z = bz + planeCoord;
            }
            case WEST -> {
                p00x = bx + planeCoord; p00y = by + 1.0 - cellV1; p00z = bz + cellU0;
                p10x = bx + planeCoord; p10y = by + 1.0 - cellV1; p10z = bz + cellU1;
                p11x = bx + planeCoord; p11y = by + 1.0 - cellV0; p11z = bz + cellU1;
                p01x = bx + planeCoord; p01y = by + 1.0 - cellV0; p01z = bz + cellU0;
            }
            case EAST -> {
                p00x = bx + planeCoord; p00y = by + 1.0 - cellV1; p00z = bz + 1.0 - cellU1;
                p10x = bx + planeCoord; p10y = by + 1.0 - cellV1; p10z = bz + 1.0 - cellU0;
                p11x = bx + planeCoord; p11y = by + 1.0 - cellV0; p11z = bz + 1.0 - cellU0;
                p01x = bx + planeCoord; p01y = by + 1.0 - cellV0; p01z = bz + 1.0 - cellU1;
            }
            default -> throw new IllegalArgumentException("Unknown face: " + normal);
        }

        p00x += nx; p00y += ny; p00z += nz;
        p10x += nx; p10y += ny; p10z += nz;
        p11x += nx; p11y += ny; p11z += nz;
        p01x += nx; p01y += ny; p01z += nz;

        return new float[] {
            (float) p00x, (float) p00y, (float) p00z,
            (float) p10x, (float) p10y, (float) p10z,
            (float) p11x, (float) p11y, (float) p11z,
            (float) p01x, (float) p01y, (float) p01z
        };
    }

    // ========================================================================
    // Quad表面描画用（Phase 9 新方式）
    // ========================================================================

    /**
     * Surface Patch 内のセル矩形範囲に対応する1枚のQuadをワールド座標で生成。
     *
     * <p>6面Boxの側面・厚みは描画しない。法線方向に normalOffset を加算。</p>
     *
     * @param blockPos     ブロック座標
     * @param patchId      Surface Patch ID
     * @param cellMinU     開始セルU (0..7)
     * @param cellMinV     開始セルV (0..7)
     * @param cellMaxUEx   終了セルU+1 (1..8)
     * @param cellMaxVEx   終了セルV+1 (1..8)
     * @param normalOffset 法線オフセット
     * @return 表面Quad
     */
    public static InkCellQuad getCellQuadForPatch(
            BlockPos blockPos,
            InkSurfacePatchId patchId,
            int cellMinU,
            int cellMinV,
            int cellMaxUEx,
            int cellMaxVEx,
            double normalOffset
    ) {
        int resolution = InkFaceData.GRID_SIZE;
        double blockRes = InkSurfacePatchId.BLOCK_RESOLUTION;
        Direction normal = patchId.normal();

        double u0 = (double) cellMinU / resolution;
        double u1 = (double) cellMaxUEx / resolution;
        double v0 = (double) cellMinV / resolution;
        double v1 = (double) cellMaxVEx / resolution;

        double planeCoord = patchId.plane() / blockRes;
        double minU = patchId.minU() / blockRes;
        double maxU = patchId.maxU() / blockRes;
        double minV = patchId.minV() / blockRes;
        double maxV = patchId.maxV() / blockRes;

        double cellU0 = minU + u0 * (maxU - minU);
        double cellU1 = minU + u1 * (maxU - minU);
        double cellV0 = minV + v0 * (maxV - minV);
        double cellV1 = minV + v1 * (maxV - minV);

        if (cellMinU > 0) cellU0 -= PATCH_EDGE_EPSILON;
        if (cellMinV > 0) cellV0 -= PATCH_EDGE_EPSILON;
        if (cellMaxUEx < resolution) cellU1 += PATCH_EDGE_EPSILON;
        if (cellMaxVEx < resolution) cellV1 += PATCH_EDGE_EPSILON;

        double nx = normal.getStepX() * normalOffset;
        double ny = normal.getStepY() * normalOffset;
        double nz = normal.getStepZ() * normalOffset;

        double bx = blockPos.getX();
        double by = blockPos.getY();
        double bz = blockPos.getZ();

        double p00x, p00y, p00z, p10x, p10y, p10z, p11x, p11y, p11z, p01x, p01y, p01z;

        switch (normal) {
            case UP -> {
                p00x = bx + cellU0; p00y = by + planeCoord; p00z = bz + cellV0;
                p10x = bx + cellU1; p10y = by + planeCoord; p10z = bz + cellV0;
                p11x = bx + cellU1; p11y = by + planeCoord; p11z = bz + cellV1;
                p01x = bx + cellU0; p01y = by + planeCoord; p01z = bz + cellV1;
            }
            case DOWN -> {
                p00x = bx + cellU0; p00y = by + planeCoord; p00z = bz + 1.0 - cellV1;
                p10x = bx + cellU1; p10y = by + planeCoord; p10z = bz + 1.0 - cellV1;
                p11x = bx + cellU1; p11y = by + planeCoord; p11z = bz + 1.0 - cellV0;
                p01x = bx + cellU0; p01y = by + planeCoord; p01z = bz + 1.0 - cellV0;
            }
            case NORTH -> {
                p00x = bx + 1.0 - cellU1; p00y = by + 1.0 - cellV1; p00z = bz + planeCoord;
                p10x = bx + 1.0 - cellU0; p10y = by + 1.0 - cellV1; p10z = bz + planeCoord;
                p11x = bx + 1.0 - cellU0; p11y = by + 1.0 - cellV0; p11z = bz + planeCoord;
                p01x = bx + 1.0 - cellU1; p01y = by + 1.0 - cellV0; p01z = bz + planeCoord;
            }
            case SOUTH -> {
                p00x = bx + cellU0; p00y = by + 1.0 - cellV1; p00z = bz + planeCoord;
                p10x = bx + cellU1; p10y = by + 1.0 - cellV1; p10z = bz + planeCoord;
                p11x = bx + cellU1; p11y = by + 1.0 - cellV0; p11z = bz + planeCoord;
                p01x = bx + cellU0; p01y = by + 1.0 - cellV0; p01z = bz + planeCoord;
            }
            case WEST -> {
                p00x = bx + planeCoord; p00y = by + 1.0 - cellV1; p00z = bz + cellU0;
                p10x = bx + planeCoord; p10y = by + 1.0 - cellV1; p10z = bz + cellU1;
                p11x = bx + planeCoord; p11y = by + 1.0 - cellV0; p11z = bz + cellU1;
                p01x = bx + planeCoord; p01y = by + 1.0 - cellV0; p01z = bz + cellU0;
            }
            case EAST -> {
                p00x = bx + planeCoord; p00y = by + 1.0 - cellV1; p00z = bz + 1.0 - cellU1;
                p10x = bx + planeCoord; p10y = by + 1.0 - cellV1; p10z = bz + 1.0 - cellU0;
                p11x = bx + planeCoord; p11y = by + 1.0 - cellV0; p11z = bz + 1.0 - cellU0;
                p01x = bx + planeCoord; p01y = by + 1.0 - cellV0; p01z = bz + 1.0 - cellU1;
            }
            default -> throw new IllegalArgumentException("Unknown face: " + normal);
        }

        p00x += nx; p00y += ny; p00z += nz;
        p10x += nx; p10y += ny; p10z += nz;
        p11x += nx; p11y += ny; p11z += nz;
        p01x += nx; p01y += ny; p01z += nz;

        return new InkCellQuad(
                (float) p00x, (float) p00y, (float) p00z,
                (float) p10x, (float) p10y, (float) p10z,
                (float) p11x, (float) p11y, (float) p11z,
                (float) p01x, (float) p01y, (float) p01z,
                normal
        );
    }
}
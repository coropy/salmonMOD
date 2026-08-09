package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;

import java.util.*;

/**
 * インク塗装を3D球ベースで複数ブロック・複数方向面へ分配する。
 *
 * <p>Phase 7: Surface Patch ベースの分配。VoxelShape から抽出した
 * パッチ単位で走査し、部分ブロック（階段、ハーフブロック）にも
 * 正しく塗装を分配する。</p>
 */
public final class InkPaintDistributor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".ink");

    public static final double MAX_PAINT_RADIUS_BLOCKS = 8.0;
    public static final int MAX_CANDIDATE_PATCHES_PER_OPERATION = 4096;
    public static final double DEFAULT_PAINT_RADIUS_BLOCKS = 2.0 / InkFaceData.GRID_SIZE;
    public static final int MAX_PROPAGATION_DEPTH = 3;

    /**
     * AABB 列挙時にブロック境界で隣接ブロックが欠落しないようにする探索用 epsilon。
     * 1e-6 で十分だが、float/double の境界判定の安全のため。1/1024 以下にする。
     */
    private static final double BLOCK_SCAN_EPSILON = 1.0e-6;

    private InkPaintDistributor() {}

    public static MultiSurfacePaintResult distributePaint(
            ServerLevel level,
            InkArena arena,
            InkStorage inkStorage,
            BlockPos hitBlockPos,
            Direction hitFace,
            Vec3 worldHitPos,
            double radiusBlocks,
            byte team) {

        if (!InkTeam.isValidTeam(team)) {
            return MultiSurfacePaintResult.fail(PaintFailureReason.INVALID_TEAM);
        }

        double clampedRadius = Math.min(radiusBlocks, MAX_PAINT_RADIUS_BLOCKS);
        if (radiusBlocks > MAX_PAINT_RADIUS_BLOCKS) {
            LOGGER.warn("Paint radius {} clamped to MAX_PAINT_RADIUS_BLOCKS={}",
                    radiusBlocks, MAX_PAINT_RADIUS_BLOCKS);
        }

        if (clampedRadius <= 0) {
            return MultiSurfacePaintResult.fail(PaintFailureReason.INVALID_TEAM);
        }

        // ブラシ中心は正確な着弾位置（内側補正は呼び出し元の責任）
        Vec3 sphereCenter = worldHitPos;

        // epsilon 付きで AABB を計算し、ブロック境界での欠落を防止
        int minX = Mth.floor(sphereCenter.x - clampedRadius - BLOCK_SCAN_EPSILON);
        int maxX = Mth.floor(sphereCenter.x + clampedRadius + BLOCK_SCAN_EPSILON);
        int minY = Mth.floor(sphereCenter.y - clampedRadius - BLOCK_SCAN_EPSILON);
        int maxY = Mth.floor(sphereCenter.y + clampedRadius + BLOCK_SCAN_EPSILON);
        int minZ = Mth.floor(sphereCenter.z - clampedRadius - BLOCK_SCAN_EPSILON);
        int maxZ = Mth.floor(sphereCenter.z + clampedRadius + BLOCK_SCAN_EPSILON);

        // 各ブラシ呼び出しごとに新規作成（射撃をまたいだ状態持越し防止）
        List<PatchCandidate> allCandidates = new ArrayList<>();
        Set<InkSurfaceKey> seen = new HashSet<>();
        int totalBlocksChecked = 0;
        int totalPaintableBlocks = 0;
        int totalExtractedPatches = 0;
        int totalIntersectingPatches = 0;

        double r2 = clampedRadius * clampedRadius;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    totalBlocksChecked++;

                    if (!arena.contains(pos)) continue;
                    if (!level.isLoaded(pos)) continue;

                    BlockState blockState = level.getBlockState(pos);
                    if (!InkPaintability.isPaintableBlock(level, pos, blockState)) continue;

                    // BlockAABB と球の交差判定で早期リジェクト
                    if (!sphereIntersectsBlockAABB(sphereCenter, clampedRadius, r2, pos)) {
                        continue;
                    }

                    totalPaintableBlocks++;

                    // 抽出されたパッチで走査（PatchId をキャッシュし、正しい BlockPos で構築）
                    List<InkSurfacePatch> patches =
                            InkSurfacePatchExtractor.extract(blockState, level, pos);

                    int blockIntersectionCount = 0;

                    for (InkSurfacePatch patch : patches) {
                        FaceBasis basis = FaceBasis.of(patch.id().normal());
                        double planeCoord = patch.id().plane()
                                / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
                        double minU = patch.id().minU()
                                / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
                        double maxU = patch.id().maxU()
                                / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
                        double minV = patch.id().minV()
                                / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
                        double maxV = patch.id().maxV()
                                / (double) InkSurfacePatchId.BLOCK_RESOLUTION;

                        // 球とパッチ矩形の交差判定
                        if (!basis.sphereIntersectsPatchRect(sphereCenter, clampedRadius,
                                pos, planeCoord, minU, minV, maxU, maxV)) {
                            continue;
                        }

                        // 簡易露出判定
                        if (!hasAirExposure(level, pos, patch)) {
                            continue;
                        }

                        // 重複排除キーは BlockPos + PatchId
                        InkSurfaceKey key = patch.toSurfaceKey();
                        if (!seen.add(key)) continue;

                        totalIntersectingPatches++;
                        blockIntersectionCount++;
                        allCandidates.add(new PatchCandidate(pos, patch, basis));
                    }

                    totalExtractedPatches += patches.size();

                    if (LOGGER.isDebugEnabled() && blockIntersectionCount > 0) {
                        LOGGER.debug("Candidate block: pos={} patches={} intersections={}",
                                pos, patches.size(), blockIntersectionCount);
                    }
                }
            }
        }

        int candidateCount = allCandidates.size();
        if (candidateCount > MAX_CANDIDATE_PATCHES_PER_OPERATION) {
            LOGGER.warn("Candidate patches {} exceeds MAX, aborting", candidateCount);
            return MultiSurfacePaintResult.fail(PaintFailureReason.OUTSIDE_ARENA);
        }

        LOGGER.info("Phase7 3D brush: Center={} Radius={} BlocksChecked={} PaintableBlocks={} "
                        + "ExtractedPatches={} CandidatePatches={}",
                sphereCenter, clampedRadius, totalBlocksChecked, totalPaintableBlocks,
                totalExtractedPatches, candidateCount);

        // --- 各パッチに塗装を分配 ---
        List<MultiSurfacePaintResult.UpdatedInkSurface> updatedSurfaces = new ArrayList<>();
        int totalChangedSurfaces = 0;
        int totalChangedCells = 0;

        Map<UUID, Map<InkSurfaceKey, InkFaceData>> arenaMap = inkStorage.getRawArenaMap();
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaMap.computeIfAbsent(
                arena.getArenaId(), k -> new HashMap<>());

        byte normalizedTeam = InkTeam.normalize(team);

        for (PatchCandidate candidate : allCandidates) {
            InkSurfacePatch patch = candidate.patch;
            FaceBasis basis = candidate.basis;

            InkSurfaceKey key = patch.toSurfaceKey();
            InkFaceData faceData = surfaces.computeIfAbsent(key, k -> new InkFaceData());

            // 球中心をパッチ面に投影し、パッチローカルUVを取得
            // clamp しない: 範囲外のUVは隣接ブロックとの連続性に必要
            FaceBasis.LocalUV localUV = patch.projectOntoPatch(sphereCenter);
            double pu = localUV.u();
            double pv = localUV.v();
            double radiusPatch = clampedRadius;

            double cellSize = 1.0 / InkFaceData.GRID_SIZE;
            int cellUMin = Math.max(0, (int) Math.floor((pu - radiusPatch) / cellSize));
            int cellUMax = Math.min(InkFaceData.GRID_SIZE - 1,
                    (int) Math.floor((pu + radiusPatch) / cellSize));
            int cellVMin = Math.max(0, (int) Math.floor((pv - radiusPatch) / cellSize));
            int cellVMax = Math.min(InkFaceData.GRID_SIZE - 1,
                    (int) Math.floor((pv + radiusPatch) / cellSize));

            int changed = 0;

            for (int cellV = cellVMin; cellV <= cellVMax; cellV++) {
                for (int cellU = cellUMin; cellU <= cellUMax; cellU++) {
                    double cellMinU = cellU * cellSize;
                    double cellMaxU = (cellU + 1) * cellSize;
                    double cellMinV = cellV * cellSize;
                    double cellMaxV = (cellV + 1) * cellSize;

                    double nearestU = InkFaceData.clamp(pu, cellMinU, cellMaxU);
                    double nearestV = InkFaceData.clamp(pv, cellMinV, cellMaxV);
                    double du = pu - nearestU;
                    double dv = pv - nearestV;

                    if (du * du + dv * dv <= radiusPatch * radiusPatch) {
                        int cellIndex = cellV * InkFaceData.GRID_SIZE + cellU;
                        if (faceData.getCellByIndex(cellIndex) != normalizedTeam) {
                            faceData.setCell(cellU, cellV, normalizedTeam);
                            changed++;
                        }
                    }
                }
            }

            if (changed > 0) {
                updatedSurfaces.add(MultiSurfacePaintResult.UpdatedInkSurface.from(
                        patch.pos(), patch.id().normal(), key, faceData, changed));
                totalChangedSurfaces++;
                totalChangedCells += changed;
            }

            if (faceData.isEmpty()) {
                surfaces.remove(key);
            }
        }

        if (surfaces.isEmpty()) {
            arenaMap.remove(arena.getArenaId());
        }

        LOGGER.info("Phase7 result: Arena #{} CandidatePatches={} ChangedSurfaces={} ChangedCells={}",
                arena.getArenaNumber(), candidateCount,
                totalChangedSurfaces, totalChangedCells);

        if (totalChangedSurfaces == 0) {
            return MultiSurfacePaintResult.fail(PaintFailureReason.NO_CHANGE);
        }

        return MultiSurfacePaintResult.success(totalChangedSurfaces, totalChangedCells,
                updatedSurfaces);
    }

    /**
     * 球がブロックの AABB と交差するか判定する。
     * 交差があれば必ず true、なければ false。
     *
     * <p>球と AABB の最短距離を計算し、r² 以下なら交差。</p>
     */
    private static boolean sphereIntersectsBlockAABB(Vec3 center, double radius,
                                                      double r2, BlockPos pos) {
        double bx = pos.getX();
        double by = pos.getY();
        double bz = pos.getZ();

        double nearestX = Math.clamp(center.x, bx, bx + 1.0);
        double nearestY = Math.clamp(center.y, by, by + 1.0);
        double nearestZ = Math.clamp(center.z, bz, bz + 1.0);

        double dx = center.x - nearestX;
        double dy = center.y - nearestY;
        double dz = center.z - nearestZ;
        return dx * dx + dy * dy + dz * dz <= r2;
    }

    /**
     * パッチが空気に露出しているか簡易判定。
     *
     * <p>正準フルキューブ外周面以外のパッチ（ハーフブロック側面・階段状側面・
     * 内部planeの部分パッチ等）は、VoxelShape抽出時に既にブロック外部への
     * 露出が保証されているため常に true を返す。</p>
     *
     * <p>正準フルキューブ外周面については隣接ブロックの occlusion チェックを行う。</p>
     */
    private static boolean hasAirExposure(ServerLevel level, BlockPos pos, InkSurfacePatch patch) {
        // 正準フルキューブ外周面以外は抽出時に露出が保証されているためスキップ
        if (!patch.id().isCanonicalFullCubeFace()) {
            return true;
        }

        Direction normal = patch.id().normal();
        BlockPos neighbor = pos.relative(normal);
        BlockState neighborState = level.getBlockState(neighbor);
        if (neighborState.isAir() || InkPaintability.isLiquidBlock(neighborState)) {
            return true;
        }
        return !neighborState.isCollisionShapeFullBlock(level, neighbor);
    }

    private record PatchCandidate(BlockPos blockPos, InkSurfacePatch patch, FaceBasis basis) {}
}
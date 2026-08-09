package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
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
 *
 * <p>Phase 10: 歪み円塗装。速度方向伸張、輪郭ノイズ、液滴飛沫を追加。</p>
 */
public final class InkPaintDistributor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".ink");

    public static final double MAX_PAINT_RADIUS_BLOCKS = 8.0;
    public static final int MAX_CANDIDATE_PATCHES_PER_OPERATION = 4096;
    public static final double DEFAULT_PAINT_RADIUS_BLOCKS = 2.0 / InkFaceData.GRID_SIZE;
    public static final int MAX_PROPAGATION_DEPTH = 3;

    private static final double BLOCK_SCAN_EPSILON = 1.0e-6;

    // ---- Phase 10 歪みパラメータ ----
    /** 輪郭ノイズの振幅（半径に対する割合） */
    private static final double EDGE_NOISE_AMPLITUDE = 0.25;
    /** 輪郭ノイズの周波数 */
    private static final double EDGE_NOISE_FREQUENCY = 3.0;
    // ---- Phase 10 streak パラメータ ----
    /** streak の角度広がり（速度方向を中心とした ± の範囲 rad。0.3 ≈ ±17°） */
    private static final double STREAK_ANGLE_SPREAD = 0.3;
    /** streak の根元の半幅（セル数。3.0 で根元が 6 セル幅、先端で 0 にテーパー） */
    private static final double STREAK_BASE_HALF_WIDTH_CELLS = 3.0;
    /** streak の最小長さ（UV単位。約 8 セル） */
    private static final double STREAK_MIN_LENGTH = 0.5;
    /** streak の最大長さ（UV単位。約 20 セル = 20/16 = 1.25） */
    private static final double STREAK_MAX_LENGTH = 1.25;
    /** streak の数（最小） */
    private static final int STREAK_COUNT_MIN = 2;
    /** streak の数（最大） */
    private static final int STREAK_COUNT_MAX = 5;
    /** 飛沫の数 */
    private static final int SPLATTER_COUNT_MIN = 3;
    private static final int SPLATTER_COUNT_MAX = 7;
    /** 飛沫の半径（メイン円半径に対する割合） */
    private static final double SPLATTER_RADIUS_MIN = 0.08;
    private static final double SPLATTER_RADIUS_MAX = 0.18;
    /** 飛沫の中心までの距離（メイン円半径に対する割合） */
    private static final double SPLATTER_DISTANCE_MIN = 0.7;
    private static final double SPLATTER_DISTANCE_MAX = 1.15;
    /** 液滴ウィング: 根本での追加幅（streak 基本幅の倍率） */
    private static final double DROPLET_WING_FACTOR = 1.8;
    /** 液滴ウィング: ウィングが消えるまでの長さ（streak 全長に対する割合） */
    private static final double DROPLET_WING_FADE_RATIO = 0.35;

    private InkPaintDistributor() {}

    public static MultiSurfacePaintResult distributePaint(
            ServerLevel level,
            InkArena arena,
            InkStorage inkStorage,
            BlockPos hitBlockPos,
            Direction hitFace,
            Vec3 worldHitPos,
            double radiusBlocks,
            byte team,
            @Nullable Vec3 impactVelocity) {

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

        Vec3 sphereCenter = worldHitPos;

        int minX = Mth.floor(sphereCenter.x - clampedRadius - BLOCK_SCAN_EPSILON);
        int maxX = Mth.floor(sphereCenter.x + clampedRadius + BLOCK_SCAN_EPSILON);
        int minY = Mth.floor(sphereCenter.y - clampedRadius - BLOCK_SCAN_EPSILON);
        int maxY = Mth.floor(sphereCenter.y + clampedRadius + BLOCK_SCAN_EPSILON);
        int minZ = Mth.floor(sphereCenter.z - clampedRadius - BLOCK_SCAN_EPSILON);
        int maxZ = Mth.floor(sphereCenter.z + clampedRadius + BLOCK_SCAN_EPSILON);

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

                    if (!sphereIntersectsBlockAABB(sphereCenter, clampedRadius, r2, pos)) {
                        continue;
                    }

                    totalPaintableBlocks++;

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

                        if (!basis.sphereIntersectsPatchRect(sphereCenter, clampedRadius,
                                pos, planeCoord, minU, minV, maxU, maxV)) {
                            continue;
                        }

                        if (!hasAirExposure(level, pos, patch)) {
                            continue;
                        }

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

        boolean useDistortion = impactVelocity != null && impactVelocity.lengthSqr() > 0.001;

        long distortionSeed = Mth.floor(sphereCenter.x * 1000)
                ^ (Mth.floor(sphereCenter.y * 1000) << 11)
                ^ (Mth.floor(sphereCenter.z * 1000) << 22);

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

            FaceBasis.LocalUV localUV = patch.projectOntoPatch(sphereCenter);
            double pu = localUV.u();
            double pv = localUV.v();
            double radiusPatch = clampedRadius;

            double cellSize = 1.0 / InkFaceData.GRID_SIZE;

            double velDirU = 0.0;
            double velDirV = 0.0;
            double velMagnitude = 0.0;

            if (useDistortion) {
                // FaceBasis.normal() returns Vec3i; convert to Vec3
                Vec3 normalVec = Vec3.atLowerCornerOf(basis.normal());
                Vec3 tangentVel = impactVelocity.subtract(
                        normalVec.scale(impactVelocity.dot(normalVec)));
                velMagnitude = tangentVel.length();

                if (velMagnitude > 0.001) {
                    Vec3 velDir = tangentVel.normalize();
                    // project velocity direction into patch UV axes (Vec3i -> Vec3)
                    Vec3 uAxis = Vec3.atLowerCornerOf(basis.uAxis());
                    Vec3 vAxis = Vec3.atLowerCornerOf(basis.vAxis());
                    velDirU = velDir.dot(uAxis);
                    velDirV = velDir.dot(vAxis);
                }
            }

            // セル走査範囲を streak の最大長で拡張
            double scanExpand = useDistortion ? STREAK_MAX_LENGTH : 0.0;
            int cellUMin = Math.max(0,
                    (int) Math.floor((pu - radiusPatch - scanExpand) / cellSize));
            int cellUMax = Math.min(InkFaceData.GRID_SIZE - 1,
                    (int) Math.floor((pu + radiusPatch + scanExpand) / cellSize));
            int cellVMin = Math.max(0,
                    (int) Math.floor((pv - radiusPatch - scanExpand) / cellSize));
            int cellVMax = Math.min(InkFaceData.GRID_SIZE - 1,
                    (int) Math.floor((pv + radiusPatch + scanExpand) / cellSize));

            int changed = 0;

            for (int cellV = cellVMin; cellV <= cellVMax; cellV++) {
                for (int cellU = cellUMin; cellU <= cellUMax; cellU++) {
                    double cellMinU = cellU * cellSize;
                    double cellMaxU = (cellU + 1) * cellSize;
                    double cellMinV = cellV * cellSize;
                    double cellMaxV = (cellV + 1) * cellSize;

                    double nearestU = InkFaceData.clamp(pu, cellMinU, cellMaxU);
                    double nearestV = InkFaceData.clamp(pv, cellMinV, cellMaxV);

                    if (useDistortion) {
                        double du = nearestU - pu;
                        double dv = nearestV - pv;

                        double effectiveR = getDistortedRadius(
                                du, dv, radiusPatch, velDirU, velDirV,
                                velMagnitude, distortionSeed, cellU, cellV);

                        if (du * du + dv * dv <= effectiveR * effectiveR) {
                            int cellIndex = cellV * InkFaceData.GRID_SIZE + cellU;
                            if (faceData.getCellByIndex(cellIndex) != normalizedTeam) {
                                faceData.setCell(cellU, cellV, normalizedTeam);
                                changed++;
                            }
                        }
                    } else {
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
     * 歪み半径を計算する。streak ベースのスプラッターパターン。
     *
     * <p>以下の要素を合成:
     * 1. 複数の太い streak（速度方向を中心に扇形に広がる）
     *    - 各 streak は進行方向にランダムな長さ（8〜20セル）
     *    - 根元は6セル幅、先端に向かって線形テーパーで細くなる
     * 2. 角度依存の輪郭ノイズ（ハッシュベース）
     * 3. 飛沫の追加判定（円周付近の小円）
     */
    private static double getDistortedRadius(
            double du, double dv, double baseRadius,
            double velDirU, double velDirV,
            double velMagnitude, long seed, int cellU, int cellV) {

        if (baseRadius <= 0) return 0;

        double distance = Math.sqrt(du * du + dv * dv);
        if (distance < 1e-12) return baseRadius;

        // 1. 方向の単位ベクトル
        double duNorm = du / distance;
        double dvNorm = dv / distance;
        double cellAngle = Math.atan2(dvNorm, duNorm);

        // 2. 輪郭ノイズ（角度依存）
        double noise = sampleEdgeNoise(cellAngle, seed);
        double noiseFactor = 1.0 + noise * EDGE_NOISE_AMPLITUDE;

        // 3. 基本有効半径（ノイズ変形のみ）
        double effectiveRadius = baseRadius * noiseFactor;

        // 4. streak 判定
        if (velMagnitude > 0.001) {
            // 速度方向成分（進行方向のみ、逆方向には伸びない）
            double velAlign = duNorm * velDirU + dvNorm * velDirV;
            if (velAlign > 0) {
                double velAngle = Math.atan2(velDirV, velDirU);
                long streakSeed = seed ^ 0x7A3F2E1DL;
                int streakCount = STREAK_COUNT_MIN
                        + (int)(InkFaceData.hashToDouble(streakSeed)
                        * (STREAK_COUNT_MAX - STREAK_COUNT_MIN + 1));

                for (int i = 0; i < streakCount; i++) {
                    long sSeed = streakSeed ^ (i * 0x9E3779B9L);

                    // 速度方向 ± STREAK_ANGLE_SPREAD 以内のランダムな角度
                    double angleOffset = (InkFaceData.hashToDouble(sSeed) * 2.0 - 1.0)
                            * STREAK_ANGLE_SPREAD;
                    double streakAngle = velAngle + angleOffset;

                    // ランダムな長さ
                    double streakLength = STREAK_MIN_LENGTH
                            + InkFaceData.hashToDouble(sSeed ^ 1)
                            * (STREAK_MAX_LENGTH - STREAK_MIN_LENGTH);

                    // streak 方向に沿った距離
                    double alongDist = distance * Math.cos(cellAngle - streakAngle);
                    if (alongDist <= 0 || alongDist > streakLength) continue;

                    // streak 中心線からの垂直距離
                    double perpDist = Math.abs(distance * Math.sin(cellAngle - streakAngle));

                    // 先端に近いほど細くなる（線形テーパー）
                    double taper = 1.0 - (alongDist / streakLength);
                    double streakHalfWidth = (STREAK_BASE_HALF_WIDTH_CELLS / InkFaceData.GRID_SIZE) * taper;

                    // 液滴ウィング: 円の境界（alongDist == baseRadius）から
                    // streak の左右にインクを追加して雫型にする。
                    // streak 長の DROPLET_WING_FADE_RATIO 分で徐々に消える。
                    double wingExtra = 0.0;
                    double distPastCircleAlongStreak = alongDist - baseRadius;
                    double wingFadeLength = streakLength * DROPLET_WING_FADE_RATIO;
                    if (distPastCircleAlongStreak > 0 && distPastCircleAlongStreak < wingFadeLength) {
                        double wingFade = 1.0 - (distPastCircleAlongStreak / wingFadeLength);
                        wingExtra = streakHalfWidth * DROPLET_WING_FACTOR * wingFade;
                    }

                    if (perpDist < streakHalfWidth + wingExtra) {
                        // セルが streak 内 → このセルを塗る
                        effectiveRadius = Math.max(effectiveRadius, distance + 1.0e-6);
                        break;  // 1つの streak に入れば十分
                    }
                }
            }
        }

        // 5. 飛沫判定: 円周の外側に飛沫小円があるか
        double splatterBoost = checkSplatter(du, dv, baseRadius, seed, cellU, cellV);
        if (splatterBoost > effectiveRadius) {
            effectiveRadius = splatterBoost;
        }

        return effectiveRadius;
    }

    /**
     * 角度に基づく輪郭ノイズをサンプリングする。
     * 2つの異なる周波数のノイズを合成して有機的な歪みを生成する。
     */
    private static double sampleEdgeNoise(double angle, long seed) {
        double a1 = angle * EDGE_NOISE_FREQUENCY;
        long s1 = seed ^ ((long)(a1 * 1000));
        double n1 = InkFaceData.hashToDouble(s1);

        double a2 = angle * EDGE_NOISE_FREQUENCY * 2.3 + 1.7;
        long s2 = seed ^ ((long)(a2 * 1000)) ^ 0xABCD;
        double n2 = InkFaceData.hashToDouble(s2);

        // n1: 大まかな歪み、n2: 細かいディテール
        return (n1 - 0.5) * 1.5 + (n2 - 0.5) * 0.5;
    }

    /**
     * 飛沫（スプラッター）判定。
     * 複数の飛沫小円のうち、最も近いものまでの距離を元に
     * このセルが塗られるべきかを判定する。
     *
     * @return このセルを塗るのに必要な最小半径（baseRadius より大きければ塗られる）
     */
    private static double checkSplatter(
            double du, double dv, double baseRadius,
            long seed, int cellU, int cellV) {

        long splatterSeed = seed ^ 0x5F1A7L;
        int splatterCount = SPLATTER_COUNT_MIN
                + (int)(InkFaceData.hashToDouble(splatterSeed) * (SPLATTER_COUNT_MAX - SPLATTER_COUNT_MIN + 1));

        double bestEffectiveRadius = 0;

        for (int i = 0; i < splatterCount; i++) {
            long sSeed = splatterSeed ^ (i * 0x9E3779B9L);

            double sAngle = InkFaceData.hashToDouble(sSeed) * Math.PI * 2.0;
            double sDist = SPLATTER_DISTANCE_MIN
                    + InkFaceData.hashToDouble(sSeed ^ 1) * (SPLATTER_DISTANCE_MAX - SPLATTER_DISTANCE_MIN);
            double sRadius = SPLATTER_RADIUS_MIN
                    + InkFaceData.hashToDouble(sSeed ^ 2) * (SPLATTER_RADIUS_MAX - SPLATTER_RADIUS_MIN);

            double scU = Math.cos(sAngle) * sDist * baseRadius;
            double scV = Math.sin(sAngle) * sDist * baseRadius;

            double sdu = du - scU;
            double sdv = dv - scV;

            double cellToSplatterDist = Math.sqrt(sdu * sdu + sdv * sdv);
            if (cellToSplatterDist <= sRadius * baseRadius) {
                double neededR = Math.sqrt(du * du + dv * dv) + 0.001;
                if (neededR > bestEffectiveRadius) {
                    bestEffectiveRadius = neededR;
                }
            }
        }

        return bestEffectiveRadius;
    }

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

    private static boolean hasAirExposure(ServerLevel level, BlockPos pos, InkSurfacePatch patch) {
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

    /**
     * 後方互換性のためのオーバーロード（velocity なし）。
     */
    public static MultiSurfacePaintResult distributePaint(
            ServerLevel level,
            InkArena arena,
            InkStorage inkStorage,
            BlockPos hitBlockPos,
            Direction hitFace,
            Vec3 worldHitPos,
            double radiusBlocks,
            byte team) {
        return distributePaint(level, arena, inkStorage, hitBlockPos, hitFace,
                worldHitPos, radiusBlocks, team, null);
    }

    private record PatchCandidate(BlockPos blockPos, InkSurfacePatch patch, FaceBasis basis) {}
}
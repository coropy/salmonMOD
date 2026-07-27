package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;

import java.util.*;

/**
 * インク塗装を3D球ベースで複数ブロック・複数方向面へ分配する。
 *
 * <p>Phase 5: 球AABB内の全候補ブロック×6面を走査し、
 * {@link FaceBasis#sphereIntersectsFace(Vec3, double, BlockPos)} で
 * 球と面の交差判定 → 露出判定 → 面上投影 → 円塗装 の流れで、
 * 同一平面だけでなく90度角面への回り込みも自然に処理する。</p>
 *
 * <p>従来の同一平面限定の分配は削除し、この3D統一方式に置き換える。</p>
 */
public final class InkPaintDistributor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".ink");

    /** 塗装可能な最大半径（ブロック単位） */
    public static final double MAX_PAINT_RADIUS_BLOCKS = 8.0;

    /** 1操作あたりの最大候補面数 */
    public static final int MAX_CANDIDATE_SURFACES_PER_OPERATION = 4096; // 6面×約682ブロック相当

    /** デフォルトの塗装半径（ブロック単位）。2セル ÷ 8セル/ブロック = 0.25 */
    public static final double DEFAULT_PAINT_RADIUS_BLOCKS = 2.0 / InkFaceData.GRID_SIZE;

    /** ブラシ伝播の最大深さ（無限ループ防止用。現在は使わないが安全策） */
    public static final int MAX_PROPAGATION_DEPTH = 3;

    private InkPaintDistributor() {}

    /**
     * ワールドヒット座標を中心とする3D球を塗装ブラシとし、
     * 球と交差するすべての塗装可能面にインクを分配する。
     *
     * <p>Phase 5: 同一平面だけでなく、90度角面（同じブロックの異なる面、
     * 隣接ブロックの面）にもインクが自然に回り込む。</p>
     *
     * @param level          サーバーレベル
     * @param arena          対象アリーナ
     * @param inkStorage     インクストレージ
     * @param hitBlockPos    クリックしたブロックの座標
     * @param hitFace        クリックした面の方向
     * @param worldHitPos    ワールド座標でのヒット位置
     * @param radiusBlocks   塗装半径（ブロック単位）
     * @param team           チーム値
     * @return 塗装分配結果
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

        // チーム値検証
        if (!InkTeam.isValidTeam(team)) {
            return MultiSurfacePaintResult.fail(PaintFailureReason.INVALID_TEAM);
        }

        // 半径検証・クランプ
        double clampedRadius = Math.min(radiusBlocks, MAX_PAINT_RADIUS_BLOCKS);
        if (radiusBlocks > MAX_PAINT_RADIUS_BLOCKS) {
            LOGGER.warn("Paint radius {} clamped to MAX_PAINT_RADIUS_BLOCKS={}",
                    radiusBlocks, MAX_PAINT_RADIUS_BLOCKS);
        }

        if (clampedRadius <= 0) {
            return MultiSurfacePaintResult.fail(PaintFailureReason.INVALID_TEAM);
        }

        // --- 3D球の中心 ---
        Vec3 sphereCenter = worldHitPos;

        // --- 球のAABBから候補BlockPos範囲を計算 ---
        int minX = Mth.floor(sphereCenter.x - clampedRadius);
        int maxX = Mth.floor(sphereCenter.x + clampedRadius);
        int minY = Mth.floor(sphereCenter.y - clampedRadius);
        int maxY = Mth.floor(sphereCenter.y + clampedRadius);
        int minZ = Mth.floor(sphereCenter.z - clampedRadius);
        int maxZ = Mth.floor(sphereCenter.z + clampedRadius);

        // --- 候補面を列挙（球AABB内の全BlockPos × 6面） ---
        List<CandidateSurface> allCandidates = new ArrayList<>();
        Set<InkSurfaceKey> seen = new HashSet<>();
        int totalBlocksChecked = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    totalBlocksChecked++;

                    // アリーナ内チェック
                    if (!arena.contains(pos)) {
                        continue;
                    }

                    // チャンクロードチェック
                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    // 各面について候補判定
                    for (Direction face : Direction.values()) {
                        FaceBasis basis = FaceBasis.of(face);

                        // 球と面矩形の交差判定
                        if (!basis.sphereIntersectsFace(sphereCenter, clampedRadius, pos)) {
                            continue;
                        }

                        // 塗装可能ブロックか（静か）
                        if (!InkPaintability.isPaintableBlock(level.getBlockState(pos))) {
                            continue;
                        }

                        // 面が露出しているか
                        if (!InkPaintability.isSurfaceExposed(level, pos, face)) {
                            continue;
                        }

                        InkSurfaceKey key = new InkSurfaceKey(pos, face);
                        if (!seen.add(key)) {
                            continue; // 重複防止（通常起きないが安全策）
                        }

                        allCandidates.add(new CandidateSurface(pos, face, basis));
                    }
                }
            }
        }

        int candidateCount = allCandidates.size();
        LOGGER.info("Phase5 3D brush: Center={} Radius={} BlocksChecked={} CandidateSurfaces={}",
                sphereCenter, clampedRadius, totalBlocksChecked, candidateCount);

        // 候補面数上限チェック
        if (candidateCount > MAX_CANDIDATE_SURFACES_PER_OPERATION) {
            LOGGER.warn("Candidate surfaces {} exceeds MAX_CANDIDATE_SURFACES_PER_OPERATION={}, aborting",
                    candidateCount, MAX_CANDIDATE_SURFACES_PER_OPERATION);
            return MultiSurfacePaintResult.fail(PaintFailureReason.OUTSIDE_ARENA);
        }

        // --- 各候補面に塗装を分配 ---
        List<MultiSurfacePaintResult.UpdatedInkSurface> updatedSurfaces = new ArrayList<>();
        int totalChangedSurfaces = 0;
        int totalChangedCells = 0;

        Map<UUID, Map<InkSurfaceKey, InkFaceData>> arenaMap = inkStorage.getRawArenaMap();
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaMap.computeIfAbsent(
                arena.getArenaId(), k -> new HashMap<>());

        for (CandidateSurface candidate : allCandidates) {
            BlockPos pos = candidate.blockPos;
            Direction face = candidate.face;
            FaceBasis basis = candidate.basis;

            // 球中心をこの面の平面上に投影し、ローカルUVを取得
            FaceBasis.LocalUV localUV = basis.projectOntoFace(sphereCenter, pos);

            // UV座標系の半径（1ブロック = 1.0 UV）
            double radiusUV = clampedRadius;

            // ラスタライズ半径（UV空間で半径がセル何個分かを計算し、必要なセル範囲だけ走査）
            // 円と交差する可能性のあるセル範囲を計算
            double cellSizeUV = 1.0 / InkFaceData.GRID_SIZE;
            int cellUMin = Math.max(0, (int) Math.floor((localUV.u() - radiusUV) / cellSizeUV));
            int cellUMax = Math.min(InkFaceData.GRID_SIZE - 1, (int) Math.floor((localUV.u() + radiusUV) / cellSizeUV));
            int cellVMin = Math.max(0, (int) Math.floor((localUV.v() - radiusUV) / cellSizeUV));
            int cellVMax = Math.min(InkFaceData.GRID_SIZE - 1, (int) Math.floor((localUV.v() + radiusUV) / cellSizeUV));

            // 面データ取得
            InkSurfaceKey key = new InkSurfaceKey(pos, face);
            InkFaceData faceData = surfaces.computeIfAbsent(key, k -> new InkFaceData());

            // 変更カウント（セル単位で判定）
            byte normalizedTeam = InkTeam.normalize(team);
            int changed = 0;

            for (int cellV = cellVMin; cellV <= cellVMax; cellV++) {
                for (int cellU = cellUMin; cellU <= cellUMax; cellU++) {
                    // セル矩形と円の交差判定
                    double cellMinU = cellU * cellSizeUV;
                    double cellMaxU = (cellU + 1) * cellSizeUV;
                    double cellMinV = cellV * cellSizeUV;
                    double cellMaxV = (cellV + 1) * cellSizeUV;

                    double nearestU = InkFaceData.clamp(localUV.u(), cellMinU, cellMaxU);
                    double nearestV = InkFaceData.clamp(localUV.v(), cellMinV, cellMaxV);
                    double du = localUV.u() - nearestU;
                    double dv = localUV.v() - nearestV;

                    if (du * du + dv * dv <= radiusUV * radiusUV) {
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
                        pos, face, faceData, changed));
                totalChangedSurfaces++;
                totalChangedCells += changed;

                LOGGER.info("  Phase5 painted: block={} face={} localUV=({},{}) changed={}",
                        pos, face,
                        String.format("%.3f", localUV.u()),
                        String.format("%.3f", localUV.v()),
                        changed);
            }

            // 面が空になったら削除
            if (faceData.isEmpty()) {
                surfaces.remove(key);
            }
        }

        // アリーナの面データが空になったら削除
        if (surfaces.isEmpty()) {
            arenaMap.remove(arena.getArenaId());
        }

        LOGGER.info("Phase5 result: Arena #{} HitFace={} Radius={} CandidateSurfaces={} ChangedSurfaces={} ChangedCells={}",
                arena.getArenaNumber(), hitFace, clampedRadius, candidateCount,
                totalChangedSurfaces, totalChangedCells);

        if (totalChangedSurfaces == 0) {
            return MultiSurfacePaintResult.fail(PaintFailureReason.NO_CHANGE);
        }

        return MultiSurfacePaintResult.success(totalChangedSurfaces, totalChangedCells,
                updatedSurfaces);
    }

    /**
     * 候補面の内部表現。
     */
    private record CandidateSurface(BlockPos blockPos, Direction face, FaceBasis basis) {}
}
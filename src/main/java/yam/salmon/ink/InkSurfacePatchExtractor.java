package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlockStateのVoxelShapeから外部に露出する軸平行矩形面（InkSurfacePatch）を抽出する。
 *
 * <p>Phase 9.1: 16×16×16 固定グリッドベースの表面抽出。
 * 座標系を完全修正:
 * - solid cell index i → 境界面 plane = i (NEGATIVE) / i+1 (POSITIVE)
 * - 隣接判定は solid[inside] && !solid[outside] の基本ルール
 * - U/V軸は FaceBasis の定義に完全準拠</p>
 *
 * <p>Surface cell 抽出後、同一 normal + plane のセル群を greedy meshing で矩形統合する。</p>
 */
public final class InkSurfacePatchExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".ink");
    private static final int RES = InkSurfacePatchId.BLOCK_RESOLUTION;

    /**
     * BlockState → List<InkSurfacePatchId> のキャッシュ。
     *
     * <p>BlockPos を含まない PatchId のみをキャッシュする。
     * 同一 BlockState の異なるブロック（例: 同じ向きの階段ブロック）でも、
     * 呼び出し時に正しい BlockPos で InkSurfacePatch を再構築する。</p>
     */
    private static final Map<BlockState, List<InkSurfacePatchId>> PATCH_CACHE = new ConcurrentHashMap<>();

    private InkSurfacePatchExtractor() {}

    /**
     * BlockState から面パッチ一覧を抽出する。
     *
     * <p>フルキューブは高速パス（キャッシュ未使用）。
     * 部分ブロックは PATCH_CACHE から PatchId 一覧を取得し、
     * 呼び出し時の正しい BlockPos で InkSurfacePatch を再構築する。</p>
     */
    public static List<InkSurfacePatch> extract(BlockState state, BlockGetter level, BlockPos pos) {
        if (isFullCube(state)) {
            return fullCubePatches(pos);
        }
        // PatchId をキャッシュし、毎回正しい BlockPos で InkSurfacePatch を構築
        List<InkSurfacePatchId> cachedIds = PATCH_CACHE.computeIfAbsent(
                state, s -> extractIdsRaw(s, level, pos));
        List<InkSurfacePatch> patches = new ArrayList<>(cachedIds.size());
        for (InkSurfacePatchId id : cachedIds) {
            patches.add(new InkSurfacePatch(id, pos));
        }
        return patches;
    }

    /**
     * VoxelShape から PatchId 一覧を抽出（キャッシュ未使用時の初回計算）。
     * pos は VoxelShape 取得用（BlockPos がキャッシュ対象ではない）。
     */
    private static List<InkSurfacePatchId> extractIdsRaw(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape;
        try {
            shape = state.getShape(level, pos);
        } catch (Exception e) {
            shape = state.getCollisionShape(level, pos);
        }
        if (shape.isEmpty()) return List.of();

        boolean[][][] grid = buildSolidGrid(shape);
        if (grid == null) return List.of();

        List<SurfaceCell> surfaceCells = new ArrayList<>();
        for (Direction normal : Direction.values()) {
            extractSurfaceCells(grid, normal, surfaceCells);
        }

        List<FaceCandidate> merged = mergeSurfaceCells(surfaceCells);
        merged.sort(FaceCandidate.COMPARATOR);

        List<InkSurfacePatchId> ids = new ArrayList<>();
        for (FaceCandidate fc : merged) {
            ids.add(new InkSurfacePatchId(fc.normal, fc.plane,
                    fc.minU, fc.minV, fc.maxU, fc.maxV));
        }
        return ids;
    }

    // ========================================================================
    // Solid Grid 構築
    // ========================================================================

    private static boolean[][][] buildSolidGrid(VoxelShape shape) {
        boolean[][][] grid = new boolean[RES][RES][RES];
        boolean hasAnySolid = false;
        for (int y = 0; y < RES; y++) {
            for (int z = 0; z < RES; z++) {
                for (int x = 0; x < RES; x++) {
                    double cx = (x + 0.5) / RES;
                    double cy = (y + 0.5) / RES;
                    double cz = (z + 0.5) / RES;
                    if (shapeContains(shape, cx, cy, cz)) {
                        grid[y][z][x] = true;
                        hasAnySolid = true;
                    }
                }
            }
        }
        return hasAnySolid ? grid : null;
    }

    private static boolean shapeContains(VoxelShape shape, double x, double y, double z) {
        final boolean[] result = {false};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            if (!result[0]
                    && x >= minX && x < maxX
                    && y >= minY && y < maxY
                    && z >= minZ && z < maxZ) {
                result[0] = true;
            }
        });
        return result[0];
    }

    // ========================================================================
    // Surface Cell 抽出
    //
    // 各 solid cell (cx,cy,cz) について、法線方向の隣接が empty または
    // ブロック範囲外の場合に表面セルを生成する。
    //
    // boundary plane の決定:
    //   POSITIVE normal (UP/EAST/SOUTH): plane = coord + 1
    //   NEGATIVE normal (DOWN/WEST/NORTH): plane = coord
    // ========================================================================

    private static void extractSurfaceCells(boolean[][][] grid, Direction normal,
                                            List<SurfaceCell> out) {
        for (int y = 0; y < RES; y++) {
            for (int z = 0; z < RES; z++) {
                for (int x = 0; x < RES; x++) {
                    if (!grid[y][z][x]) continue;

                    int nx = x + normal.getStepX();
                    int ny = y + normal.getStepY();
                    int nz = z + normal.getStepZ();

                    boolean outside;
                    if (nx < 0 || nx >= RES || ny < 0 || ny >= RES || nz < 0 || nz >= RES) {
                        outside = true;
                    } else {
                        outside = !grid[ny][nz][nx];
                    }

                    if (!outside) continue;

                    int plane = boundaryPlane(normal, x, y, z);
                    FaceCellUV uv = toFaceCellUV(normal, x, y, z);
                    out.add(new SurfaceCell(normal, plane, uv.u, uv.v));
                }
            }
        }
    }

    /**
     * solid cell (x,y,z) の法線方向境界面座標を返す。
     *
     * <pre>
     * cell index i → [i, i+1]
     *
     * WEST  / negative X: plane = x       (cell の始端)
     * EAST  / positive X: plane = x + 1   (cell の終端)
     *
     * DOWN  / negative Y: plane = y
     * UP    / positive Y: plane = y + 1
     *
     * NORTH / negative Z: plane = z
     * SOUTH / positive Z: plane = z + 1
     * </pre>
     */
    static int boundaryPlane(Direction normal, int x, int y, int z) {
        return switch (normal) {
            case WEST -> x;
            case EAST -> x + 1;
            case DOWN -> y;
            case UP -> y + 1;
            case NORTH -> z;
            case SOUTH -> z + 1;
        };
    }

    /**
     * solid cell (x,y,z) の、指定法線面における U/V 座標を返す。
     *
     * <p>U/V 軸は FaceBasis の定義と完全に一致させる:
     * <pre>
     *   face   | normal   | uAxis    | vAxis
     *   -------|----------|----------|----------
     *   UP     | ( 0, 1, 0) | X+      | Z+
     *   DOWN   | ( 0,-1, 0) | X+      | Z- (反転)
     *   NORTH  | ( 0, 0,-1) | X- (反転) | Y+
     *   SOUTH  | ( 0, 0, 1) | X+      | Y+
     *   WEST   | (-1, 0, 0) | Z+      | Y+
     *   EAST   | ( 1, 0, 0) | Z- (反転) | Y+
     * </pre>
     *
     * <p>Surface cell の U/V は 0..15 の cell index であり、
     * パッチ矩形では minU..maxU (exclusive) として使われる。</p>
     */
    static FaceCellUV toFaceCellUV(Direction normal, int x, int y, int z) {
        return switch (normal) {
            case UP    -> new FaceCellUV(x, z);
            case DOWN  -> new FaceCellUV(x, 15 - z);
            case SOUTH -> new FaceCellUV(x, 15 - y);
            case NORTH -> new FaceCellUV(15 - x, 15 - y);
            case EAST  -> new FaceCellUV(15 - z, 15 - y);
            case WEST  -> new FaceCellUV(z, 15 - y);
        };
    }

    record FaceCellUV(int u, int v) {}

    // ========================================================================
    // Greedy 矩形統合
    // ========================================================================

    private static List<FaceCandidate> mergeSurfaceCells(List<SurfaceCell> cells) {
        record GroupKey(Direction normal, int plane) {}
        Map<GroupKey, List<SurfaceCell>> groups = new LinkedHashMap<>();
        for (SurfaceCell sc : cells) {
            groups.computeIfAbsent(new GroupKey(sc.normal, sc.plane), k -> new ArrayList<>()).add(sc);
        }

        List<FaceCandidate> result = new ArrayList<>();
        for (var entry : groups.entrySet()) {
            Direction normal = entry.getKey().normal;
            int plane = entry.getKey().plane;
            List<SurfaceCell> group = entry.getValue();

            boolean[][] occupied = new boolean[RES][RES];
            for (SurfaceCell sc : group) {
                if (sc.u >= 0 && sc.u < RES && sc.v >= 0 && sc.v < RES) {
                    occupied[sc.v][sc.u] = true;
                }
            }

            boolean[][] used = new boolean[RES][RES];
            for (int v = 0; v < RES; v++) {
                for (int u = 0; u < RES; u++) {
                    if (!occupied[v][u] || used[v][u]) continue;

                    int maxU = u;
                    while (maxU + 1 < RES && occupied[v][maxU + 1] && !used[v][maxU + 1]) {
                        maxU++;
                    }

                    int maxV = v;
                    while (maxV + 1 < RES) {
                        boolean canExtend = true;
                        for (int cu = u; cu <= maxU; cu++) {
                            if (!occupied[maxV + 1][cu] || used[maxV + 1][cu]) {
                                canExtend = false;
                                break;
                            }
                        }
                        if (canExtend) {
                            maxV++;
                        } else {
                            break;
                        }
                    }

                    for (int cv = v; cv <= maxV; cv++) {
                        for (int cu = u; cu <= maxU; cu++) {
                            used[cv][cu] = true;
                        }
                    }

                    // maxU+1, maxV+1 は exclusive
                    result.add(new FaceCandidate(normal, plane,
                            u, v, maxU + 1, maxV + 1));
                }
            }
        }
        return result;
    }

    // ========================================================================
    // フルキューブ検出
    // ========================================================================

    public static List<InkSurfacePatch> fullCubePatches(BlockPos pos) {
        List<InkSurfacePatch> patches = new ArrayList<>();
        for (Direction face : Direction.values()) {
            patches.add(new InkSurfacePatch(InkSurfacePatchId.fullFace(face), pos));
        }
        return patches;
    }

    public static boolean isFullCube(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof SlabBlock) {
            return state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE;
        }
        return state.isCollisionShapeFullBlock(null, null);
    }

    public static void clearCache() { PATCH_CACHE.clear(); }

    // ========================================================================
    // Records
    // ========================================================================

    private record SurfaceCell(Direction normal, int plane, int u, int v) {}

    private record FaceCandidate(
            Direction normal, int plane, int minU, int minV, int maxU, int maxV) {
        static final Comparator<FaceCandidate> COMPARATOR = Comparator
                .comparingInt((FaceCandidate fc) -> fc.normal.ordinal())
                .thenComparingInt(fc -> fc.plane)
                .thenComparingInt(fc -> fc.minU)
                .thenComparingInt(fc -> fc.minV)
                .thenComparingInt(fc -> fc.maxU)
                .thenComparingInt(fc -> fc.maxV);
    }
}
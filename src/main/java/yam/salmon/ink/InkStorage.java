package yam.salmon.ink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;

import java.util.*;

/**
 * アリーナごとのインクデータを管理するストレージ。
 *
 * <p>疎な保存方式: 実際に塗られた面だけ {@link InkSurfaceKey} → {@link InkFaceData} で保持する。
 * アリーナ作成時に全ブロック×6面のデータを確保しない。</p>
 *
 * <p>スレッドセーフ: サーバースレッドからのみアクセスされる前提（synchronized不要）。</p>
 */
public class InkStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".ink");

    /** デフォルトの塗装半径（UV座標系） */
    public static final double DEFAULT_PAINT_RADIUS = 0.24;

    /** アリーナUUID → 塗装面マップ */
    private final Map<UUID, Map<InkSurfaceKey, InkFaceData>> arenaInks = new HashMap<>();

    /**
     * 塗装操作を実行する（1面 / フルブロック互換）。
     */
    public PaintResult paint(ServerLevel level, InkArena arena,
                              BlockPos blockPos, Direction face,
                              InkFaceCoordinates coords,
                              double radius, byte team) {
        if (!InkTeam.isValidTeam(team)) {
            return PaintResult.fail(PaintFailureReason.INVALID_TEAM);
        }

        if (!arena.contains(blockPos)) {
            return PaintResult.fail(PaintFailureReason.OUTSIDE_ARENA);
        }

        if (!InkPaintability.isPaintableBlock(level, blockPos, level.getBlockState(blockPos))) {
            return PaintResult.fail(PaintFailureReason.NOT_PAINTABLE_BLOCK);
        }

        if (!InkPaintability.isSurfaceExposed(level, blockPos, face)) {
            return PaintResult.fail(PaintFailureReason.FACE_OCCLUDED);
        }

        InkSurfaceKey key = new InkSurfaceKey(blockPos, InkSurfacePatchId.fullFace(face));
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.computeIfAbsent(
                arena.getArenaId(), k -> new HashMap<>());

        InkFaceData faceData = surfaces.computeIfAbsent(key, k -> new InkFaceData());

        int changed = faceData.paintCircle(coords.u(), coords.v(), radius, team);

        if (changed == 0) {
            return PaintResult.noChange(arena.getArenaNumber(), blockPos, face, coords, team);
        }

        if (faceData.isEmpty()) {
            surfaces.remove(key);
            if (surfaces.isEmpty()) {
                arenaInks.remove(arena.getArenaId());
            }
            LOGGER.info("Ink surface removed (empty): arena #{} block={} face={}",
                    arena.getArenaNumber(), blockPos, face);
        } else {
            LOGGER.info("Ink paint success: arena #{} block={} face={} team={} changed={} cells",
                    arena.getArenaNumber(), blockPos, face, InkTeam.toName(team), changed);
        }

        return PaintResult.success(changed, arena.getArenaNumber(),
                blockPos, face, coords, team);
    }

    /**
     * 指定アリーナの指定面データを取得する（Patch ID 指定）。
     */
    public Optional<InkFaceData> getFace(InkArena arena, BlockPos blockPos, InkSurfacePatchId patchId) {
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.get(arena.getArenaId());
        if (surfaces == null) return Optional.empty();
        return Optional.ofNullable(surfaces.get(new InkSurfaceKey(blockPos, patchId)));
    }

    /**
     * 指定アリーナの指定面データを取得する（Direction指定 / 後方互換）。
     */
    public Optional<InkFaceData> getFace(InkArena arena, BlockPos blockPos, Direction face) {
        return getFace(arena, blockPos, InkSurfacePatchId.fullFace(face));
    }

    /**
     * 面データがない場合に空の InkFaceData 表現を返す（inspect用）。
     */
    public InkFaceData getFaceOrEmpty(InkArena arena, BlockPos blockPos, Direction face) {
        return getFace(arena, blockPos, face).orElse(new InkFaceData());
    }

    /**
     * アリーナの全インクデータを消去する。
     */
    public int clearArena(InkArena arena) {
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.remove(arena.getArenaId());
        if (surfaces == null) return 0;
        int count = surfaces.size();
        surfaces.clear();
        LOGGER.info("Ink cleared: arena #{} / {} surfaces removed",
                arena.getArenaNumber(), count);
        return count;
    }

    /**
     * アリーナの全インクデータを削除する。
     */
    public int removeArena(InkArena arena) {
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.remove(arena.getArenaId());
        if (surfaces == null) return 0;
        int count = surfaces.size();
        surfaces.clear();
        LOGGER.info("Ink removed with arena: arena #{} / {} surfaces",
                arena.getArenaNumber(), count);
        return count;
    }

    /**
     * 指定アリーナの塗装面数を返す。
     */
    public int getSurfaceCount(InkArena arena) {
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.get(arena.getArenaId());
        return surfaces != null ? surfaces.size() : 0;
    }

    /**
     * 内部データマップの直接参照を返す（InkPaintDistributor 用）。
     */
    public Map<UUID, Map<InkSurfaceKey, InkFaceData>> getRawArenaMap() {
        return arenaInks;
    }

    public boolean hasData() {
        return !arenaInks.isEmpty();
    }

    public void clearAll() {
        arenaInks.clear();
    }

    // ========================================================================
    // 保存用データ構造と Codec (dataVersion=4: Patch ID 対応)
    // ========================================================================

    /**
     * 保存用: 1面のデータ。
     */
    public record SavedSurface(BlockPos blockPos, String faceName, byte[] cells,
                                int plane, int minU, int minV, int maxU, int maxV) {
        public static final Codec<byte[]> CELLS_CODEC = Codec.BYTE.listOf()
                .xmap(
                        list -> {
                            byte[] arr = new byte[list.size()];
                            for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
                            return arr;
                        },
                        arr -> {
                            List<Byte> list = new ArrayList<>(arr.length);
                            for (byte b : arr) list.add(b);
                            return list;
                        }
                );

        public static final Codec<SavedSurface> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("blockPos").forGetter(SavedSurface::blockPos),
                Codec.STRING.fieldOf("face").forGetter(SavedSurface::faceName),
                CELLS_CODEC.fieldOf("cells").forGetter(SavedSurface::cells),
                Codec.INT.optionalFieldOf("plane", -1).forGetter(SavedSurface::plane),
                Codec.INT.optionalFieldOf("minU", -1).forGetter(SavedSurface::minU),
                Codec.INT.optionalFieldOf("minV", -1).forGetter(SavedSurface::minV),
                Codec.INT.optionalFieldOf("maxU", -1).forGetter(SavedSurface::maxU),
                Codec.INT.optionalFieldOf("maxV", -1).forGetter(SavedSurface::maxV)
        ).apply(instance, SavedSurface::new));

        public Direction getFace() {
            return Direction.byName(faceName);
        }

        /** Patch ID が指定されているか */
        public boolean hasPatchId() {
            return plane >= 0 && minU >= 0 && minV >= 0 && maxU >= 0 && maxV >= 0;
        }

        /** Patch ID を復元 */
        public InkSurfacePatchId getPatchId() {
            if (hasPatchId()) {
                Direction normal = getFace();
                if (normal == null) return null;
                return new InkSurfacePatchId(normal, plane, minU, minV, maxU, maxV);
            }
            // 旧データ: faceName から fullFace を生成
            Direction normal = getFace();
            if (normal == null) return null;
            return InkSurfacePatchId.fullFace(normal);
        }
    }

    /**
     * 保存用: 1アリーナ分のインクデータ。
     */
    public record SavedArenaInk(UUID arenaId, List<SavedSurface> surfaces) {
        public static final Codec<SavedArenaInk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("arenaId").forGetter(SavedArenaInk::arenaId),
                Codec.list(SavedSurface.CODEC).fieldOf("surfaces").forGetter(SavedArenaInk::surfaces)
        ).apply(instance, SavedArenaInk::new));
    }

    /**
     * 保存用リスト ↔ 内部 Map の変換 Codec。
     */
    public static final Codec<Map<UUID, Map<InkSurfaceKey, InkFaceData>>> STORAGE_CODEC =
            Codec.list(SavedArenaInk.CODEC).xmap(
                    InkStorage::fromSavedList,
                    InkStorage::toSavedList
            );

    private static Map<UUID, Map<InkSurfaceKey, InkFaceData>> fromSavedList(List<SavedArenaInk> list) {
        Map<UUID, Map<InkSurfaceKey, InkFaceData>> result = new HashMap<>();
        for (SavedArenaInk saved : list) {
            Map<InkSurfaceKey, InkFaceData> surfaces = new HashMap<>();
            for (SavedSurface ss : saved.surfaces()) {
                InkSurfacePatchId patchId = ss.getPatchId();
                if (patchId == null) {
                    LOGGER.warn("Invalid direction '{}' for surface {} in arena {}, skipping",
                            ss.faceName(), ss.blockPos(), saved.arenaId());
                    continue;
                }
                byte[] cells = ss.cells();
                if (cells.length != InkFaceData.CELL_COUNT) {
                    // 旧8×8データ（64バイト）を16×16（256バイト）にアップスケール
                    if (cells.length == 64) {
                        cells = upscaleFrom8x8(cells);
                        LOGGER.info("Upscaled 8x8 cells to 16x16 for surface {} / {} in arena {}",
                                ss.blockPos(), ss.faceName(), saved.arenaId());
                    } else {
                        LOGGER.warn("Invalid cell count {} for surface {} / {} in arena {}, skipping",
                                cells.length, ss.blockPos(), ss.faceName(), saved.arenaId());
                        continue;
                    }
                }
                surfaces.put(new InkSurfaceKey(ss.blockPos(), patchId),
                        new InkFaceData(cells));
            }
            if (!surfaces.isEmpty()) {
                result.put(saved.arenaId(), surfaces);
            }
        }
        LOGGER.info("Loaded {} arena ink data sets", result.size());
        return result;
    }

    /**
     * 旧8×8（64バイト）のセルデータを16×16（256バイト）にアップスケールする。
     * 各旧セルを2×2の新セルに拡大する。
     */
    private static byte[] upscaleFrom8x8(byte[] oldCells) {
        final int OLD_GRID = 8;
        final int NEW_GRID = 16;
        byte[] newCells = new byte[NEW_GRID * NEW_GRID];
        for (int ov = 0; ov < OLD_GRID; ov++) {
            for (int ou = 0; ou < OLD_GRID; ou++) {
                byte team = oldCells[ov * OLD_GRID + ou];
                // 1つの旧セルを2×2=4つの新セルに拡大
                int nvStart = ov * 2;
                int nuStart = ou * 2;
                for (int dv = 0; dv < 2; dv++) {
                    for (int du = 0; du < 2; du++) {
                        newCells[(nvStart + dv) * NEW_GRID + (nuStart + du)] = team;
                    }
                }
            }
        }
        return newCells;
    }

    private static List<SavedArenaInk> toSavedList(Map<UUID, Map<InkSurfaceKey, InkFaceData>> data) {
        List<SavedArenaInk> list = new ArrayList<>();
        for (var entry : data.entrySet()) {
            List<SavedSurface> surfaces = new ArrayList<>();
            for (var se : entry.getValue().entrySet()) {
                InkSurfacePatchId pid = se.getKey().patchId();
                surfaces.add(new SavedSurface(
                        se.getKey().blockPos(),
                        pid.normal().getSerializedName(),
                        se.getValue().copyCells(),
                        pid.plane(),
                        pid.minU(),
                        pid.minV(),
                        pid.maxU(),
                        pid.maxV()
                ));
            }
            list.add(new SavedArenaInk(entry.getKey(), surfaces));
        }
        return list;
    }

    /**
     * 内部データを保存用 Map としてエクスポートする。
     */
    public Map<UUID, Map<InkSurfaceKey, InkFaceData>> exportData() {
        Map<UUID, Map<InkSurfaceKey, InkFaceData>> copy = new HashMap<>();
        for (var entry : arenaInks.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * 保存データをインポートする（既存データにマージする）。
     * ディメンションごとにロードされるため、全消去せずに追記する。
     * 同じarenaIdのデータは上書きされる。
     */
    public void importData(Map<UUID, Map<InkSurfaceKey, InkFaceData>> data) {
        for (var entry : data.entrySet()) {
            arenaInks.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
    }
}
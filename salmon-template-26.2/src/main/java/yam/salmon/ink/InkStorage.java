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

    /** デフォルトの塗装半径（8×8グリッドで直径約4セル） */
    public static final double DEFAULT_PAINT_RADIUS = 0.24;

    /** アリーナUUID → 塗装面マップ */
    private final Map<UUID, Map<InkSurfaceKey, InkFaceData>> arenaInks = new HashMap<>();

    /**
     * 塗装操作を実行する。
     *
     * <p>事前条件として {@link InkPaintability#checkPaintable} が通過していることを想定。
     * 追加でチーム値の検証も行う。</p>
     *
     * @param level    サーバーワールド
     * @param arena    対象アリーナ
     * @param blockPos 対象ブロック座標
     * @param face     対象面
     * @param coords   ヒット座標から計算された面座標情報
     * @param radius   塗装半径
     * @param team     チーム値
     * @return 塗装結果
     */
    public PaintResult paint(ServerLevel level, InkArena arena,
                              BlockPos blockPos, Direction face,
                              InkFaceCoordinates coords,
                              double radius, byte team) {
        // チーム値検証
        if (!InkTeam.isValidTeam(team)) {
            return PaintResult.fail(PaintFailureReason.INVALID_TEAM);
        }

        // 再検証: アリーナ内か
        if (!arena.contains(blockPos)) {
            return PaintResult.fail(PaintFailureReason.OUTSIDE_ARENA);
        }

        // 再検証: 塗装可能ブロックか
        if (!InkPaintability.isPaintableBlock(level.getBlockState(blockPos))) {
            return PaintResult.fail(PaintFailureReason.NOT_PAINTABLE_BLOCK);
        }

        // 再検証: 面が露出しているか
        if (!InkPaintability.isSurfaceExposed(level, blockPos, face)) {
            return PaintResult.fail(PaintFailureReason.FACE_OCCLUDED);
        }

        InkSurfaceKey key = new InkSurfaceKey(blockPos, face);
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.computeIfAbsent(
                arena.getArenaId(), k -> new HashMap<>());

        InkFaceData faceData = surfaces.computeIfAbsent(key, k -> new InkFaceData());

        int changed = faceData.paintCircle(coords.u(), coords.v(), radius, team);

        if (changed == 0) {
            return PaintResult.noChange(arena.getArenaNumber(), blockPos, face, coords, team);
        }

        // 面が完全に未塗装に戻ったら削除（paintCircle はセルを NONE にしないので現状は起こらないが将来対応）
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
     * 指定アリーナの指定面データを取得する。
     */
    public Optional<InkFaceData> getFace(InkArena arena, BlockPos blockPos, Direction face) {
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.get(arena.getArenaId());
        if (surfaces == null) return Optional.empty();
        return Optional.ofNullable(surfaces.get(new InkSurfaceKey(blockPos, face)));
    }

    /**
     * 面データがない場合に空の InkFaceData 表現を返す（inspect用）。
     */
    public InkFaceData getFaceOrEmpty(InkArena arena, BlockPos blockPos, Direction face) {
        return getFace(arena, blockPos, face).orElse(new InkFaceData());
    }

    /**
     * アリーナの全インクデータを消去する。
     *
     * @return 削除された面数
     */
    public int clearArena(InkArena arena) {
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.remove(arena.getArenaId());
        if (surfaces == null) {
            return 0;
        }
        int count = surfaces.size();
        surfaces.clear();
        LOGGER.info("Ink cleared: arena #{} / {} surfaces removed",
                arena.getArenaNumber(), count);
        return count;
    }

    /**
     * アリーナの全インクデータを削除する（アリーナ削除時の後始末）。
     *
     * @return 削除された面数
     */
    public int removeArena(InkArena arena) {
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.remove(arena.getArenaId());
        if (surfaces == null) {
            return 0;
        }
        int count = surfaces.size();
        surfaces.clear();
        LOGGER.info("Ink removed with arena: arena #{} / {} surfaces",
                arena.getArenaNumber(), count);
        return count;
    }

    /**
     * 指定アリーナの塗装面数を返す（デバッグ/表示用）。
     */
    public int getSurfaceCount(InkArena arena) {
        Map<InkSurfaceKey, InkFaceData> surfaces = arenaInks.get(arena.getArenaId());
        return surfaces != null ? surfaces.size() : 0;
    }

    /**
     * 内部データマップの直接参照を返す（InkPaintDistributor 用）。
     * 呼び出し元はスレッドセーフに注意すること（メインスレッドからのみアクセス）。
     */
    public Map<UUID, Map<InkSurfaceKey, InkFaceData>> getRawArenaMap() {
        return arenaInks;
    }

    /**
     * 内部データが変更されたかを返す（SavedData dirty判定用）。
     */
    public boolean hasData() {
        return !arenaInks.isEmpty();
    }

    /**
     * 全データをクリアする（サーバー停止時など）。
     */
    public void clearAll() {
        arenaInks.clear();
    }

    // ========================================================================
    // 保存用データ構造と Codec
    // ========================================================================

    /**
     * 保存用: 1面のデータ。
     */
    public record SavedSurface(BlockPos blockPos, String faceName, byte[] cells) {
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
                CELLS_CODEC.fieldOf("cells").forGetter(SavedSurface::cells)
        ).apply(instance, SavedSurface::new));

        /**
         * faceName から Direction を取得。不正な名前の場合は null。
         */
        public Direction getFace() {
            return Direction.byName(faceName);
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
                Direction face = ss.getFace();
                if (face == null) {
                    LOGGER.warn("Invalid direction '{}' for surface {} in arena {}, skipping",
                            ss.faceName(), ss.blockPos(), saved.arenaId());
                    continue;
                }
                if (ss.cells().length != InkFaceData.CELL_COUNT) {
                    LOGGER.warn("Invalid cell count {} for surface {} / {} in arena {}, skipping",
                            ss.cells().length, ss.blockPos(), ss.faceName(), saved.arenaId());
                    continue;
                }
                surfaces.put(new InkSurfaceKey(ss.blockPos(), face),
                        new InkFaceData(ss.cells()));
            }
            if (!surfaces.isEmpty()) {
                result.put(saved.arenaId(), surfaces);
            }
        }
        LOGGER.info("Loaded {} arena ink data sets", result.size());
        return result;
    }

    private static List<SavedArenaInk> toSavedList(Map<UUID, Map<InkSurfaceKey, InkFaceData>> data) {
        List<SavedArenaInk> list = new ArrayList<>();
        for (var entry : data.entrySet()) {
            List<SavedSurface> surfaces = new ArrayList<>();
            for (var se : entry.getValue().entrySet()) {
                surfaces.add(new SavedSurface(
                        se.getKey().blockPos(),
                        se.getKey().face().getSerializedName(),
                        se.getValue().copyCells()
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
     * 保存データをインポートする。
     */
    public void importData(Map<UUID, Map<InkSurfaceKey, InkFaceData>> data) {
        arenaInks.clear();
        for (var entry : data.entrySet()) {
            arenaInks.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
    }
}
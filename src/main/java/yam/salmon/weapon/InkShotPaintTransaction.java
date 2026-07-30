package yam.salmon.weapon;

import net.minecraft.server.level.ServerLevel;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.ink.InkPaintAccumulator;
import yam.salmon.network.InkSyncManager;

import java.util.*;

/**
 * 1発の射撃における塗装変更をアリーナごとに集約するトランザクション。
 *
 * <p>主弾命中とトレイル滴が異なるアリーナに落ちる場合を安全に扱うため、
 * アリーナUUIDをキーとして InkPaintAccumulator を保持する。</p>
 *
 * <p>コミット時に変更があるアリーナだけ保存・同期を実行する。</p>
 */
public final class InkShotPaintTransaction {

    private final Map<UUID, Entry> accumulators = new LinkedHashMap<>();

    private record Entry(InkArena arena, InkPaintAccumulator accumulator) {}

    /**
     * 指定アリーナのアキュムレータを取得（なければ生成）。
     *
     * @param arena 対象アリーナ
     * @return アキュムレータ
     */
    public InkPaintAccumulator forArena(InkArena arena) {
        UUID id = arena.getArenaId();
        Entry existing = accumulators.get(id);
        if (existing != null) return existing.accumulator();
        Entry entry = new Entry(arena, new InkPaintAccumulator());
        accumulators.put(id, entry);
        return entry.accumulator();
    }

    /**
     * このトランザクションに1件以上の変更があるか。
     */
    public boolean hasAnyChanges() {
        for (var entry : accumulators.values()) {
            if (!entry.accumulator().isEmpty()) return true;
        }
        return false;
    }

    /**
     * すべての変更をコミットする。
     * 変更があるアリーナごとに保存と同期を実行する。
     *
     * @param level サーバーレベル
     */
    public void commitAll(ServerLevel level) {
        InkArenaManager arenaManager = InkArenaManager.getInstance();
        InkSyncManager syncManager = InkSyncManager.getInstance();

        for (var entry : accumulators.values()) {
            InkPaintAccumulator acc = entry.accumulator();
            if (acc.isEmpty()) continue;

            InkArena arena = entry.arena();

            // アリーナがまだ存在するか確認
            if (!arenaManager.arenaExists(arena.getArenaId())) continue;

            // 保存
            arenaManager.saveInkDataNow(level);

            // 同期（1 revision + 複数面一括送信）
            syncManager.broadcastMultiFaceUpdate(level, arena, acc.getUpdatedSurfaces());
        }
    }

    /**
     * 変更があったアリーナUUIDの一覧を返す。
     */
    public Set<UUID> affectedArenaIds() {
        Set<UUID> ids = new LinkedHashSet<>();
        for (var entry : accumulators.entrySet()) {
            if (!entry.getValue().accumulator().isEmpty()) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    /**
     * トランザクション内の変更面の総数を返す。
     */
    public int totalChangedSurfaces() {
        int total = 0;
        for (var entry : accumulators.values()) {
            total += entry.accumulator().changedSurfaceCount();
        }
        return total;
    }

    /**
     * トランザクション内の変更セルの総数を返す。
     */
    public int totalChangedCells() {
        int total = 0;
        for (var entry : accumulators.values()) {
            total += entry.accumulator().changedCellCount();
        }
        return total;
    }
}
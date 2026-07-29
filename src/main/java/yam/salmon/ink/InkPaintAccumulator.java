package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 塗装変更をバッチ集約するアキュムレータ。
 *
 * <p>主弾の命中塗装とトレイル滴の塗装を同一トランザクションにまとめ、
 * revision 1回 + バッチ同期 1回 でコミットする。</p>
 *
 * <p>スレッドセーフではない。1回の射撃処理内で使用後は破棄する。</p>
 */
public final class InkPaintAccumulator {
    private final List<MultiSurfacePaintResult.UpdatedInkSurface> updatedSurfaces = new ArrayList<>();
    private final Set<String> dedupKeys = new HashSet<>();
    private int totalChangedCells = 0;

    /** 重複排除用のキーを生成する。BlockPos + PatchId + cellIndex を含む。 */
    private static String dedupKey(BlockPos pos, InkSurfacePatchId patchId, int cellIndex) {
        return pos.toShortString() + "@" + patchId.normal().getSerializedName()
                + "/" + patchId.plane() + "/" + patchId.minU() + "/" + patchId.minV()
                + "/" + patchId.maxU() + "/" + patchId.maxV()
                + "#" + cellIndex;
    }

    /**
     * 変更された面情報を追加する。内部で重複排除を行う。
     *
     * @param updatedSurface 変更された面の情報
     * @param surfaceKey     サーフェスキー
     * @param changedCellIndices この面で変更されたセルインデックス（0-63）
     */
    public void addSurface(
            MultiSurfacePaintResult.UpdatedInkSurface updatedSurface,
            InkSurfaceKey surfaceKey,
            int[] changedCellIndices) {
        // cell単位の重複排除：同一発射内で同じcellへ複数回書き込んでも1回だけカウント
        int uniqueChanges = 0;
        for (int ci : changedCellIndices) {
            String key = dedupKey(surfaceKey.blockPos(), surfaceKey.patchId(), ci);
            if (dedupKeys.add(key)) {
                uniqueChanges++;
            }
        }

        if (uniqueChanges > 0) {
            updatedSurfaces.add(updatedSurface);
            totalChangedCells += uniqueChanges;
        }
    }

    /**
     * 変更された面情報を追加する（重複排除なし・呼び出し元で管理）。
     */
    public void addSurfaceRaw(MultiSurfacePaintResult.UpdatedInkSurface surface) {
        if (surface.changedCells() > 0) {
            updatedSurfaces.add(surface);
            totalChangedCells += surface.changedCells();
        }
    }

    /**
     * 別の PaintResult から変更面を一括追加する。
     */
    public void addAllFrom(MultiSurfacePaintResult result) {
        if (result.success()) {
            for (var s : result.updatedSurfaces()) {
                addSurfaceRaw(s);
            }
        }
    }

    /**
     * Accumulator が空かどうかを返す。
     */
    public boolean isEmpty() {
        return updatedSurfaces.isEmpty();
    }

    /**
     * 更新された面のリストを返す（読み取り専用）。
     */
    public List<MultiSurfacePaintResult.UpdatedInkSurface> getUpdatedSurfaces() {
        return Collections.unmodifiableList(updatedSurfaces);
    }

    /**
     * 変更されたサーフェス数を返す。
     */
    public int changedSurfaceCount() {
        return updatedSurfaces.size();
    }

    /**
     * 変更されたセルの総数を返す。
     */
    public int changedCellCount() {
        return totalChangedCells;
    }
}
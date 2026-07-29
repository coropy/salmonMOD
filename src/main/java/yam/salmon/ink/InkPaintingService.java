package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.network.InkSyncManager;

/**
 * インク塗装の共通サービス。
 *
 * <p>クリック塗装（{@link yam.salmon.block.InkableBlock#useWithoutItem}）と
 * シューター着弾塗装の両方から呼び出される単一の塗装エントリポイント。</p>
 *
 * <p>内部では {@link InkPaintDistributor#distributePaint} を呼び出し、
 * データ保存とクライアント同期までを一貫して実行する。</p>
 */
public final class InkPaintingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".ink");

    private InkPaintingService() {}

    /**
     * ワールドヒット座標を中心とする3D球ブラシでインク塗装を実行する。
     *
     * <p>塗装結果に応じて SavedData の保存とクライアント同期を行う。</p>
     *
     * @param level        サーバーレベル
     * @param arena        対象アリーナ
     * @param inkStorage   インクストレージ
     * @param hitBlockPos  ヒットしたブロック座標
     * @param hitFace      ヒットした面の方向
     * @param worldHitPos  ワールド座標でのヒット位置
     * @param radiusBlocks 塗装半径（ブロック単位）
     * @param team         チーム値
     * @return 塗装分配結果
     */
    public static MultiSurfacePaintResult paint(
            ServerLevel level,
            InkArena arena,
            InkStorage inkStorage,
            BlockPos hitBlockPos,
            Direction hitFace,
            Vec3 worldHitPos,
            double radiusBlocks,
            byte team) {

        // 塗装分配
        MultiSurfacePaintResult result = InkPaintDistributor.distributePaint(
                level, arena, inkStorage,
                hitBlockPos, hitFace, worldHitPos,
                radiusBlocks, team);

        if (result.success()) {
            // SavedData を保存
            InkArenaManager.getInstance().saveInkDataNow(level);

            // クライアントへ同期
            InkSyncManager.getInstance().broadcastMultiFaceUpdate(
                    level, arena, result.updatedSurfaces());
        }

        return result;
    }

    /**
     * 塗装を実行し、結果をアキュムレータに追加する（同期は行わない）。
     *
     * <p>トレイル滴のように発射単位でバッチにまとめる場合に使用する。
     * 呼び出し元で accumulator の変更後に
     * {@link InkSyncManager#commitAccumulator} を呼ぶこと。</p>
     *
     * @param level        サーバーレベル
     * @param arena        対象アリーナ
     * @param inkStorage   インクストレージ
     * @param hitBlockPos  ヒットしたブロック座標
     * @param hitFace      ヒットした面の方向
     * @param worldHitPos  ワールド座標でのヒット位置
     * @param radiusBlocks 塗装半径（ブロック単位）
     * @param team         チーム値
     * @param accumulator  集約先アキュムレータ
     * @return 塗装分配結果
     */
    public static MultiSurfacePaintResult paintInto(
            ServerLevel level,
            InkArena arena,
            InkStorage inkStorage,
            BlockPos hitBlockPos,
            Direction hitFace,
            Vec3 worldHitPos,
            double radiusBlocks,
            byte team,
            InkPaintAccumulator accumulator) {

        MultiSurfacePaintResult result = InkPaintDistributor.distributePaint(
                level, arena, inkStorage,
                hitBlockPos, hitFace, worldHitPos,
                radiusBlocks, team);

        if (result.success() && accumulator != null) {
            accumulator.addAllFrom(result);
        }

        return result;
    }
}

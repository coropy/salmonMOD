package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import yam.salmon.block.ModBlockTags;

/**
 * 塗装可能判定の中核サービス。
 *
 * <p>ブロック全体および面単位の塗装可否を判定する。
 * クリック塗装、シューター着弾塗装、面伝播（平行面・90度面）の
 * すべてでこのサービスを単一の判定ポイントとして使用する。</p>
 *
 * <h3>判定優先順位</h3>
 * <ol>
 *   <li>{@code salmon:ink_unpaintable} タグ → 常に塗装不可</li>
 *   <li>{@code salmon:ink_paintable} タグ → 常に塗装可能</li>
 *   <li>デフォルト判定（形状・BlockEntity・液体・可視性等のヒューリスティック）</li>
 * </ol>
 *
 * <p>将来の拡張: ブロック単位の明示設定（GUI/コマンド）、
 * {@link InkPaintRuleProvider} による外部ルール解決。</p>
 *
 * <p><b>すべてサーバー側で判定する。クライアント側では呼び出さないこと。</b></p>
 */
public final class InkPaintabilityService {

    private InkPaintabilityService() {}

    // ───── タグ定数 ─────

    /** 塗装可能ブロックのタグ (salmon:ink_paintable) */
    public static final TagKey<Block> TAG_PAINTABLE = ModBlockTags.INK_PAINTABLE;

    /** 塗装不可ブロックのタグ (salmon:ink_unpaintable) */
    public static final TagKey<Block> TAG_UNPAINTABLE = ModBlockTags.INK_UNPAINTABLE;

    // ───── 公開判定メソッド ─────

    /**
     * ブロック全体が塗装対象かどうかを判定する。
     *
     * <p>判定順序:
     * <ol>
     *   <li>ink_unpaintable タグ → false</li>
     *   <li>ink_paintable タグ → true</li>
     *   <li>デフォルト形状判定</li>
     * </ol>
     *
     * @param level    サーバーレベル
     * @param blockPos ブロック座標
     * @param state    ブロック状態
     * @return 塗装可能なら true
     */
    public static boolean canPaintBlock(ServerLevel level, BlockPos blockPos, BlockState state) {
        // 1. 明示的拒否: ink_unpaintable タグ → 常に不可
        if (state.is(TAG_UNPAINTABLE)) {
            return false;
        }

        // 2. 明示的許可: ink_paintable タグ → 常に可
        if (state.is(TAG_PAINTABLE)) {
            return true;
        }

        // 3. デフォルト判定
        return isDefaultPaintable(level, blockPos, state);
    }

    /**
     * 指定面が塗装可能かどうかを判定する（ブロック全体判定 + 面露出判定）。
     *
     * <p>内部で {@link #canPaintBlock(ServerLevel, BlockPos, BlockState)} と
     * {@link InkPaintability#isSurfaceExposed(ServerLevel, BlockPos, Direction)}
     * の両方をチェックする。</p>
     *
     * @param level    サーバーレベル
     * @param blockPos ブロック座標
     * @param state    ブロック状態
     * @param face     対象面
     * @return 塗装可能なら true
     */
    public static boolean canPaintFace(ServerLevel level, BlockPos blockPos,
                                       BlockState state, Direction face) {
        if (!canPaintBlock(level, blockPos, state)) {
            return false;
        }
        return InkPaintability.isSurfaceExposed(level, blockPos, face);
    }

    // ───── デフォルト判定ロジック ─────

    /**
     * デフォルトの形状ベース判定。
     *
     * <p>原則として通常のフルキューブブロックを塗装可能とする。
     * 以下のいずれかに該当する場合は塗装不可:</p>
     * <ul>
     *   <li>空気</li>
     *   <li>液体を含む</li>
     *   <li>BlockEntity を持つ</li>
     *   <li>衝突形状がフルブロックでない（階段・ハーフブロック・フェンス等）</li>
     *   <li>occlusion しない（ガラス・葉等の透明/半透明ブロック）</li>
     *   <li>solid render でない（不可視ブロック・特殊レンダー）</li>
     * </ul>
     *
     * <p>これらの条件は、現在のインクシステムが前提とする
     * 「ブロックの各面が完全な8×8正方形である」制約と整合する。</p>
     */
    private static boolean isDefaultPaintable(ServerLevel level, BlockPos blockPos, BlockState state) {
        // 空気は不可
        if (state.isAir()) {
            return false;
        }

        // 液体を含むブロックは不可（水・溶岩）
        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        // BlockEntity を持つブロックは不可（チェスト・かまど・看板等）
        // ただし、将来的に設定で上書き可能にする余地を残す
        if (state.hasBlockEntity()) {
            return false;
        }

        // 衝突形状がフルブロックでない → 不可
        // （階段・ハーフブロック・フェンス・壁・フェンスゲート等を除外）
        if (!state.isCollisionShapeFullBlock(level, blockPos)) {
            return false;
        }

        // occlusion しないブロックは不可
        // （ガラス・葉・氷等の透明/半透明ブロック、ポータル等を除外）
        if (!state.canOcclude()) {
            return false;
        }

        // solid render でないブロックは不可
        // （看板・レッドストーンワイヤー・ボタン等の非立方体レンダーブロックを除外）
        if (!state.isSolidRender()) {
            return false;
        }

        return true;
    }
}
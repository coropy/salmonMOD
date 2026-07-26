package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.block.ModBlockTags;

import java.util.Optional;

/**
 * ブロックの塗装可能判定を集約するユーティリティクラス。
 *
 * <p>判定順序:
 * <ol>
 *   <li>対象ブロックが salmon:ink_paintable タグに含まれているか</li>
 *   <li>対象座標が登録済みインクアリーナ内にあるか</li>
 *   <li>指定面が隣接ブロックによって完全に塞がれていないか</li>
 * </ol>
 *
 * <p>すべてサーバー側で判定する。クライアント側では呼び出さないこと。</p>
 */
public final class InkPaintability {

    private InkPaintability() {}

    /**
     * 指定されたブロック座標・面が塗装可能かどうかを判定する。
     *
     * @param level サーバーワールド
     * @param blockPos 対象ブロック座標
     * @param face 塗装対象面
     * @return 塗装可能判定結果
     */
    public static PaintabilityResult checkPaintable(ServerLevel level, BlockPos blockPos, Direction face) {
        BlockState state = level.getBlockState(blockPos);

        // 1. タグチェック: salmon:ink_paintable に含まれるか
        if (!isPaintableBlock(state)) {
            return PaintabilityResult.fail(PaintabilityFailureReason.NOT_PAINTABLE_BLOCK);
        }

        // 2. アリーナ内判定
        Optional<InkArena> arena = findPaintableArena(level, blockPos);
        if (arena.isEmpty()) {
            return PaintabilityResult.fail(PaintabilityFailureReason.OUTSIDE_ARENA);
        }

        // 3. 面の露出判定
        if (!isSurfaceExposed(level, blockPos, face)) {
            return PaintabilityResult.fail(PaintabilityFailureReason.FACE_OCCLUDED);
        }

        return PaintabilityResult.success(arena.get());
    }

    /**
     * 指定された BlockState が塗装可能ブロック（salmon:ink_paintable タグ）かを判定する。
     */
    public static boolean isPaintableBlock(BlockState state) {
        return state.is(ModBlockTags.INK_PAINTABLE);
    }

    /**
     * 指定された座標・面が塗装可能な表面を持つか判定する。
     * タグ・アリーナ・面露出の3条件すべてを確認する。
     */
    public static boolean isPaintableSurface(ServerLevel level, BlockPos blockPos, Direction face) {
        return checkPaintable(level, blockPos, face).paintable();
    }

    /**
     * 指定された座標が属するインクアリーナを検索する。
     */
    public static Optional<InkArena> findPaintableArena(ServerLevel level, BlockPos blockPos) {
        return InkArenaManager.getInstance().findArenaContaining(level, blockPos);
    }

    /**
     * 指定面が隣接ブロックによって完全に塞がれていないか判定する。
     *
     * <p>MC 26.2 API:
     * <ul>
     *   <li>{@code BlockState#getOcclusionShape()} - 引数なし。ブロックのオクルージョン形状を返す</li>
     *   <li>{@code BlockState#isSolidRender()} - 引数なし。solid rendering かどうか</li>
     *   <li>{@code BlockState#canOcclude()} - 引数なし。オクルージョンするか</li>
     *   <li>{@code BlockState#isCollisionShapeFullBlock(BlockGetter, BlockPos)} - コリジョン形状がフルブロックか</li>
     *   <li>{@code BlockState#isFaceSturdy(BlockGetter, BlockPos, Direction)} - 面が sturdy か</li>
     * </ul>
     *
     * <p>初期実装では以下の場合に「露出している」と判定する:
     * <ul>
     *   <li>隣接ブロックが空気である</li>
     *   <li>隣接ブロックが置換可能（草・液体等）である</li>
     *   <li>隣接ブロックが occlusion（canOcclude）しない</li>
     *   <li>隣接ブロックの occlusion shape がフルブロック（bounds 0,0,0→1,1,1）でない</li>
     * </ul>
     *
     * <p><b>注意:</b> 部分ブロック（階段・ハーフブロック等）の厳密な面判定は将来対応。
     */
    public static boolean isSurfaceExposed(ServerLevel level, BlockPos blockPos, Direction face) {
        BlockPos adjacentPos = blockPos.relative(face);
        BlockState adjacentState = level.getBlockState(adjacentPos);

        // 空気なら露出
        if (adjacentState.isAir()) {
            return true;
        }

        // 置換可能なブロック（草・液体等）なら露出
        if (adjacentState.canBeReplaced()) {
            return true;
        }

        // occlusion しないブロック（ガラス等）は面を塞がない → 露出
        if (!adjacentState.canOcclude()) {
            return true;
        }

        // occlusion shape を取得（MC 26.2: 引数なし）
        var occlusionShape = adjacentState.getOcclusionShape();

        // occlusion shape が empty なら露出
        if (occlusionShape.isEmpty()) {
            return true;
        }

        // occlusion shape がフルブロック (0,0,0)-(1,1,1) であれば面は塞がっている
        if (isFullBlockShape(occlusionShape)) {
            return false;
        }

        // それ以外は露出とみなす（透過ブロック、部分ブロック等）
        return true;
    }

    /**
     * VoxelShape がフルブロック（bounds 0,0,0 → 1,1,1）相当かどうかを判定する。
     */
    private static boolean isFullBlockShape(net.minecraft.world.phys.shapes.VoxelShape shape) {
        // フルブロック (0,0,0)-(1,1,1) であれば全面を覆う
        return shape.min(Direction.Axis.X) <= 0.0
                && shape.max(Direction.Axis.X) >= 1.0
                && shape.min(Direction.Axis.Y) <= 0.0
                && shape.max(Direction.Axis.Y) >= 1.0
                && shape.min(Direction.Axis.Z) <= 0.0
                && shape.max(Direction.Axis.Z) >= 1.0;
    }
}
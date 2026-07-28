package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;
import yam.salmon.block.ModBlockTags;

import java.util.List;

/**
 * 塗装可能判定のユーティリティクラス。
 *
 * <p>Phase 7: 水没ブロック対応。水没している階段・ハーフブロックでも
 * 固体表面は塗装可能。VoxelShapeベースの判定に拡張。</p>
 */
public final class InkPaintability {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".ink");

    private InkPaintability() {}

    /**
     * ブロックが塗装可能か、アリーナの完全判定を含む。
     */
    public static PaintabilityResult checkPaintable(BlockGetter level, BlockPos pos,
                                                     Direction face, InkArena arena) {
        BlockState state = level.getBlockState(pos);

        if (!isPaintableBlock(level, pos, state)) {
            return PaintabilityResult.fail(PaintabilityFailureReason.NOT_PAINTABLE_BLOCK);
        }

        if (arena != null && !arena.contains(pos)) {
            return PaintabilityResult.fail(PaintabilityFailureReason.OUTSIDE_ARENA);
        }

        if (face != null && !isSurfaceExposed(level, pos, face)) {
            return PaintabilityResult.fail(PaintabilityFailureReason.FACE_OCCLUDED);
        }

        return PaintabilityResult.success(arena);
    }

    /**
     * ブロックが塗装可能か（VoxelShape ベースの一般判定）。
     *
     * <p>判定基準: InkSurfacePatchExtractor で少なくとも1つのパッチが抽出できるか。
     * これにより、フルキューブ、階段、ハーフブロックなど、VoxelShape を持つ
     * すべての固体ブロックが自動的に許可される。</p>
     *
     * <p>明示的なdeny: 空気、液体そのもの、非表示ブロック（barrier等）、
     * denyタグが付与されたブロックは除外。</p>
     */
    public static boolean isPaintableBlock(BlockGetter level, BlockPos pos, BlockState state) {
        // 空気は不可
        if (state.isAir()) return false;

        Block block = state.getBlock();

        // 液体そのものは不可
        if (isLiquidBlock(state)) return false;

        // バリアブロック等の非表示ブロックは不可
        if (block == Blocks.BARRIER || block == Blocks.STRUCTURE_VOID
                || block == Blocks.LIGHT || block == Blocks.MOVING_PISTON) {
            return false;
        }

        // deny タグがあれば不可
        if (state.is(ModBlockTags.INK_UNPAINTABLE)) return false;

        // ink_paintable タグがあれば確実に許可
        if (state.is(ModBlockTags.INK_PAINTABLE)) return true;

        // パッチ抽出ベースの判定
        try {
            List<InkSurfacePatch> patches = InkSurfacePatchExtractor.extract(state, level, pos);
            return !patches.isEmpty();
        } catch (Exception e) {
            // VoxelShape 抽出に失敗した場合は、フォールバックとして collision shape ベース
            VoxelShape shape = state.getCollisionShape(level, pos);
            return !shape.isEmpty();
        }
    }

    /**
     * ブロックが液体そのもの（水や溶岩）かどうか。
     */
    public static boolean isLiquidBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.WATER || block == Blocks.LAVA
                || block == Blocks.BUBBLE_COLUMN;
    }

    /**
     * ブロックが水没しているかどうか。
     */
    public static boolean isWaterlogged(BlockState state) {
        if (state.hasProperty(StairBlock.WATERLOGGED)) {
            return state.getValue(StairBlock.WATERLOGGED);
        }
        if (state.hasProperty(SlabBlock.WATERLOGGED)) {
            return state.getValue(SlabBlock.WATERLOGGED);
        }
        return false;
    }

    /**
     * 面が露出しているか（簡易判定：隣接ブロックの有無）。
     * Phase 7: パッチ単位の露出判定は InkPaintDistributor 側でも行う。
     */
    public static boolean isSurfaceExposed(BlockGetter level, BlockPos pos, Direction face) {
        BlockPos neighbor = pos.relative(face);
        BlockState neighborState = level.getBlockState(neighbor);

        // 隣接が空気 or 液体 → 露出
        if (neighborState.isAir() || isLiquidBlock(neighborState)) {
            return true;
        }

        // 隣接がフルブロックでなければ一部露出の可能性あり
        if (!neighborState.isCollisionShapeFullBlock(level, neighbor)) {
            return true;
        }

        // MC 26.2: getOcclusionShape() は引数なし
        VoxelShape neighborOcclusion = neighborState.getOcclusionShape();
        return neighborOcclusion.isEmpty();
    }
}
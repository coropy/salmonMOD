package yam.salmon.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;

/**
 * インク弾・トレイル滴の共通ブロックレイキャストヘルパー。
 *
 * <p>{@link ClipContext.Block#COLLIDER} を使用し、
 * 草・花などの空の衝突形状を持つブロックを自動的に通過する。</p>
 *
 * <p>主弾・トレイル滴の両方で同じ判定ロジックを使用する。</p>
 */
public final class InkCollisionRaycast {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".weapon");

    /** 空collision shapeを通過した後の再レイキャスト用epsilon */
    private static final double PASS_THROUGH_EPSILON = 5.0E-4;

    /** 連続スキップの安全上限 */
    private static final int MAX_SKIPS = 16;

    private InkCollisionRaycast() {}

    /**
     * 最初の実体衝突ブロックまでのレイキャストを実行する。
     *
     * <p>collision shapeが空のブロック（草、花など）に当たった場合は
     * それを無視し、残りの線分で再レイキャストする。</p>
     *
     * <p>液体は常に無視する（{@link ClipContext.Fluid#NONE}）。</p>
     *
     * @param level  ワールド
     * @param start  レイ開始位置
     * @param end    レイ終了位置
     * @param source 発射元Entity（自己衝突除外用、block collisionには影響しない）
     * @return 最初の実体衝突。貫通した場合はMISS
     */
    public static BlockHitResult clipSolidBlocks(Level level, Vec3 start, Vec3 end, Entity source) {
        Vec3 currentStart = start;
        Vec3 currentEnd = end;
        double totalLength = start.distanceTo(end);

        for (int skips = 0; skips < MAX_SKIPS; skips++) {
            BlockHitResult hit = level.clip(new ClipContext(
                    currentStart, currentEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    source
            ));

            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                return BlockHitResult.miss(currentEnd, null, BlockPos.containing(currentEnd));
            }

            BlockPos hitPos = hit.getBlockPos();
            BlockState hitState = level.getBlockState(hitPos);

            // 衝突形状が空のブロック（草、花など）は通過
            if (hitState.getCollisionShape(level, hitPos, CollisionContext.empty()).isEmpty()) {
                // ヒット位置から進行方向へepsilonだけ進め、残り線分で再レイキャスト
                Vec3 direction = currentEnd.subtract(currentStart).normalize();
                double remainingDist = currentEnd.distanceTo(hit.getLocation()) + PASS_THROUGH_EPSILON;
                if (remainingDist >= totalLength * 0.999 || remainingDist <= 1e-9) {
                    // 残りが極小なら通過
                    return BlockHitResult.miss(currentEnd, null, BlockPos.containing(currentEnd));
                }
                currentStart = hit.getLocation().add(direction.scale(PASS_THROUGH_EPSILON));
                // currentEnd はそのまま維持（残り線分）
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Skipping non-collidable block at {} (shape empty), remaining={:.3f}",
                            hitPos, remainingDist);
                }
                continue;
            }

            // 実体のあるブロックに命中
            return hit;
        }

        // 安全上限超え：現在位置でMISS扱い
        LOGGER.warn("Raycast skip limit exceeded after {} skips, treating as miss", MAX_SKIPS);
        return BlockHitResult.miss(currentEnd, null, BlockPos.containing(currentEnd));
    }
}
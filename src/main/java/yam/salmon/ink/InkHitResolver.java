package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ヒット位置から正しいSurface Patchを特定するレゾルバ。
 *
 * <p>入力:
 * - BlockState, BlockPos, Direction(hitFace), Vec3(worldHitPosition), BlockGetter</p>
 *
 * <p>出力: ResolvedInkSurfaceHit (surfaceKey, patch, patchLocalUV, exactSurfacePosition)</p>
 */
public final class InkHitResolver {
    /** パッチ平面距離の許容誤差（ブロック単位） */
    private static final double HIT_EPSILON = 1.0e-4;

    private InkHitResolver() {}

    /**
     * ヒット位置からターゲットパッチを解決する。
     *
     * @return 解決されたヒット情報。見つからない場合は empty。
     */
    public static Optional<ResolvedInkSurfaceHit> resolve(
            BlockState state,
            BlockPos blockPos,
            Direction hitFace,
            Vec3 worldHitPosition,
            BlockGetter level) {

        // ブロックローカル座標に変換
        double localX = worldHitPosition.x - blockPos.getX();
        double localY = worldHitPosition.y - blockPos.getY();
        double localZ = worldHitPosition.z - blockPos.getZ();

        // パッチ一覧を抽出
        List<InkSurfacePatch> patches = InkSurfacePatchExtractor.extract(state, level, blockPos);

        if (patches.isEmpty()) {
            return Optional.empty();
        }

        // hitFace と同じ normal を持つパッチを候補化
        List<InkSurfacePatch> candidates = new ArrayList<>();
        for (InkSurfacePatch patch : patches) {
            if (patch.id().normal() == hitFace) {
                candidates.add(patch);
            }
        }

        if (candidates.isEmpty()) {
            // normal が合致しなくても、最も近いパッチを試す
            candidates.addAll(patches);
        }

        // 各候補について、パッチ平面までの距離と U/V 包含を評価
        InkSurfacePatch bestPatch = null;
        double bestDist = Double.MAX_VALUE;
        double bestU = 0, bestV = 0;

        for (InkSurfacePatch patch : candidates) {
            FaceBasis basis = FaceBasis.of(patch.id().normal());

            // パッチ平面のワールド座標
            double planeCoord = patch.id().plane() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;

            // ヒット位置をパッチ平面に投影したUV
            FaceBasis.LocalUV projUV = basis.projectOntoFaceAtCoord(
                    worldHitPosition, blockPos, planeCoord);

            double blockU = projUV.u();
            double blockV = projUV.v();

            // パッチ矩形内かどうか（epsilon込み）
            double patchMinU = patch.id().minU() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
            double patchMaxU = patch.id().maxU() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
            double patchMinV = patch.id().minV() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
            double patchMaxV = patch.id().maxV() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;

            boolean inU = blockU >= patchMinU - HIT_EPSILON && blockU <= patchMaxU + HIT_EPSILON;
            boolean inV = blockV >= patchMinV - HIT_EPSILON && blockV <= patchMaxV + HIT_EPSILON;

            // 平面距離を計測
            double planeDist = basis.distanceToPatchPlane(worldHitPosition, blockPos, planeCoord);

            // 矩形内に入っていて、平面距離がepsilon以内であれば採用
            if (inU && inV && planeDist < bestDist) {
                bestDist = planeDist;
                bestPatch = patch;

                // パッチローカルUVに変換
                double patchU = (blockU - patchMinU) / (patchMaxU - patchMinU);
                double patchV = (blockV - patchMinV) / (patchMaxV - patchMinV);
                bestU = Math.clamp(patchU, 0.0, 1.0);
                bestV = Math.clamp(patchV, 0.0, 1.0);
            }
        }

        if (bestPatch == null) {
            // フォールバック: 最も平面距離が近いパッチ
            for (InkSurfacePatch patch : candidates) {
                FaceBasis basis = FaceBasis.of(patch.id().normal());
                double planeCoord = patch.id().plane() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
                double dist = basis.distanceToPatchPlane(worldHitPosition, blockPos, planeCoord);

                if (dist < bestDist) {
                    bestDist = dist;
                    bestPatch = patch;
                    FaceBasis.LocalUV projUV = basis.projectOntoFaceAtCoord(
                            worldHitPosition, blockPos, planeCoord);
                    double patchMinU = patch.id().minU() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
                    double patchMaxU = patch.id().maxU() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
                    double patchMinV = patch.id().minV() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
                    double patchMaxV = patch.id().maxV() / (double) InkSurfacePatchId.BLOCK_RESOLUTION;
                    bestU = Math.clamp((projUV.u() - patchMinU) / (patchMaxU - patchMinU), 0.0, 1.0);
                    bestV = Math.clamp((projUV.v() - patchMinV) / (patchMaxV - patchMinV), 0.0, 1.0);
                }
            }
        }

        if (bestPatch == null) {
            return Optional.empty();
        }

        InkSurfaceKey key = bestPatch.toSurfaceKey();
        Vec3 exactPos = bestPatch.toWorldPoint(bestU, bestV);

        return Optional.of(new ResolvedInkSurfaceHit(key, bestPatch, bestU, bestV, exactPos));
    }

    /**
     * 解決されたインク面ヒット情報。
     */
    public record ResolvedInkSurfaceHit(
            InkSurfaceKey surfaceKey,
            InkSurfacePatch patch,
            double patchU,
            double patchV,
            Vec3 exactSurfacePosition
    ) {}
}
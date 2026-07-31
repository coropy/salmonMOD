package yam.salmon.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 軌道シミュレーションの結果レコード。
 *
 * <p>substepごとの軌道制御点、終了位置、終了種別、
 * ブロックヒット/Entityヒットの詳細、トレイルセグメント、
 * トレイル塗装結果、終了理由、実際の飛行tick数（age）を含む。</p>
 */
public record InkTrajectoryResult(
        /** 軌道制御点リスト（視覚描画用） */
        List<Vec3> points,

        /** 軌道終了位置 */
        Vec3 endPosition,

        /** 累積飛行距離（デバッグ表示用） */
        double travelledDistance,

        /** シミュレーションしたsubstep総数 */
        int simulatedSegments,

        /** ヒット種別 */
        HitType hitType,

        /** ブロックヒット位置（BLOCK_HIT時のみ有効） */
        @Nullable BlockPos blockHitPos,

        /** ブロックヒット面（BLOCK_HIT時のみ有効） */
        @Nullable Direction blockHitFace,

        /** ブロックヒットの正確なワールド座標（BLOCK_HIT時のみ有効） */
        @Nullable Vec3 blockHitExactLocation,

        /** ヒットしたEntityのID（ENTITY_HIT時のみ有効、-1は無効） */
        int entityId,

        /** Entityヒット位置（ENTITY_HIT時のみ有効） */
        @Nullable Vec3 entityHitPosition,

        /** ダメージが適用されたか */
        boolean damaged,

        /** トレイル滴用のsubstep線分リスト */
        List<InkTrailPaintService.TrailSegment> trailSegments,

        /** トレイル塗装結果（トレイル塗装実行後に設定） */
        @Nullable InkTrailPaintService.TrailPaintResult trailPaintResult,

        /** 終了理由文字列（BLOCK_HIT, ENTITY_HIT, OUT_OF_WORLD, INVALID_PHYSICS, SAFETY_TIMEOUT） */
        @Nullable String finishReason,

        /** 実際の飛行tick数（age）。クライアント描画寿命として使用する */
        int age
) {
    public enum HitType { MISS, BLOCK_HIT, ENTITY_HIT }

    public boolean isMiss() { return hitType == HitType.MISS; }
    public boolean isBlockHit() { return hitType == HitType.BLOCK_HIT; }
    public boolean isEntityHit() { return hitType == HitType.ENTITY_HIT; }

    /**
     * 実際の飛行tick数（クライアント描画寿命）。
     * substep数からtick数を計算する。
     */
    public int actualFlightTicks(int substepsPerTick) {
        return Math.max(1, age + 1); // ageは0ベース、経過tick数=age+1
    }

    @Deprecated
    public static InkTrajectoryResult miss(List<Vec3> points, Vec3 endPosition,
                                            double travelledDistance, int simulatedSegments) {
        return new InkTrajectoryResult(points, endPosition, travelledDistance, simulatedSegments,
                HitType.MISS, null, null, null, -1, null, false, List.of(), null, null, simulatedSegments);
    }

    @Deprecated
    public static InkTrajectoryResult blockHit(List<Vec3> points, Vec3 endPosition,
                                                double travelledDistance, int simulatedSegments,
                                                BlockPos hitPos, Direction face, Vec3 hitLocation) {
        return new InkTrajectoryResult(points, endPosition, travelledDistance, simulatedSegments,
                HitType.BLOCK_HIT, hitPos, face, hitLocation, -1, null, false, List.of(), null, null, simulatedSegments);
    }

    @Deprecated
    public static InkTrajectoryResult entityHit(List<Vec3> points, Vec3 endPosition,
                                                 double travelledDistance, int simulatedSegments,
                                                 int entityId, Vec3 hitPosition, boolean damaged) {
        return new InkTrajectoryResult(points, endPosition, travelledDistance, simulatedSegments,
                HitType.ENTITY_HIT, null, null, null, entityId, hitPosition, damaged, List.of(), null, null, simulatedSegments);
    }

    public InkShotResult.Result toLegacyResult() {
        return switch (hitType) {
            case MISS -> InkShotResult.miss(endPosition);
            case BLOCK_HIT -> InkShotResult.blockHit(blockHitPos, blockHitFace, blockHitExactLocation);
            case ENTITY_HIT -> InkShotResult.entityHit(entityId, entityHitPosition, damaged);
        };
    }
}
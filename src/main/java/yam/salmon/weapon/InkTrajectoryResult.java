package yam.salmon.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record InkTrajectoryResult(
        List<Vec3> points,
        Vec3 endPosition,
        double travelledDistance,
        int simulatedSegments,
        HitType hitType,
        @Nullable BlockPos blockHitPos,
        @Nullable Direction blockHitFace,
        @Nullable Vec3 blockHitExactLocation,
        int entityId,
        @Nullable Vec3 entityHitPosition,
        boolean damaged,
        List<InkTrailPaintService.TrailSegment> trailSegments,
        @Nullable InkTrailPaintService.TrailPaintResult trailPaintResult,
        @Nullable String finishReason
) {
    public enum HitType { MISS, BLOCK_HIT, ENTITY_HIT }

    public boolean isMiss() { return hitType == HitType.MISS; }
    public boolean isBlockHit() { return hitType == HitType.BLOCK_HIT; }
    public boolean isEntityHit() { return hitType == HitType.ENTITY_HIT; }

    public static InkTrajectoryResult miss(List<Vec3> points, Vec3 endPosition,
                                            double travelledDistance, int simulatedSegments) {
        return new InkTrajectoryResult(points, endPosition, travelledDistance, simulatedSegments,
                HitType.MISS, null, null, null, -1, null, false, List.of(), null, null);
    }

    public static InkTrajectoryResult blockHit(List<Vec3> points, Vec3 endPosition,
                                                double travelledDistance, int simulatedSegments,
                                                BlockPos hitPos, Direction face, Vec3 hitLocation) {
        return new InkTrajectoryResult(points, endPosition, travelledDistance, simulatedSegments,
                HitType.BLOCK_HIT, hitPos, face, hitLocation, -1, null, false, List.of(), null, null);
    }

    public static InkTrajectoryResult entityHit(List<Vec3> points, Vec3 endPosition,
                                                 double travelledDistance, int simulatedSegments,
                                                 int entityId, Vec3 hitPosition, boolean damaged) {
        return new InkTrajectoryResult(points, endPosition, travelledDistance, simulatedSegments,
                HitType.ENTITY_HIT, null, null, null, entityId, hitPosition, damaged, List.of(), null, null);
    }

    public InkShotResult.Result toLegacyResult() {
        return switch (hitType) {
            case MISS -> InkShotResult.miss(endPosition);
            case BLOCK_HIT -> InkShotResult.blockHit(blockHitPos, blockHitFace, blockHitExactLocation);
            case ENTITY_HIT -> InkShotResult.entityHit(entityId, entityHitPosition, damaged);
        };
    }
}
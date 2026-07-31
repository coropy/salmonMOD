package yam.salmon.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 放物線軌道のシミュレーター。
 *
 * <p>初速・重力・射程・拡散のパラメータに基づき、
 * substep単位でブロックおよびEntityとの衝突を判定しながら
 * 放物線軌道をシミュレーションする。</p>
 */
public final class InkTrajectorySimulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".weapon");

    /** プレイヤー自身を弾道から除外するために視線方向に前に出す距離 */
    private static final double EYE_OFFSET = 0.3;

    /** 円形拡散を使用するか */
    private static final boolean CIRCULAR_SPREAD = true;

    private InkTrajectorySimulator() {}

    public static InkTrajectoryResult simulate(
            ServerLevel level,
            ServerPlayer shooter,
            InkWeaponConfig config) {
        RandomSource random = level.getRandom();

        Vec3 eyePos = shooter.getEyePosition();
        Vec3 lookDir = shooter.getLookAngle();
        Vec3 shootDir = applySpread(lookDir, config, random);
        Vec3 startPos = eyePos.add(shootDir.scale(EYE_OFFSET));

        Vec3 velocity = shootDir.scale(config.initialSpeed());
        Vec3 position = startPos;

        int substepsPerTick = config.trajectorySubstepsPerTick();
        int maxFlightTicks = config.maxFlightTicks();
        double maxRange = config.maxRange();
        double gravityPerSubstep = config.gravityPerTick() / substepsPerTick;

        int pointInterval = Math.max(1, substepsPerTick);
        List<Vec3> points = new ArrayList<>();
        points.add(startPos);

        double travelledDistance = 0.0;
        int simulatedSegments = 0;

        List<InkTrailPaintService.TrailSegment> trailSegments = new ArrayList<>();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Trajectory start: lookDir=({:.3f},{:.3f},{:.3f}) startPos=({:.3f},{:.3f},{:.3f}) "
                            + "initVel=({:.3f},{:.3f},{:.3f}) gravity={} maxTicks={} maxRange={}",
                    lookDir.x, lookDir.y, lookDir.z,
                    startPos.x, startPos.y, startPos.z,
                    velocity.x, velocity.y, velocity.z,
                    config.gravityPerTick(), maxFlightTicks, maxRange);
        }

        for (int tick = 0; tick < maxFlightTicks; tick++) {
            for (int sub = 0; sub < substepsPerTick; sub++) {
                simulatedSegments++;

                Vec3 previousPosition = position;
                Vec3 stepVelocity = velocity.scale(1.0 / substepsPerTick);
                position = position.add(stepVelocity);

                double stepDist = stepVelocity.length();
                double segmentStartDist = travelledDistance;
                travelledDistance += stepDist;

                // 収集: トレイル滴用のsubstep線分（常に記録、上限はトレイル側で制御）
                double segLen = previousPosition.distanceTo(position);
                if (segLen > 1e-9) {
                    trailSegments.add(new InkTrailPaintService.TrailSegment(
                            previousPosition, position, segmentStartDist));
                }

                double excessDistance = travelledDistance - maxRange;
                if (excessDistance > 0) {
                    double overstep = excessDistance / stepDist;
                    if (overstep >= 1.0) {
                        return makeMiss(points, position, travelledDistance,
                                simulatedSegments, trailSegments);
                    }
                    Vec3 segmentDir = position.subtract(previousPosition);
                    Vec3 rangeEnd = previousPosition.add(segmentDir.scale(1.0 - overstep));
                    points.add(rangeEnd);
                    return makeMiss(points, rangeEnd, maxRange,
                            simulatedSegments, trailSegments);
                }

                // 共通レイキャスト: COLLIDER基準（草・花を通過）
                BlockHitResult blockHit = InkCollisionRaycast.clipSolidBlocks(
                        level, previousPosition, position, shooter);

                AABB segmentBox = new AABB(previousPosition, position)
                        .inflate(config.collisionRadius());

                EntityHitResult entityHit = null;
                double closestEntityDist = Double.MAX_VALUE;

                for (Entity entity : level.getEntities(shooter, segmentBox,
                        e -> e.isAlive() && !e.isSpectator() && e.isAttackable())) {
                    AABB entityBox = entity.getBoundingBox().inflate(config.collisionRadius());
                    Optional<Vec3> hitOpt = entityBox.clip(previousPosition, position);
                    if (hitOpt.isPresent()) {
                        double dist = previousPosition.distanceTo(hitOpt.get());
                        if (dist < closestEntityDist) {
                            closestEntityDist = dist;
                            entityHit = new EntityHitResult(entity, hitOpt.get());
                        }
                    }
                }

                double blockDist = (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK)
                        ? previousPosition.distanceTo(blockHit.getLocation())
                        : Double.MAX_VALUE;

                if (entityHit != null && closestEntityDist < blockDist) {
                    points.add(entityHit.getLocation());
                    return makeEntityHit(points, entityHit.getLocation(),
                            travelledDistance, simulatedSegments,
                            entityHit.getEntity().getId(), entityHit.getLocation(),
                            false, trailSegments);
                }

                if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                    BlockPos hitBp = blockHit.getBlockPos();
                    Direction hitFace = blockHit.getDirection();
                    Vec3 hitLocation = blockHit.getLocation();
                    Vec3 correctedHit = correctHitPosition(hitLocation, hitFace);
                    points.add(correctedHit);
                    return makeBlockHit(points, correctedHit,
                            travelledDistance, simulatedSegments,
                            hitBp, hitFace, hitLocation, trailSegments);
                }

                if (simulatedSegments % pointInterval == 0) {
                    points.add(position);
                }
            }

            velocity = velocity.add(0.0, -config.gravityPerTick(), 0.0);
        }

        points.add(position);
        return makeMiss(points, position, travelledDistance, simulatedSegments, trailSegments);
    }

    private static InkTrajectoryResult makeMiss(List<Vec3> points, Vec3 endPos,
                                                  double dist, int segments,
                                                  List<InkTrailPaintService.TrailSegment> trail) {
        return new InkTrajectoryResult(points, endPos, dist, segments,
                InkTrajectoryResult.HitType.MISS,
                null, null, null, -1, null, false, trail, null);
    }

    private static InkTrajectoryResult makeBlockHit(List<Vec3> points, Vec3 endPos,
                                                      double dist, int segments,
                                                      BlockPos bp, Direction face, Vec3 hitLoc,
                                                      List<InkTrailPaintService.TrailSegment> trail) {
        return new InkTrajectoryResult(points, endPos, dist, segments,
                InkTrajectoryResult.HitType.BLOCK_HIT,
                bp, face, hitLoc, -1, null, false, trail, null);
    }

    private static InkTrajectoryResult makeEntityHit(List<Vec3> points, Vec3 endPos,
                                                       double dist, int segments,
                                                       int eid, Vec3 hitPos, boolean damaged,
                                                       List<InkTrailPaintService.TrailSegment> trail) {
        return new InkTrajectoryResult(points, endPos, dist, segments,
                InkTrajectoryResult.HitType.ENTITY_HIT,
                null, null, null, eid, hitPos, damaged, trail, null);
    }

    public static Vec3 applySpread(Vec3 direction, InkWeaponConfig config, RandomSource random) {
        double hSpread = config.horizontalSpreadDegrees();
        double vSpread = config.verticalSpreadDegrees();

        if (hSpread <= 0 && vSpread <= 0) {
            return direction;
        }

        Vec3 forward = direction;
        Vec3 right;
        Vec3 up;

        if (Math.abs(forward.y) > 0.999) {
            right = new Vec3(1, 0, 0).cross(forward).normalize();
            up = forward.cross(right).normalize();
        } else {
            Vec3 worldUp = new Vec3(0, 1, 0);
            right = forward.cross(worldUp).normalize();
            up = right.cross(forward).normalize();
        }

        double hRad = Math.toRadians(hSpread);
        double vRad = Math.toRadians(vSpread);

        if (CIRCULAR_SPREAD) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double radius = Math.sqrt(random.nextDouble());

            double hOffset = Math.cos(angle) * radius * Math.sin(hRad);
            double vOffset = Math.sin(angle) * radius * Math.sin(vRad);

            Vec3 perturbed = forward
                    .add(right.scale(hOffset))
                    .add(up.scale(vOffset));

            return perturbed.normalize();
        } else {
            double hOffset = (random.nextDouble() - 0.5) * 2.0 * Math.sin(hRad);
            double vOffset = (random.nextDouble() - 0.5) * 2.0 * Math.sin(vRad);

            Vec3 perturbed = forward
                    .add(right.scale(hOffset))
                    .add(up.scale(vOffset));

            return perturbed.normalize();
        }
    }

    private static Vec3 correctHitPosition(Vec3 hitPos, Direction face) {
        double offset = 0.001;
        return new Vec3(
                hitPos.x - face.getStepX() * offset,
                hitPos.y - face.getStepY() * offset,
                hitPos.z - face.getStepZ() * offset
        );
    }
}
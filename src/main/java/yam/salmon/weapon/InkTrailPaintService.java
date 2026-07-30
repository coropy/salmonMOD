package yam.salmon.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.ink.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * トレイル塗装サービス: 主弾の軌道から小さなインク滴を下方向へ落とす。
 *
 * <p>軌道substep線分からランダムなワールド距離間隔でサンプル位置を決定し、
 * 下方向レイキャストで床や段差に命中したら既存のSurface Patch塗装を呼び出す。</p>
 *
 * <p>滴ごとに着弾位置からArenaを解決し、複数Arenaに対応する。</p>
 */
public final class InkTrailPaintService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".weapon");

    private InkTrailPaintService() {}

    /**
     * トレイル滴サンプリングに使う直線セグメント。
     */
    public record TrailSegment(Vec3 start, Vec3 end, double segmentStartDistance) {}

    /**
     * ドロップの視覚情報。
     */
    public record TrailDropVisual(Vec3 start, Vec3 end, int travelTicks) {}

    /**
     * トレイル塗装の結果。
     */
    public record TrailPaintResult(
            int candidates,
            int chanceRejected,
            int rayMisses,
            int unpaintableHits,
            int successfulDrops,
            List<TrailDropVisual> visuals
    ) {
        public static final TrailPaintResult EMPTY =
                new TrailPaintResult(0, 0, 0, 0, 0, List.of());
    }

    /**
     * 軌道サブステップ線分からトレイル滴をサンプリングし、塗装と視覚情報を生成する。
     *
     * <p>滴ごとに自身の着弾BlockPosからArenaを解決する。
     * 複数Arenaへ滴が落ちる場合もtransaction内で正しく集約される。</p>
     *
     * @param level     サーバーレベル
     * @param shooter   発射者
     * @param config    武器設定
     * @param segments  軌道substep線分リスト
     * @param totalTravelled 軌道の総移動距離
     * @param shotSeed  発射ごとのシード（位置多様化用）
     * @param transaction 射撃トランザクション（蓄積先）
     * @return トレイル塗装の統計結果
     */
    public static TrailPaintResult paintTrail(
            ServerLevel level,
            ServerPlayer shooter,
            InkWeaponConfig config,
            List<TrailSegment> segments,
            double totalTravelled,
            long shotSeed,
            InkShotPaintTransaction transaction) {

        InkTrailPaintConfig trail = config.trailPaintConfig();
        if (!trail.enabled() || trail.maxTrailDropsPerShot() <= 0 || segments.isEmpty()) {
            return TrailPaintResult.EMPTY;
        }

        RandomSource random = RandomSource.create(shotSeed);

        // チーム判定
        byte team = shooter.isShiftKeyDown() ? InkTeam.TEAM_B : InkTeam.TEAM_A;

        // 最初のサンプル距離をランダム化
        double firstDropDistance = trail.minimumDistanceFromMuzzle()
                + random.nextDouble() * trail.maxTrailDropSpacing();
        double nextSampleDistance = firstDropDistance;

        // 主弾命中時の最終距離（命中位置までの距離）
        double impactDistance = totalTravelled;
        double stopBeforeImpact = impactDistance - trail.minimumDistanceFromImpact();

        int candidates = 0;
        int chanceRejected = 0;
        int rayMisses = 0;
        int unpaintableHits = 0;
        int successfulDrops = 0;
        List<TrailDropVisual> visuals = new ArrayList<>();

        for (TrailSegment seg : segments) {
            if (successfulDrops >= trail.maxTrailDropsPerShot()) break;

            double segStartDist = seg.segmentStartDistance();
            double segEndDist = segStartDist + seg.start().distanceTo(seg.end());
            if (segEndDist <= segStartDist) continue;

            // この線分内に次のサンプル距離が含まれるか
            while (nextSampleDistance >= segStartDist && nextSampleDistance < segEndDist) {
                if (successfulDrops >= trail.maxTrailDropsPerShot()) break;
                candidates++;

                // 着弾点直前は停止（Entityヒットでも、通過済み軌道まで）
                if (nextSampleDistance >= stopBeforeImpact) break;

                // 線形補間で厳密なサンプル位置を求める
                double t = (nextSampleDistance - segStartDist) / (segEndDist - segStartDist);
                t = Math.clamp(t, 0.0, 1.0);
                Vec3 samplePos = seg.start().lerp(seg.end(), t);

                // paintChance に基づく確率判定
                if (random.nextDouble() > trail.paintChance()) {
                    chanceRejected++;
                    // ランダム間隔で次へ
                    nextSampleDistance += trail.randomSpacing(random);
                    continue;
                }

                // 横方向ジッター（弾道forwardに対するright方向）
                Vec3 forward = seg.end().subtract(seg.start()).normalize();
                if (forward.lengthSqr() < 1e-9) forward = new Vec3(0, 0, 1);
                Vec3 right;
                if (Math.abs(forward.y) > 0.999) {
                    right = new Vec3(1, 0, 0).cross(forward).normalize();
                } else {
                    right = forward.cross(new Vec3(0, 1, 0)).normalize();
                }
                double jitterRight = (random.nextGaussian() * trail.horizontalJitter());
                double jitterFwd = (random.nextGaussian() * trail.horizontalJitter() * 0.5);
                Vec3 jitteredPos = samplePos.add(right.scale(jitterRight)).add(forward.scale(jitterFwd));

                // 下方向レイキャスト（常にワールド下方向）
                Vec3 rayStart = jitteredPos.add(0, trail.verticalStartOffset(), 0);
                Vec3 rayEnd = rayStart.add(0, -trail.downwardRange(), 0);

                BlockHitResult hit = level.clip(new ClipContext(
                        rayStart, rayEnd,
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        shooter
                ));

                if (hit == null || hit.getType() == HitResult.Type.MISS) {
                    rayMisses++;
                    nextSampleDistance += trail.randomSpacing(random);
                    continue;
                }

                BlockPos hitPos = hit.getBlockPos();
                Direction hitFace = hit.getDirection();
                Vec3 hitLoc = hit.getLocation();

                // 滴自身の着弾BlockPosからArenaを解決
                Optional<InkArena> arenaOpt = InkArenaManager.getInstance()
                        .findArenaContaining(level, hitPos);
                if (arenaOpt.isEmpty()) {
                    unpaintableHits++;
                    nextSampleDistance += trail.randomSpacing(random);
                    continue;
                }

                InkArena arena = arenaOpt.get();
                BlockState targetState = level.getBlockState(hitPos);

                // 塗装可能判定
                if (!InkPaintability.isPaintableBlock(level, hitPos, targetState)) {
                    unpaintableHits++;
                    nextSampleDistance += trail.randomSpacing(random);
                    continue;
                }

                // 面露出判定
                if (!InkPaintability.isSurfaceExposed(level, hitPos, hitFace)) {
                    unpaintableHits++;
                    nextSampleDistance += trail.randomSpacing(random);
                    continue;
                }

                // 塗装実行（アリーナ別トランザクションに追加、即時同期しない）
                InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();
                InkPaintAccumulator accumulator = transaction.forArena(arena);
                MultiSurfacePaintResult paintResult = InkPaintingService.paintInto(
                        level, arena, inkStorage,
                        hitPos, hitFace, hitLoc,
                        trail.paintRadius(), team,
                        accumulator);

                if (paintResult.success()) {
                    successfulDrops++;
                    // 視覚滴情報: start=samplePos, end=hitLoc
                    int dropTravelTicks = Math.max(2,
                            (int) Math.ceil(trail.downwardRange() * 2.0));
                    visuals.add(new TrailDropVisual(samplePos, hitLoc, dropTravelTicks));
                }

                // ランダム間隔で次へ
                nextSampleDistance += trail.randomSpacing(random);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Trail paint: weapon={} len={:.1f} candidates={} chanceRejected={} "
                            + "rayMisses={} unpaintable={} drops={} visuals={}",
                    config.weaponId(), totalTravelled,
                    candidates, chanceRejected, rayMisses, unpaintableHits,
                    successfulDrops, visuals.size());
        }

        return new TrailPaintResult(candidates, chanceRejected, rayMisses,
                unpaintableHits, successfulDrops, visuals);
    }
}
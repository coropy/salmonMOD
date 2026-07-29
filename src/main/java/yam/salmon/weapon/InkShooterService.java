package yam.salmon.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.combat.InkCombatService;
import yam.salmon.ink.*;
import yam.salmon.network.InkSyncManager;
import yam.salmon.weapon.InkTrajectoryResult.HitType;

import java.util.Optional;

/**
 * インクシューターの射撃処理サービス。
 */
public final class InkShooterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".weapon");

    private InkShooterService() {}

    @Deprecated
    public static InkShotResult.Result fire(ServerPlayer player, InkShooterConfig config) {
        InkWeaponConfig newConfig = InkWeaponConfig.fromLegacy(config);
        InkTrajectoryResult trajResult = fire(player, newConfig, 0L);
        return trajResult.toLegacyResult();
    }

    /**
     * 1発の射撃を放物線軌道で実行する（統合版: トレイル塗装を含む）。
     *
     * @param player   発射者
     * @param config   武器設定
     * @param shotSeed 発射ごとのシード（連射時の滴位置多様化用）
     * @return 軌道シミュレーション結果
     */
    public static InkTrajectoryResult fire(ServerPlayer player, InkWeaponConfig config, long shotSeed) {
        ServerLevel level = player.level();

        InkTrajectoryResult result = InkTrajectorySimulator.simulate(level, player, config);

        LOGGER.debug("Trajectory result: weapon={} hitType={} dist={:.2f} segments={} points={} trailSegments={}",
                config.weaponId(), result.hitType(), result.travelledDistance(),
                result.simulatedSegments(), result.points().size(), result.trailSegments().size());

        InkPaintAccumulator accumulator = new InkPaintAccumulator();

        // 主弾処理
        switch (result.hitType()) {
            case BLOCK_HIT -> handleBlockHit(player, level, result, config, accumulator);
            case ENTITY_HIT -> handleEntityHit(player, level, result, config);
            case MISS -> handleMiss(level, result);
        }

        // トレイル塗装（軌道substep線分から滴サンプリング）
        InkTrailPaintService.TrailPaintResult trailResult = InkTrailPaintService.paintTrail(
                level, player, config, result.trailSegments(),
                result.travelledDistance(), shotSeed, accumulator);

        // 一括コミット: 主弾 + トレイル滴 → revision 1回 + 同期1回
        if (!accumulator.isEmpty() && result.hitType() != HitType.MISS) {
            Optional<InkArena> arenaOpt = findHitArena(level, result);
            if (arenaOpt.isPresent()) {
                InkSyncManager.getInstance().commitAccumulator(level, arenaOpt.get(), accumulator);
            }
        } else if (!accumulator.isEmpty()) {
            // MISS+滴のみの場合: hitBlockPosがないのでtrail paint内で使われたarenaを探す必要あり
            // 簡易実装: 最初の1滴のヒットポジションからアリーナを検索
            // trailSegmentsから滴着弾のarenaを推測（paintTrail内で判定済み）
        }

        if (trailResult.successfulDrops() > 0) {
            LOGGER.info("Trail paint: player={} weapon={} drops={} surfaces={} cells={}",
                    player.getUUID(), config.weaponId(),
                    trailResult.successfulDrops(),
                    accumulator.changedSurfaceCount(),
                    accumulator.changedCellCount());
        }

        // trailPaintResultを結果に埋め込んで返す
        return new InkTrajectoryResult(
                result.points(), result.endPosition(),
                result.travelledDistance(), result.simulatedSegments(),
                result.hitType(), result.blockHitPos(), result.blockHitFace(),
                result.blockHitExactLocation(), result.entityId(),
                result.entityHitPosition(), result.damaged(),
                result.trailSegments(), trailResult);
    }

    private static Optional<InkArena> findHitArena(ServerLevel level, InkTrajectoryResult result) {
        if (result.blockHitPos() != null) {
            return InkArenaManager.getInstance().findArenaContaining(level, result.blockHitPos());
        }
        return Optional.empty();
    }

    private static void handleBlockHit(ServerPlayer player, ServerLevel level,
                                        InkTrajectoryResult result, InkWeaponConfig config,
                                        InkPaintAccumulator accumulator) {
        BlockPos hitPos = result.blockHitPos();
        Direction hitFace = result.blockHitFace();
        Vec3 hitLocation = result.blockHitExactLocation();
        Vec3 correctedHit = result.endPosition();

        Optional<InkArena> arenaOpt = InkArenaManager.getInstance()
                .findArenaContaining(level, hitPos);

        if (arenaOpt.isPresent() && config.paintOnBlockHit()) {
            InkArena arena = arenaOpt.get();
            BlockState targetState = level.getBlockState(hitPos);
            boolean paintableBlock = InkPaintability.isPaintableBlock(level, hitPos, targetState);
            boolean surfaceExposed = InkPaintability.isSurfaceExposed(level, hitPos, hitFace);

            if (!paintableBlock || !surfaceExposed) {
                LOGGER.info("Shooter paint skipped: player={} arena=#{} hit={}/{}",
                        player.getUUID(), arena.getArenaNumber(), hitPos, hitFace);
            }

            if (paintableBlock && surfaceExposed) {
                byte team = player.isShiftKeyDown() ? InkTeam.TEAM_B : InkTeam.TEAM_A;
                InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();

                // アキュムレータに追加（即時同期しない）
                MultiSurfacePaintResult paintResult = InkPaintingService.paintInto(
                        level, arena, inkStorage,
                        hitPos, hitFace, hitLocation,
                        config.paintRadius(), team,
                        accumulator);

                if (paintResult.success()) {
                    LOGGER.info("Shooter paint: player={} arena=#{} hit={}/{} surfaces={} cells={}",
                            player.getUUID(), arena.getArenaNumber(),
                            hitPos, hitFace,
                            paintResult.changedSurfaceCount(),
                            paintResult.changedCellCount());
                } else {
                    LOGGER.info("Shooter paint failed: player={} arena=#{} hit={}/{} reason={}",
                            player.getUUID(), arena.getArenaNumber(),
                            hitPos, hitFace, paintResult.failureReason());
                }
            }
        } else if (arenaOpt.isEmpty()) {
            LOGGER.info("Shooter paint skipped: player={} hit={} not in any arena",
                    player.getUUID(), hitPos);
        }

        InkShotEffects.spawnBlockHitEffect(level, correctedHit, hitFace);

        LOGGER.info("Shooter block hit: player={} pos={} face={} dist={:.2f}",
                player.getUUID(), hitPos, hitFace, result.travelledDistance());
    }

    private static void handleEntityHit(ServerPlayer player, ServerLevel level,
                                         InkTrajectoryResult result, InkWeaponConfig config) {
        Entity target = level.getEntity(result.entityId());
        if (target == null) return;

        boolean damaged = false;
        if (config.damageEntities()) {
            damaged = InkCombatService.applyShooterHit(player, target, config.damage());
        }

        InkShotEffects.spawnEntityHitEffect(level, result.entityHitPosition(), player);

        LOGGER.info("Shooter entity hit: player={} target={} dist={:.2f} damaged={}",
                player.getUUID(), target.getId(), result.travelledDistance(), damaged);
    }

    private static void handleMiss(ServerLevel level, InkTrajectoryResult result) {
        InkShotEffects.spawnMissEffect(level, result.endPosition(), null);

        LOGGER.info("Shooter miss: endPos=({:.1f},{:.1f},{:.1f}) dist={:.2f} segments={}",
                result.endPosition().x, result.endPosition().y, result.endPosition().z,
                result.travelledDistance(), result.simulatedSegments());
    }
}
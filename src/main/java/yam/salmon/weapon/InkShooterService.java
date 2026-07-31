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
     * <p>Entityヒット・MISS・主弾終点がアリーナ外でも、
     * 軌道途中でアリーナ内へ落ちたトレイル滴は塗装される。</p>
     *
     * @param player   発射者
     * @param config   武器設定
     * @param shotSeed 発射ごとのシード（連射時の滴位置多様化用）
     * @return 軌道シミュレーション結果
     */
    public static InkTrajectoryResult fire(ServerPlayer player, InkWeaponConfig config, long shotSeed) {
        ServerLevel level = player.level();

        InkTrajectoryResult result = InkTrajectorySimulator.simulate(level, player, config);

        LOGGER.debug("Trajectory result: weapon={} hitType={} dist={:.2f} segments={} points={} trailSegments={} finishReason={}",
                config.weaponId(), result.hitType(), result.travelledDistance(),
                result.simulatedSegments(), result.points().size(), result.trailSegments().size(),
                result.finishReason());

        // アリーナ別トランザクション（主弾+滴の全変更を集約）
        InkShotPaintTransaction transaction = new InkShotPaintTransaction();

        // 主弾処理（BlockHit時のみ塗装）
        switch (result.hitType()) {
            case BLOCK_HIT -> handleBlockHit(player, level, result, config, transaction);
            case ENTITY_HIT -> handleEntityHit(player, level, result, config);
            case MISS -> handleMiss(level, result);
        }

        // トレイル塗装（全ヒット種別で実行、滴ごとに独自のArenaを解決）
        InkTrailPaintService.TrailPaintResult trailResult = InkTrailPaintService.paintTrail(
                level, player, config, result.trailSegments(),
                result.travelledDistance(), shotSeed, transaction);

        // 一括コミット: 主弾 + トレイル滴 → アリーナごとにrevision 1回 + 同期1回
        if (transaction.hasAnyChanges()) {
            transaction.commitAll(level);
        }

        boolean mainPainted = result.hitType() == InkTrajectoryResult.HitType.BLOCK_HIT
                && transaction.totalChangedCells() > 0;
        boolean trailsChanged = trailResult.successfulDrops() > 0;

        if (trailsChanged || mainPainted) {
            LOGGER.info("Shot paint result:\n  mainImpact={}\n  mainPainted={}\n  trailCandidates={}\n  trailSuccessful={}\n  changedSurfaces={}\n  changedCells={}\n  affectedArenas={}",
                    result.hitType(),
                    mainPainted,
                    trailResult.candidates(),
                    trailResult.successfulDrops(),
                    transaction.totalChangedSurfaces(),
                    transaction.totalChangedCells(),
                    transaction.affectedArenaIds());
        } else if (result.hitType() == InkTrajectoryResult.HitType.BLOCK_HIT) {
            LOGGER.info("Shot paint result: mainImpact=BLOCK mainPainted=false trailSuccessful=0 (no paint change)");
        }

        // trailPaintResultを結果に埋め込んで返す
        return new InkTrajectoryResult(
                result.points(), result.endPosition(),
                result.travelledDistance(), result.simulatedSegments(),
                result.hitType(), result.blockHitPos(), result.blockHitFace(),
                result.blockHitExactLocation(), result.entityId(),
                result.entityHitPosition(), result.damaged(),
                result.trailSegments(), trailResult, result.finishReason(),
                result.age());
    }

    private static void handleBlockHit(ServerPlayer player, ServerLevel level,
                                        InkTrajectoryResult result, InkWeaponConfig config,
                                        InkShotPaintTransaction transaction) {
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

                // アリーナ別トランザクションに追加
                InkPaintAccumulator accumulator = transaction.forArena(arena);
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

        LOGGER.info("Shooter miss: endPos=({:.1f},{:.1f},{:.1f}) dist={:.2f} segments={} finishReason={}",
                result.endPosition().x, result.endPosition().y, result.endPosition().z,
                result.travelledDistance(), result.simulatedSegments(), result.finishReason());
    }
}
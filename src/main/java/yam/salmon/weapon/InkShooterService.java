package yam.salmon.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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
 *
 * <p>サーバー側でレイキャストを行い、ブロックとEntityの両方を判定し、
 * 最も近い命中対象に対して塗装またはダメージを適用する。</p>
 *
 * <p>射撃間隔の管理は {@link InkShooterItem} 側で行い、
 * このクラスは1発分の純粋な射撃計算に集中する。</p>
 */
public final class InkShooterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".weapon");

    /** プレイヤー自身をレイキャストから除外するために視線開始位置を前に出す距離 */
    private static final double EYE_OFFSET = 0.3;

    private InkShooterService() {}

    /**
     * 1発の射撃を実行する。
     *
     * @param player 発射者
     * @param config 武器設定
     * @return 射撃結果
     */
    public static InkShotResult.Result fire(ServerPlayer player, InkShooterConfig config) {
        ServerLevel level = player.level();
        RandomSource random = level.getRandom();

        // 発射位置と方向
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();

        // 拡散を適用
        Vec3 shootDir = applySpread(lookDir, config.spreadDegrees(), random);

        // 壁内発射防止: 視線方向に少し前に出す
        Vec3 startPos = eyePos.add(shootDir.scale(EYE_OFFSET));

        // 射程終端
        Vec3 endPos = startPos.add(shootDir.scale(config.range()));

        // --- ブロックレイキャスト ---
        BlockHitResult blockHit = level.clip(new ClipContext(
                startPos, endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        // --- Entityレイキャスト ---
        Vec3 blockHitPos = (blockHit != null && blockHit.getType() != HitResult.Type.MISS)
                ? blockHit.getLocation()
                : endPos;

        AABB searchBox = new AABB(startPos, endPos).inflate(1.0);
        EntityHitResult entityHit = null;
        double closestEntityDist = Double.MAX_VALUE;

        for (Entity entity : level.getEntities(player, searchBox,
                e -> e.isAlive() && !e.isSpectator() && e.isAttackable())) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hitOpt = entityBox.clip(startPos, endPos);
            if (hitOpt.isPresent()) {
                double dist = startPos.distanceTo(hitOpt.get());
                if (dist < closestEntityDist) {
                    closestEntityDist = dist;
                    entityHit = new EntityHitResult(entity, hitOpt.get());
                }
            }
        }

        // 距離を比較して近い方を最終命中対象に
        double blockDist = (blockHit != null && blockHit.getType() != HitResult.Type.MISS)
                ? startPos.distanceTo(blockHit.getLocation())
                : Double.MAX_VALUE;

        if (entityHit != null && closestEntityDist <= blockDist) {
            // Entityに命中
            Entity target = entityHit.getEntity();
            boolean damaged = InkCombatService.applyShooterHit(player, target, config.damage());

            // Entity命中エフェクト
            InkShotEffects.spawnEntityHitEffect(level, entityHit.getLocation(), player);

            LOGGER.info("Shooter entity hit: player={} target={} dist={} damaged={}",
                    player.getUUID(), target.getId(), closestEntityDist, damaged);

            return InkShotResult.entityHit(target.getId(), entityHit.getLocation(), damaged);
        }

        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            // ブロックに命中
            BlockPos hitPos = blockHit.getBlockPos();
            Direction hitFace = blockHit.getDirection();
            Vec3 hitLocation = blockHit.getLocation();

            // 着弾座標を面の内側に微小補正（レンダリング用）
            Vec3 correctedHit = correctHitPosition(hitLocation, hitFace);

            // 塗装可能ブロックか判定
            Optional<InkArena> arenaOpt = InkArenaManager.getInstance()
                    .findArenaContaining(level, hitPos);

            if (arenaOpt.isPresent()) {
                InkArena arena = arenaOpt.get();
                BlockState targetState = level.getBlockState(hitPos);
                boolean paintableBlock = InkPaintability.isPaintableBlock(level, hitPos, targetState);
                boolean surfaceExposed = InkPaintability.isSurfaceExposed(level, hitPos, hitFace);
                if (!paintableBlock || !surfaceExposed) {
                    LOGGER.info("Shooter paint skipped: player={} arena=#{} hit={}/{} block={} paintableBlock={} surfaceExposed={}",
                            player.getUUID(), arena.getArenaNumber(),
                            hitPos, hitFace, targetState.getBlock(),
                            paintableBlock, surfaceExposed);
                }
                if (paintableBlock && surfaceExposed) {
                    // 塗装実行（ブラシ中心は正確な表面座標。内側補正は交差判定側のepsilonで吸収）
                    byte team = player.isShiftKeyDown() ? InkTeam.TEAM_B : InkTeam.TEAM_A;
                    InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();

                    MultiSurfacePaintResult paintResult = InkPaintingService.paint(
                            level, arena, inkStorage,
                            hitPos, hitFace, hitLocation,
                            config.brushRadius(), team);

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
            } else {
                LOGGER.info("Shooter paint skipped: player={} hit={} not in any arena",
                        player.getUUID(), hitPos);
            }

            // ブロック命中エフェクト
            InkShotEffects.spawnBlockHitEffect(level, correctedHit, hitFace);

            LOGGER.info("Shooter block hit: player={} pos={} face={} dist={}",
                    player.getUUID(), hitPos, hitFace, blockDist);

            return InkShotResult.blockHit(hitPos, hitFace, correctedHit);
        }

        // 何にも当たらなかった
        InkShotEffects.spawnMissEffect(level, endPos, player);

        return InkShotResult.miss(endPos);
    }

    /**
     * 視線方向に拡散を適用する。
     *
     * @param direction     正規化された視線方向
     * @param spreadDegrees 拡散角度（度数法）
     * @param random        乱数ソース
     * @return 拡散後の方向ベクトル
     */
    private static Vec3 applySpread(Vec3 direction, double spreadDegrees, RandomSource random) {
        if (spreadDegrees <= 0) {
            return direction;
        }

        double spreadRad = Math.toRadians(spreadDegrees);
        double yaw = Math.atan2(direction.x, direction.z);
        double pitch = Math.asin(direction.y);

        yaw += (random.nextDouble() - 0.5) * spreadRad * 2.0;
        pitch += (random.nextDouble() - 0.5) * spreadRad * 2.0;

        // ピッチを安全な範囲にクランプ（真上/真下を超えないように）
        pitch = Mth.clamp(pitch, -Math.PI / 2.0 + 0.01, Math.PI / 2.0 - 0.01);

        double cosPitch = Math.cos(pitch);
        return new Vec3(
                Math.sin(yaw) * cosPitch,
                Math.sin(pitch),
                Math.cos(yaw) * cosPitch
        );
    }

    /**
     * 着弾座標を面の内側に微小補正する。
     * 浮動小数点誤差でブロック境界に張り付くのを防ぐ。
     */
    private static Vec3 correctHitPosition(Vec3 hitPos, Direction face) {
        double offset = 0.001;
        return new Vec3(
                hitPos.x - face.getStepX() * offset,
                hitPos.y - face.getStepY() * offset,
                hitPos.z - face.getStepZ() * offset
        );
    }
}
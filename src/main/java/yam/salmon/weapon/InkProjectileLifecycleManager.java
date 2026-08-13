package yam.salmon.weapon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.combat.InkCombatService;
import yam.salmon.ink.*;
import yam.salmon.network.InkShotImpactPayload;
import yam.salmon.network.InkShotSpawnPayload;
import yam.salmon.network.InkTrailDropImpactPayload;
import yam.salmon.network.InkTrailDropSpawnPayload;
import yam.salmon.team.TeamManager;

import java.util.*;
import java.util.function.Predicate;

/**
 * サーバー側プロジェクタイルライフサイクルマネージャー。
 *
 * <p>主弾とトレイル滴を毎サーバーtick更新する。
 * 塗装、Entityダメージ、Payload送信はすべて実際の衝突tickでのみ実行される。</p>
 *
 * <p>発射処理は {@link #spawnShot(ServerLevel, ServerPlayer, Vec3, InkWeaponConfig)} を呼ぶだけでよい。
 * その後は毎tick、{@link #tickServer(MinecraftServer)} が
 * ブロック/Entity衝突を検出し、必要な効果を適用する。</p>
 */
public final class InkProjectileLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".weapon");

    private static final InkProjectileLifecycleManager INSTANCE = new InkProjectileLifecycleManager();

    /** 視線方向に前に出す距離 */
    private static final double EYE_OFFSET = 0.3;

    /** 発射位置の右方向オフセット（プレイヤーの利き手側） */
    private static final double RIGHT_OFFSET = 0.4;

    /** 発射位置の下方向オフセット */
    private static final double DOWN_OFFSET = 0.5;

    /** ワールド外判定マージン */
    static final int OUT_OF_WORLD_MARGIN = 64;

    private final Map<UUID, ActiveInkShot> activeShots = new LinkedHashMap<>();
    private final Map<UUID, ActiveTrailDrop> activeDrops = new LinkedHashMap<>();

    /** tickループ中に追加する滴の一時キュー */
    private final List<ActiveTrailDrop> pendingDropAdds = new ArrayList<>();

    private boolean registered = false;

    private InkProjectileLifecycleManager() {}

    public static InkProjectileLifecycleManager getInstance() {
        return INSTANCE;
    }

    /**
     * Fabricのサーバーtickイベントに登録する。
     * {@link yam.salmon.Salmon#onInitialize()} から1度だけ呼ぶこと。
     */
    public void register() {
        if (registered) return;
        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(this::tickServer);

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            activeShots.clear();
            activeDrops.clear();
            pendingDropAdds.clear();
        });
    }

    // ===================================================================
    // 発射
    // ===================================================================

    /**
     * 主弾を生成し、Spawn Payloadを周囲に送信する。
     *
     * @param level   発射ワールド
     * @param shooter 発射者
     * @param config  武器設定（発射時のスナップショット）
     * @return 生成された主弾
     */
    public ActiveInkShot spawnShot(ServerLevel level, ServerPlayer shooter,
                                   InkWeaponConfig config) {
        UUID shotId = UUID.randomUUID();
        RandomSource random = RandomSource.create(level.getRandom().nextLong());

        Vec3 eyePos = shooter.getEyePosition();
        Vec3 lookDir = shooter.getLookAngle();
        Vec3 shootDir = InkTrajectorySimulator.applySpread(lookDir, config, random);
        Vec3 right = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 startPos = eyePos.add(shootDir.scale(EYE_OFFSET))
                .add(right.scale(RIGHT_OFFSET))
                .add(0, -DOWN_OFFSET, 0);
        Vec3 initialVelocity = shootDir.scale(config.initialSpeed());

        int colorRgb = InkVisualColorResolver.resolveShotColor(shooter);
        ActiveInkShot shot = new ActiveInkShot(
                shotId, shooter.getUUID(), level.dimension(),
                startPos, initialVelocity, config,
                level.getGameTime(), random, colorRgb);

        activeShots.put(shotId, shot);

        // Spawn Payloadを周囲に送信
        sendShotSpawnPayload(level, shot, shooter);

        LOGGER.debug("Shot spawned: shotId={} player={} pos=({:.2f},{:.2f},{:.2f}) vel=({:.2f},{:.2f},{:.2f})",
                shotId, shooter.getUUID(),
                startPos.x, startPos.y, startPos.z,
                initialVelocity.x, initialVelocity.y, initialVelocity.z);

        return shot;
    }

    // ===================================================================
    // サーバーtick
    // ===================================================================

    void tickServer(MinecraftServer server) {
        // 全ディメンションのアクティブ弾・滴を処理
        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dim = level.dimension();

            // 主弾
            Iterator<Map.Entry<UUID, ActiveInkShot>> shotIter = activeShots.entrySet().iterator();
            while (shotIter.hasNext()) {
                ActiveInkShot shot = shotIter.next().getValue();
                if (!shot.dimension().equals(dim)) continue;
                if (shot.isFinished()) {
                    shotIter.remove();
                    continue;
                }
                tickShot(level, shot);
                if (shot.isFinished()) {
                    shotIter.remove();
                }
            }

            // トレイル滴
            Iterator<Map.Entry<UUID, ActiveTrailDrop>> dropIter = activeDrops.entrySet().iterator();
            while (dropIter.hasNext()) {
                ActiveTrailDrop drop = dropIter.next().getValue();
                if (!drop.dimension().equals(dim)) continue;
                if (drop.isFinished()) {
                    dropIter.remove();
                    continue;
                }
                tickDrop(level, drop);
                if (drop.isFinished()) {
                    dropIter.remove();
                }
            }

            // tickループ中に追加された滴をマージ
            if (!pendingDropAdds.isEmpty()) {
                for (ActiveTrailDrop drop : pendingDropAdds) {
                    if (drop.dimension().equals(dim)) {
                        activeDrops.put(drop.dropId(), drop);
                    }
                }
                pendingDropAdds.clear();
            }
        }
    }

    // ===================================================================
    // 主弾tick
    // ===================================================================

    private void tickShot(ServerLevel level, ActiveInkShot shot) {
        Vec3 previousPosition = shot.position();

        // substepごとの移動
        Vec3 substepVelocity = shot.velocity().scale(1.0 / shot.substepsPerTick());
        Vec3 nextPosition = previousPosition;

        for (int sub = 0; sub < shot.substepsPerTick(); sub++) {
            Vec3 subPrev = nextPosition;
            nextPosition = subPrev.add(substepVelocity);

            // ブロック衝突判定
            ServerPlayer shooter = level.getServer().getPlayerList()
                    .getPlayer(shot.shooterId());
            BlockHitResult blockHit = InkCollisionRaycast.clipSolidBlocks(
                    level, subPrev, nextPosition, shooter);

            // Entity衝突判定
            EntityHitResult entityHit = findNearestEntityHit(level, subPrev, nextPosition, shot);

            // 近い方のヒットを選択
            double blockDist = (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK)
                    ? subPrev.distanceTo(blockHit.getLocation()) : Double.MAX_VALUE;
            double entityDist = (entityHit != null)
                    ? subPrev.distanceTo(entityHit.getLocation()) : Double.MAX_VALUE;

            if (entityHit != null && entityDist < blockDist) {
                finishShotAsEntityHit(level, shot, entityHit);
                return;
            }

            if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                finishShotAsBlockHit(level, shot, blockHit);
                return;
            }
        }

        // 衝突なし → 位置更新
        shot.setPosition(nextPosition);
        shot.setVelocity(shot.velocity().add(0.0, -shot.gravity(), 0.0));
        shot.incrementAge();

        // トレイル滴生成
        maybeSpawnTrailDrop(level, shot);

        // 安全チェック
        if (!isFinite(shot.position()) || !isFinite(shot.velocity())) {
            finishShotWithoutPaint(level, shot, ProjectileFinishReason.INVALID_PHYSICS);
            return;
        }

        if (shot.position().y < -256) {
            finishShotWithoutPaint(level, shot, ProjectileFinishReason.OUT_OF_WORLD);
            return;
        }

        if (shot.age() >= shot.hardSafetyMaxTicks()) {
            finishShotWithoutPaint(level, shot, ProjectileFinishReason.SAFETY_TIMEOUT);
        }
    }

    // ===================================================================
    // 主弾の終了処理
    // ===================================================================

    private void finishShotAsBlockHit(ServerLevel level, ActiveInkShot shot, BlockHitResult hit) {
        Vec3 impactPosition = hit.getLocation();
        shot.setPosition(impactPosition);
        shot.setFinishReason(ProjectileFinishReason.BLOCK_HIT);

        // 塗装（実際の衝突tickで実行）
        BlockPos hitPos = hit.getBlockPos();
        Direction hitFace = hit.getDirection();

        Optional<InkArena> arenaOpt = InkArenaManager.getInstance()
                .findArenaContaining(level, hitPos);

        if (arenaOpt.isPresent() && shot.config().paintOnBlockHit()) {
            InkArena arena = arenaOpt.get();
            BlockState targetState = level.getBlockState(hitPos);
            boolean paintableBlock = InkPaintability.isPaintableBlock(level, hitPos, targetState);
            boolean surfaceExposed = InkPaintability.isSurfaceExposed(level, hitPos, hitFace);

            if (paintableBlock && surfaceExposed) {
                byte team = TeamManager.getInstance().getTeam(shot.shooterId());

                InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();
                InkShotPaintTransaction transaction = new InkShotPaintTransaction();
                InkPaintAccumulator accumulator = transaction.forArena(arena);

                Vec3 shotVelocity = shot.velocity();
                MultiSurfacePaintResult paintResult = InkPaintingService.paintInto(
                        level, arena, inkStorage,
                        hitPos, hitFace, impactPosition,
                        shot.config().paintRadius(), team,
                        shotVelocity,
                        accumulator);

                if (transaction.hasAnyChanges()) {
                    transaction.commitAll(level);
                }

                if (paintResult.success()) {
                    LOGGER.info("Shot impact paint: shotId={} arena=#{} hit={}/{} surfaces={} cells={}",
                            shot.shotId(), arena.getArenaNumber(),
                            hitPos, hitFace,
                            paintResult.changedSurfaceCount(),
                            paintResult.changedCellCount());
                } else {
                    LOGGER.info("Shot impact paint failed: shotId={} arena=#{} hit={}/{} reason={}",
                            shot.shotId(), arena.getArenaNumber(),
                            hitPos, hitFace, paintResult.failureReason());
                }
            }
        }

        InkShotEffects.spawnBlockHitEffect(level, impactPosition, hitFace);

        // Impact Payload送信
        sendShotImpactPayload(level, shot, impactPosition, hitFace);

        shot.markFinished();

        LOGGER.info("Shot block hit: shotId={} pos={}/{} dist={:.2f} age={}",
                shot.shotId(), hitPos, hitFace,
                shot.spawnPosition().distanceTo(impactPosition), shot.age());
    }

    private void finishShotAsEntityHit(ServerLevel level, ActiveInkShot shot, EntityHitResult entityHit) {
        Entity target = entityHit.getEntity();
        Vec3 impactPosition = entityHit.getLocation();

        shot.setPosition(impactPosition);
        shot.setFinishReason(ProjectileFinishReason.ENTITY_HIT);

        // ダメージ適用（実際の衝突tickで実行）
        boolean damaged = false;
        if (shot.config().damageEntities()) {
            ServerPlayer shooter = level.getServer().getPlayerList()
                    .getPlayer(shot.shooterId());
            if (shooter != null) {
                damaged = InkCombatService.applyShooterHit(shooter, target, shot.config().damage());
            }
        }

        InkShotEffects.spawnEntityHitEffect(level, impactPosition,
                level.getServer().getPlayerList().getPlayer(shot.shooterId()));

        // Impact Payload送信
        sendShotImpactPayload(level, shot, impactPosition,
                Direction.fromYRot(target.getYRot()));

        shot.markFinished();

        LOGGER.info("Shot entity hit: shotId={} target={} damaged={} age={}",
                shot.shotId(), target.getId(), damaged, shot.age());
    }

    private void finishShotWithoutPaint(ServerLevel level, ActiveInkShot shot,
                                         ProjectileFinishReason reason) {
        shot.setFinishReason(reason);

        // Impact Payload送信（miss扱い）
        sendShotImpactPayload(level, shot, shot.position(), null);

        shot.markFinished();

        LOGGER.info("Shot finished without paint: shotId={} reason={} age={} pos=({:.1f},{:.1f},{:.1f})",
                shot.shotId(), reason, shot.age(),
                shot.position().x, shot.position().y, shot.position().z);
    }

    // ===================================================================
    // トレイル滴生成
    // ===================================================================

    private void maybeSpawnTrailDrop(ServerLevel level, ActiveInkShot shot) {
        InkTrailPaintConfig trail = shot.config().trailPaintConfig();
        if (!trail.enabled() || trail.maxTrailDropsPerShot() <= 0) return;

        int maxDrops = trail.maxTrailDropsPerShot();
        if (shot.generatedDropCount() >= maxDrops) return;

        RandomSource random = shot.random();

        // 初回のみ、マズルからの最小距離をスキップ
        if (shot.generatedDropCount() == 0 && shot.age() == 0) {
            // 最初の数tickは滴を生成しない（マズル直近のため）
            if (shot.age() < 2) return;
        }

        // 確率判定（低頻度: 1回のtickで複数生成しないよう制御）
        if (random.nextDouble() > trail.paintChance() * 0.3) return;

        // 滴生成位置: 主弾の現在位置 + ランダムジッター
        Vec3 shotPos = shot.position();

        // 横方向ジッター
        double jitterX = random.nextGaussian() * trail.horizontalJitter();
        double jitterZ = random.nextGaussian() * trail.horizontalJitter();
        Vec3 dropPos = shotPos.add(jitterX, trail.verticalStartOffset(), jitterZ);

        UUID dropId = UUID.randomUUID();
        Vec3 dropVelocity = new Vec3(
                random.nextGaussian() * 0.02,
                -0.5 - random.nextDouble() * 0.5,
                random.nextGaussian() * 0.02);

        ActiveTrailDrop drop = new ActiveTrailDrop(
                dropId, shot.shotId(), shot.shooterId(),
                level.dimension(), dropPos, dropVelocity,
                shot.gravity(), level.getGameTime(),
                trail.visualDropSize(), trail.paintRadius(),
                shot.visualColorRgb());

        pendingDropAdds.add(drop);
        shot.incrementDropCount();

        // Spawn Payload送信
        sendTrailDropSpawnPayload(level, drop);
    }

    // ===================================================================
    // トレイル滴tick
    // ===================================================================

    private void tickDrop(ServerLevel level, ActiveTrailDrop drop) {
        Vec3 previousPosition = drop.position();
        Vec3 nextPosition = previousPosition.add(drop.velocity());

        ServerPlayer shooter = level.getServer().getPlayerList()
                .getPlayer(drop.shooterId());
        BlockHitResult blockHit = InkCollisionRaycast.clipSolidBlocks(
                level, previousPosition, nextPosition, shooter);

        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            finishDropAsBlockHit(level, drop, blockHit);
            return;
        }

        drop.setPosition(nextPosition);
        drop.setVelocity(drop.velocity().add(0.0, -drop.gravity(), 0.0));
        drop.incrementAge();

        if (!isFinite(drop.position()) || !isFinite(drop.velocity())) {
            finishDropWithoutPaint(level, drop, ProjectileFinishReason.INVALID_PHYSICS);
            return;
        }

        if (drop.position().y < -256) {
            finishDropWithoutPaint(level, drop, ProjectileFinishReason.OUT_OF_WORLD);
            return;
        }

        if (drop.age() >= ActiveTrailDrop.HARD_DROP_SAFETY_TICKS) {
            finishDropWithoutPaint(level, drop, ProjectileFinishReason.SAFETY_TIMEOUT);
        }
    }

    private void finishDropAsBlockHit(ServerLevel level, ActiveTrailDrop drop, BlockHitResult hit) {
        Vec3 impactPosition = hit.getLocation();
        drop.setPosition(impactPosition);
        drop.setFinishReason(ProjectileFinishReason.BLOCK_HIT);

        BlockPos hitPos = hit.getBlockPos();
        Direction hitFace = hit.getDirection();

        Optional<InkArena> arenaOpt = InkArenaManager.getInstance()
                .findArenaContaining(level, hitPos);

        if (arenaOpt.isPresent()) {
            InkArena arena = arenaOpt.get();
            BlockState targetState = level.getBlockState(hitPos);

            if (InkPaintability.isPaintableBlock(level, hitPos, targetState)
                    && InkPaintability.isSurfaceExposed(level, hitPos, hitFace)) {

                byte team = TeamManager.getInstance().getTeam(drop.shooterId());

                InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();
                InkShotPaintTransaction transaction = new InkShotPaintTransaction();
                InkPaintAccumulator accumulator = transaction.forArena(arena);

                Vec3 dropVelocity = drop.velocity();
                MultiSurfacePaintResult paintResult = InkPaintingService.paintInto(
                        level, arena, inkStorage,
                        hitPos, hitFace, impactPosition,
                        drop.paintRadius(), team,
                        dropVelocity,
                        accumulator);

                if (transaction.hasAnyChanges()) {
                    transaction.commitAll(level);
                }

                if (paintResult.success()) {
                    LOGGER.debug("Trail drop paint: dropId={} pos={}/{} cells={}",
                            drop.dropId(), hitPos, hitFace,
                            paintResult.changedCellCount());
                }
            }
        }

        sendTrailDropImpactPayload(level, drop, impactPosition, hitFace);
        drop.markFinished();
    }

    private void finishDropWithoutPaint(ServerLevel level, ActiveTrailDrop drop,
                                         ProjectileFinishReason reason) {
        drop.setFinishReason(reason);
        sendTrailDropImpactPayload(level, drop, drop.position(), null);
        drop.markFinished();
    }

    // ===================================================================
    // Entity衝突判定
    // ===================================================================

    @Nullable
    private EntityHitResult findNearestEntityHit(ServerLevel level,
                                                  Vec3 start, Vec3 end,
                                                  ActiveInkShot shot) {
        AABB segmentBox = new AABB(start, end).inflate(shot.config().collisionRadius());

        double closestDist = Double.MAX_VALUE;
        EntityHitResult closestHit = null;

        Predicate<Entity> entityPredicate = e -> e.isAlive() && !e.isSpectator()
                && e.isAttackable() && !e.getUUID().equals(shot.shooterId());
        for (Entity entity : level.getEntities((Entity) null, segmentBox, entityPredicate)) {
            AABB entityBox = entity.getBoundingBox().inflate(shot.config().collisionRadius());
            Optional<Vec3> hitOpt = entityBox.clip(start, end);
            if (hitOpt.isPresent()) {
                double dist = start.distanceTo(hitOpt.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestHit = new EntityHitResult(entity, hitOpt.get());
                }
            }
        }

        return closestHit;
    }

    // ===================================================================
    // Payload送信
    // ===================================================================

    private void sendShotSpawnPayload(ServerLevel level, ActiveInkShot shot, ServerPlayer shooter) {
        InkShotSpawnPayload payload = new InkShotSpawnPayload(
                shot.shotId(), shot.shooterId(),
                shot.spawnPosition(), shot.initialVelocity(),
                shot.gravity(), shot.spawnGameTime(),
                shot.visualColorRgb(),
                shot.config().visualProjectileSize());

        for (ServerPlayer recipient : level.players()) {
            if (recipient.position().distanceToSqr(shot.spawnPosition()) <= 64.0 * 64.0) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(recipient, payload);
            }
        }
    }

    private void sendShotImpactPayload(ServerLevel level, ActiveInkShot shot,
                                        Vec3 impactPosition, @Nullable Direction face) {
        InkShotImpactPayload payload = new InkShotImpactPayload(
                shot.shotId(), impactPosition,
                face != null ? face : Direction.UP,
                shot.finishReason(), level.getGameTime());

        for (ServerPlayer recipient : level.players()) {
            if (recipient.position().distanceToSqr(impactPosition) <= 64.0 * 64.0) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(recipient, payload);
            }
        }
    }

    private void sendTrailDropSpawnPayload(ServerLevel level, ActiveTrailDrop drop) {
        InkTrailDropSpawnPayload payload = new InkTrailDropSpawnPayload(
                drop.dropId(), drop.parentShotId(), drop.shooterId(),
                drop.position(), drop.velocity(),
                drop.gravity(), drop.spawnGameTime(),
                drop.visualSize(), drop.colorRgb());

        for (ServerPlayer recipient : level.players()) {
            if (recipient.position().distanceToSqr(drop.position()) <= 64.0 * 64.0) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(recipient, payload);
            }
        }
    }

    private void sendTrailDropImpactPayload(ServerLevel level, ActiveTrailDrop drop,
                                             Vec3 impactPosition, @Nullable Direction face) {
        InkTrailDropImpactPayload payload = new InkTrailDropImpactPayload(
                drop.dropId(), impactPosition,
                face != null ? face : Direction.UP,
                drop.finishReason(), level.getGameTime());

        for (ServerPlayer recipient : level.players()) {
            if (recipient.position().distanceToSqr(impactPosition) <= 64.0 * 64.0) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(recipient, payload);
            }
        }
    }

    // ===================================================================
    // ユーティリティ
    // ===================================================================

    private static boolean isFinite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    /**
     * テスト・デバッグ用: アクティブな主弾数を返す。
     */
    public int activeShotCount() {
        return activeShots.size();
    }

    /**
     * テスト・デバッグ用: アクティブな滴数を返す。
     */
    public int activeDropCount() {
        return activeDrops.size();
    }
}
package yam.salmon.item;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.network.InkShotVisualPayload;
import yam.salmon.weapon.InkShooterConfig;
import yam.salmon.weapon.InkShooterService;
import yam.salmon.weapon.InkShooterVisualConfig;
import yam.salmon.weapon.InkShotEffects;
import yam.salmon.weapon.InkShotResult;
import yam.salmon.weapon.InkTrajectoryResult;
import yam.salmon.weapon.InkVisualColorResolver;
import yam.salmon.weapon.InkWeaponConfig;
import yam.salmon.weapon.InkWeaponRegistry;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * インクシューターアイテム。
 */
public class InkShooterItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".item");

    private final InkWeaponConfig config;
    static final Map<UUID, Long> nextFireTick = new ConcurrentHashMap<>();

    /** 視覚弾道Payloadの送信範囲（ブロック） */
    private static final double VISUAL_PAYLOAD_RANGE = 64.0;

    public InkShooterItem() {
        this(InkWeaponConfig.INK_SHOOTER);
    }

    public InkShooterItem(InkWeaponConfig config) {
        super(new Properties()
                .setId(ResourceKey.create(Registries.ITEM, Salmon.id("ink_shooter")))
                .stacksTo(1)
                .useBlockDescriptionPrefix());
        this.config = config;
    }

    public InkWeaponConfig getConfig() {
        return config;
    }

    // ===================================================================
    // 右クリック開始（useOn + use）
    // ===================================================================

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        InteractionHand hand = context.getHand();
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof InkShooterItem) {
            if (player instanceof ServerPlayer serverPlayer) {
                startUsingAndFireFirst(serverPlayer, hand);
            } else {
                player.startUsingItem(hand);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!player.getItemInHand(hand).is(this)) {
            return InteractionResult.PASS;
        }
        startUsingAndFireFirst(serverPlayer, hand);
        return InteractionResult.CONSUME;
    }

    private void startUsingAndFireFirst(ServerPlayer player, InteractionHand hand) {
        player.startUsingItem(hand);
        long serverTick = player.level().getServer().getTickCount();
        if (canFire(player, serverTick, config.fireIntervalTicks())) {
            fire(player, config, serverTick);
        }
    }

    // ===================================================================
    // 使用状態確認・間隔管理
    // ===================================================================

    public static boolean isUsingShooter(ServerPlayer player) {
        return player.isUsingItem()
                && player.getUseItem().getItem() instanceof InkShooterItem;
    }

    public static boolean canFire(ServerPlayer player, long serverTick, int fireIntervalTicks) {
        Long nextTick = nextFireTick.get(player.getUUID());
        return nextTick == null || serverTick >= nextTick;
    }

    public static void markFired(ServerPlayer player, long serverTick, int fireIntervalTicks) {
        nextFireTick.put(player.getUUID(), serverTick + fireIntervalTicks);
    }

    public static void cleanup(ServerPlayer player) {
        nextFireTick.remove(player.getUUID());
    }

    // ===================================================================
    // 射撃実行
    // ===================================================================

    public static void fire(ServerPlayer player, InkWeaponConfig config, long serverTick) {
        ServerLevel level = player.level();

        if (!canFire(player, serverTick, config.fireIntervalTicks())) {
            return;
        }

        long shotSeed = player.level().getRandom().nextLong();
        InkTrajectoryResult result = InkShooterService.fire(player, config, shotSeed);

        // 発射音（サーバー側即時）
        InkShotEffects.spawnFireEffect(level, player.getEyePosition(), player);

        // 視覚弾道Payloadを周囲に送信
        sendVisualPayload(player, result, config);

        markFired(player, serverTick, config.fireIntervalTicks());

        LOGGER.debug("Shooter fired: player={} hitType={} tick={}",
                player.getUUID(), result.hitType(), serverTick);
    }

    // ===================================================================
    // 視覚弾道Payload送信
    // ===================================================================

    private static void sendVisualPayload(ServerPlayer player, InkTrajectoryResult result,
                                           InkWeaponConfig config) {
        ServerLevel level = player.level();
        InkShooterVisualConfig vis = InkShooterVisualConfig.DEFAULT;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();

        // 視覚開始位置: 手元から（軌道の最初の点を基準に補正）
        Vec3 visualStart = eyePos.add(lookDir.scale(vis.launchForwardOffset()));

        // 軌道点を視覚開始位置からの相対でリスト化
        List<Vec3> trajPoints = result.points();
        java.util.List<Vec3> visualPoints = new java.util.ArrayList<>();

        // 最初の点を視覚開始位置に置き換え
        visualPoints.add(visualStart);

        // 残りの軌道点を間引いて追加（最大制御点に収める）
        int maxPoints = InkShotVisualPayload.MAX_CONTROL_POINTS - 1;
        int remainingTrajPoints = trajPoints.size() - 1;
        if (remainingTrajPoints > 0) {
            int step = Math.max(1, remainingTrajPoints / maxPoints);
            for (int i = 1; i < trajPoints.size(); i += step) {
                visualPoints.add(trajPoints.get(i));
            }
            // 最終点が含まれていない場合は追加
            Vec3 last = trajPoints.get(trajPoints.size() - 1);
            if (!visualPoints.get(visualPoints.size() - 1).equals(last)) {
                visualPoints.add(last);
            }
        }

        // 飛行tick数 = 実際にシミュレーションが走ったtick数（サーバー実測値）
        int flightTicks = Math.max(1, result.age() + 1);

        int colorRgb = InkVisualColorResolver.resolveShotColor(player);

        byte hitType = switch (result.hitType()) {
            case MISS -> InkShotVisualPayload.HIT_MISS;
            case BLOCK_HIT -> InkShotVisualPayload.HIT_BLOCK;
            case ENTITY_HIT -> InkShotVisualPayload.HIT_ENTITY;
        };

        // トレイル滴ビジュアルをPayload用に変換
        List<InkShotVisualPayload.InkTrailDropVisual> trailDrops = new java.util.ArrayList<>();
        if (result.trailPaintResult() != null) {
            for (var drop : result.trailPaintResult().visuals()) {
                if (trailDrops.size() >= InkShotVisualPayload.MAX_TRAIL_DROPS) break;
                trailDrops.add(new InkShotVisualPayload.InkTrailDropVisual(
                        drop.start(), drop.end(), drop.travelTicks(),
                        config.trailPaintConfig().visualDropSize()));
            }
        }

        InkShotVisualPayload payload = new InkShotVisualPayload(
                player.getId(),
                visualPoints,
                flightTicks,
                colorRgb,
                config.visualProjectileSize(),
                hitType,
                trailDrops
        );

        for (ServerPlayer recipient : level.players()) {
            if (recipient.position().distanceToSqr(player.position())
                    <= VISUAL_PAYLOAD_RANGE * VISUAL_PAYLOAD_RANGE) {
                ServerPlayNetworking.send(recipient, payload);
            }
        }
    }
}
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
import yam.salmon.weapon.InkVisualColorResolver;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * インクシューターアイテム。
 */
public class InkShooterItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".item");

    private final InkShooterConfig config;
    static final Map<UUID, Long> nextFireTick = new ConcurrentHashMap<>();

    /** 視覚弾道Payloadの送信範囲（ブロック） */
    private static final double VISUAL_PAYLOAD_RANGE = 64.0;

    public InkShooterItem() {
        this(InkShooterConfig.DEFAULT);
    }

    public InkShooterItem(InkShooterConfig config) {
        super(new Properties()
                .setId(ResourceKey.create(Registries.ITEM, Salmon.id("ink_shooter")))
                .stacksTo(1)
                .useBlockDescriptionPrefix());
        this.config = config;
    }

    public InkShooterConfig getConfig() {
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
            fire(player, config);
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

    public static void fire(ServerPlayer player, InkShooterConfig config) {
        ServerLevel level = player.level();
        long serverTick = level.getServer().getTickCount();

        if (!canFire(player, serverTick, config.fireIntervalTicks())) {
            return;
        }

        InkShotResult.Result result = InkShooterService.fire(player, config);

        // 発射音（サーバー側即時）
        InkShotEffects.spawnFireEffect(level, player.getEyePosition(), player);

        // 視覚弾道Payloadを周囲に送信
        sendVisualPayload(player, result, config);

        markFired(player, serverTick, config.fireIntervalTicks());

        LOGGER.debug("Shooter fired: player={} result={} tick={}",
                player.getUUID(), result.type(), serverTick);
    }

    // ===================================================================
    // 視覚弾道Payload送信
    // ===================================================================

    private static void sendVisualPayload(ServerPlayer player, InkShotResult.Result result,
                                           InkShooterConfig config) {
        ServerLevel level = player.level();
        InkShooterVisualConfig vis = InkShooterVisualConfig.DEFAULT;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        Vec3 visualStart = eyePos.add(lookDir.scale(vis.launchForwardOffset()));
        Vec3 visualEnd = result.endPosition();

        double distance = visualStart.distanceTo(visualEnd);
        int travelTicks = Math.max(vis.minTravelTicks(),
                Math.min(vis.maxTravelTicks(),
                        (int) Math.ceil(distance / vis.speedBlocksPerTick())));

        int colorRgb = InkVisualColorResolver.resolveShotColor(player);

        byte hitType = switch (result.type()) {
            case MISS -> InkShotVisualPayload.HIT_MISS;
            case BLOCK_HIT -> InkShotVisualPayload.HIT_BLOCK;
            case ENTITY_HIT -> InkShotVisualPayload.HIT_ENTITY;
        };

        InkShotVisualPayload payload = new InkShotVisualPayload(
                player.getId(),
                visualStart.x, visualStart.y, visualStart.z,
                visualEnd.x, visualEnd.y, visualEnd.z,
                travelTicks,
                colorRgb,
                vis.projectileSize(),
                hitType
        );

        for (ServerPlayer recipient : level.players()) {
            if (recipient.position().distanceToSqr(player.position())
                    <= VISUAL_PAYLOAD_RANGE * VISUAL_PAYLOAD_RANGE) {
                ServerPlayNetworking.send(recipient, payload);
            }
        }
    }
}
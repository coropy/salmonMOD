package yam.salmon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import yam.salmon.Salmon;
import yam.salmon.arena.ArenaPermission;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.ink.*;
import yam.salmon.network.InkSyncManager;

/**
 * インクで塗装可能なブロック。
 * BlockEntity は持たず、塗装データは InkStorage で管理する。
 *
 * <p>管理者が素手で右クリックすると塗装を行う。
 * 通常右クリック=Team A、Shift+右クリック=Team B。</p>
 */
public class InkableBlock extends Block {

    public InkableBlock() {
        super(Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Salmon.id("inkable_block")))
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // クリエイティブ + 権限レベル2以上 + 素手 のときのみ塗装
        if (!ArenaPermission.canConfigure(player)) {
            return InteractionResult.CONSUME;
        }

        // 素手チェック（メインハンドに何も持っていない）
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.CONSUME;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }

        var direction = hitResult.getDirection();
        Vec3 hitLoc = hitResult.getLocation();

        // ローカル座標計算
        double localX = clampToRange(hitLoc.x - pos.getX(), 0.0, 1.0);
        double localY = clampToRange(hitLoc.y - pos.getY(), 0.0, 1.0);
        double localZ = clampToRange(hitLoc.z - pos.getZ(), 0.0, 1.0);

        // Phase 1 の判定を再利用
        PaintabilityResult result = InkPaintability.checkPaintable(serverLevel, pos, direction);

        if (!result.paintable()) {
            String reason = result.failureReason()
                    .map(fr -> switch (fr) {
                        case NOT_PAINTABLE_BLOCK -> "対象ブロックがink_paintableタグに含まれていません";
                        case OUTSIDE_ARENA -> "インクアリーナ外です";
                        case FACE_OCCLUDED -> "指定面が隣接ブロックに塞がれています";
                    })
                    .orElse("不明な理由");
            serverPlayer.sendSystemMessage(
                    Component.literal("塗装不可: " + reason)
            );
            Salmon.LOGGER.info("Paint rejected: reason={} at {} face {} player={}",
                    reason, pos, direction, serverPlayer.getUUID());
            return InteractionResult.CONSUME;
        }

        InkArena arena = result.arena().orElseThrow();

        // UV → セル座標 変換
        InkFaceCoordinates coords = InkFaceCoordinates.fromHit(direction, localX, localY, localZ);

        // ShiftでTeam B、通常でTeam A
        byte team = player.isShiftKeyDown() ? InkTeam.TEAM_B : InkTeam.TEAM_A;

        // 塗装実行
        InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();
        PaintResult paintResult = inkStorage.paint(serverLevel, arena, pos, direction,
                coords, InkStorage.DEFAULT_PAINT_RADIUS, team);

        if (paintResult.success()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("塗装しました: Arena #" + paintResult.arenaNumber()
                            + " / " + InkTeam.toName(paintResult.team())
                            + " / Block=(" + pos.getX() + "," + pos.getY() + "," + pos.getZ()
                            + ") / Face=" + direction
                            + " / Cell=(" + coords.cellU() + "," + coords.cellV()
                            + ") / Changed=" + paintResult.changedCells())
            );
            // SavedData を dirty にして保存
            InkArenaManager.getInstance().saveInkDataNow(serverLevel);

            // 変更があった面データをクライアントへ同期
            inkStorage.getFace(arena, pos, direction).ifPresent(updatedFaceData -> {
                InkSyncManager.getInstance().broadcastFaceUpdate(
                        serverLevel, arena, pos, direction,
                        updatedFaceData, paintResult.changedCells());
            });
        } else if (paintResult.failureReason() == PaintFailureReason.NO_CHANGE) {
            serverPlayer.sendSystemMessage(
                    Component.literal("変更なし: すでに同じチームのインクです")
            );
        } else {
            String failMsg = switch (paintResult.failureReason()) {
                case NO_PERMISSION -> "権限がありません";
                case NOT_PAINTABLE_BLOCK -> "対象ブロックがink_paintableタグに含まれていません";
                case OUTSIDE_ARENA -> "インクアリーナ外です";
                case FACE_OCCLUDED -> "指定面が隣接ブロックに塞がれています";
                case INVALID_TEAM -> "無効なチームです";
                case NO_CHANGE -> "変更なし";
            };
            serverPlayer.sendSystemMessage(
                    Component.literal("塗装失敗: " + failMsg)
            );
        }

        return InteractionResult.CONSUME;
    }

    /** 値を [0.0, 1.0] にクランプして小数点3桁で文字列化 */
    private static String formatClamped(double value) {
        return String.format("%.3f", clampToRange(value, 0.0, 1.0));
    }

    private static double clampToRange(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
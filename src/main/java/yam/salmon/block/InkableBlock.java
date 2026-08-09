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
 * <p>Phase 4: 複数ブロックにまたがる塗装に対応。
 * クリック面の同一平面上で円AABB内の隣接ブロックにも塗装を分配する。</p>
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

        // アリーナを先に検索
        InkArena arena = InkArenaManager.getInstance().findArenaContaining(serverLevel, pos).orElse(null);
        if (arena == null) {
            serverPlayer.sendSystemMessage(
                    Component.literal("塗装不可: インクアリーナ外です")
            );
            Salmon.LOGGER.info("Paint rejected: reason=OUTSIDE_ARENA at {} face {} player={}",
                    pos, direction, serverPlayer.getUUID());
            return InteractionResult.CONSUME;
        }

        // Phase 1 の判定（アリーナありでチェック）
        PaintabilityResult result = InkPaintability.checkPaintable(serverLevel, pos, direction, arena);

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

        // ShiftでTeam B、通常でTeam A
        byte team = player.isShiftKeyDown() ? InkTeam.TEAM_B : InkTeam.TEAM_A;

        // Phase 4+5: 共通塗装サービスを使用（クリック塗装は歪みなし）
        InkStorage inkStorage = InkArenaManager.getInstance().getInkStorage();
        MultiSurfacePaintResult paintResult = InkPaintingService.paint(
                serverLevel, arena, inkStorage,
                pos, direction, hitLoc,
                InkPaintDistributor.DEFAULT_PAINT_RADIUS_BLOCKS, team,
                null);

        if (paintResult.success()) {
            // 管理者向けメッセージ
            serverPlayer.sendSystemMessage(
                    Component.literal("塗装しました: Arena #" + arena.getArenaNumber()
                            + " / " + InkTeam.toName(team)
                            + " / Changed surfaces=" + paintResult.changedSurfaceCount()
                            + " / Changed cells=" + paintResult.changedCellCount()
                            + " / Radius=" + String.format("%.3f", InkPaintDistributor.DEFAULT_PAINT_RADIUS_BLOCKS))
            );

            // 詳細デバッグ（全座標表示）: 常にログへ出力、プレイヤーには簡易表示
            if (paintResult.updatedSurfaces().size() <= 10) {
                for (var surface : paintResult.updatedSurfaces()) {
                    serverPlayer.sendSystemMessage(
                            Component.literal("  (" + surface.blockPos().getX()
                                    + "," + surface.blockPos().getY()
                                    + "," + surface.blockPos().getZ()
                                    + ")/" + surface.face()
                                    + " changed=" + surface.changedCells())
                    );
                }
            }
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

    /** 値を [0.0, 1.0] にクランプ */
    private static double clampToRange(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
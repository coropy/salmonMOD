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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import yam.salmon.Salmon;
import yam.salmon.arena.ArenaPermission;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.network.ArenaDebugSync;
import yam.salmon.selection.PlayerMarkerSelectionManager;
import yam.salmon.selection.PlayerMarkerSelectionManager.MarkerSelection;

import java.util.Optional;

/**
 * エリアマーカーブロック。
 * 2個設置して対角を指定し、インクアリーナを登録する。
 */
public class InkAreaMarkerBlock extends Block implements EntityBlock {

    public InkAreaMarkerBlock() {
        super(Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Salmon.id("ink_area_marker")))
                .strength(3.0F, 3600000.0F) // 高い爆発耐性
                .requiresCorrectToolForDrops() // ツルハシで破壊可能
                .sound(SoundType.STONE)
                .pushReaction(PushReaction.BLOCK) // ピストンで移動不可
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InkAreaMarkerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // マーカーブロック所持チェック
        if (!player.getMainHandItem().is(ModBlocks.INK_AREA_MARKER_BLOCK.asItem())
                && !player.getOffhandItem().is(ModBlocks.INK_AREA_MARKER_BLOCK.asItem())) {
            player.sendSystemMessage(
                    Component.literal("インクエリアマーカーを持って右クリックしてください")
            );
            return InteractionResult.CONSUME;
        }

        // 権限チェック
        if (!ArenaPermission.canConfigure(player)) {
            return InteractionResult.CONSUME;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof InkAreaMarkerBlockEntity markerEntity)) {
            Salmon.LOGGER.warn("BlockEntity mismatch at {}", pos);
            return InteractionResult.CONSUME;
        }

        ResourceKey<Level> dimension = level.dimension();

        // すでに別アリーナに所属しているマーカーは選択不可
        Optional<InkArena> existingArena = InkArenaManager.getInstance().getArenaByMarker(markerEntity.getMarkerId());
        if (existingArena.isPresent()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("このマーカーはすでにインクアリーナに登録されています")
            );
            return InteractionResult.CONSUME;
        }

        PlayerMarkerSelectionManager selectionManager = PlayerMarkerSelectionManager.getInstance();
        MarkerSelection selection = selectionManager.getSelection(serverPlayer.getUUID());

        // 同じマーカーを再選択 → 選択解除
        if (selection != null && selection.markerId().equals(markerEntity.getMarkerId())) {
            selectionManager.clearSelection(serverPlayer.getUUID());
            serverPlayer.sendSystemMessage(
                    Component.literal("第1地点の選択を解除しました")
            );
            Salmon.LOGGER.info("Player {} deselected marker: markerId={}",
                    serverPlayer.getUUID(), markerEntity.getMarkerId());
            return InteractionResult.CONSUME;
        }

        // 第1地点選択がない → これを第1地点に
        if (selection == null) {
            selectionManager.setSelection(
                    serverPlayer.getUUID(),
                    markerEntity.getMarkerId(),
                    pos,
                    dimension
            );
            serverPlayer.sendSystemMessage(
                    Component.literal("第1地点を選択しました: X=" + pos.getX()
                            + ", Y=" + pos.getY()
                            + ", Z=" + pos.getZ())
            );
            Salmon.LOGGER.info("Player {} selected first marker: markerId={}, pos={}, dim={}",
                    serverPlayer.getUUID(), markerEntity.getMarkerId(), pos, dimension);
            return InteractionResult.CONSUME;
        }

        // 第1地点選択あり → 第2地点としてアリーナ登録を試みる

        // 安全性チェック: 別ディメンション
        if (!selection.dimension().equals(dimension)) {
            serverPlayer.sendSystemMessage(
                    Component.literal("異なるディメンションのマーカーはペアにできません")
            );
            selectionManager.clearSelection(serverPlayer.getUUID());
            Salmon.LOGGER.warn("Player {} tried to pair markers across dimensions: dim1={}, dim2={}",
                    serverPlayer.getUUID(), selection.dimension(), dimension);
            return InteractionResult.CONSUME;
        }

        // 同じマーカー（再チェック、理論上ここには来ないが念のため）
        if (selection.markerId().equals(markerEntity.getMarkerId())) {
            selectionManager.clearSelection(serverPlayer.getUUID());
            serverPlayer.sendSystemMessage(
                    Component.literal("第1地点の選択を解除しました")
            );
            return InteractionResult.CONSUME;
        }

        // 第1地点のマーカーがまだ有効かチェック
        BlockPos firstPos = selection.pos();
        BlockEntity firstBe = level.getBlockEntity(firstPos);
        if (!(firstBe instanceof InkAreaMarkerBlockEntity firstMarker)) {
            selectionManager.clearSelection(serverPlayer.getUUID());
            serverPlayer.sendSystemMessage(
                    Component.literal("第1地点のマーカーが見つかりませんでした。選択を解除しました")
            );
            Salmon.LOGGER.warn("First marker BlockEntity not found or invalid at {}", firstPos);
            return InteractionResult.CONSUME;
        }

        // 第1地点のマーカーが別アリーナに所属していないか再確認
        if (InkArenaManager.getInstance().getArenaByMarker(firstMarker.getMarkerId()).isPresent()) {
            selectionManager.clearSelection(serverPlayer.getUUID());
            serverPlayer.sendSystemMessage(
                    Component.literal("第1地点のマーカーが別のアリーナに登録されました。選択を解除しました")
            );
            return InteractionResult.CONSUME;
        }

        // アリーナ作成
        InkArenaManager.ArenaCreateResult result = InkArenaManager.getInstance().createArena(
                dimension,
                selection.pos(),
                pos,
                firstMarker.getMarkerId(),
                markerEntity.getMarkerId()
        );

        if (result.success()) {
            firstMarker.setPaired(markerEntity.getMarkerId(), result.arena().getArenaId(), true);
            markerEntity.setPaired(firstMarker.getMarkerId(), result.arena().getArenaId(), false);

            InkArena arena = result.arena();
            serverPlayer.sendSystemMessage(
                    Component.literal("インクアリーナ #" + arena.getArenaNumber() + " を登録しました: X="
                            + arena.getMin().getX() + ".." + arena.getMax().getX()
                            + ", Y=" + arena.getMin().getY() + ".." + arena.getMax().getY()
                            + ", Z=" + arena.getMin().getZ() + ".." + arena.getMax().getZ())
            );
            Salmon.LOGGER.info("Arena created: arenaNumber={}, arenaUuid={}, dimension={}, cornerA={}, cornerB={}, min={}, max={}, playerUuid={}",
                    arena.getArenaNumber(), arena.getArenaId(), arena.getDimension().identifier(),
                    arena.getCornerA(), arena.getCornerB(), arena.getMin(), arena.getMax(),
                    serverPlayer.getUUID());

            // デバッグ表示同期: 同一次元の全デバッグ有効プレイヤーに追加通知
            ArenaDebugSync.broadcastArenaAdded(serverLevel, arena);
        } else {
            serverPlayer.sendSystemMessage(
                    Component.literal(result.message())
            );
            Salmon.LOGGER.warn("Arena creation failed: reason={}, player={}", result.message(), serverPlayer.getUUID());
        }

        // いずれの場合も選択を解除
        selectionManager.clearSelection(serverPlayer.getUUID());

        return InteractionResult.CONSUME;
    }
}
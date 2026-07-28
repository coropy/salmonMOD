package yam.salmon.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import yam.salmon.Salmon;
import yam.salmon.arena.InkArena;
import yam.salmon.arena.InkArenaManager;
import yam.salmon.network.ArenaDebugSync;

import java.util.Optional;
import java.util.UUID;

/**
 * エリアマーカーブロックのBlockEntity。
 * マーカー固有UUID、ペア相手UUID、所属アリーナUUIDを保持する。
 */
public class InkAreaMarkerBlockEntity extends BlockEntity {

    /** このマーカーの固有UUID */
    private UUID markerId;

    /** ペア相手のマーカーUUID（登録済みの場合） */
    @Nullable
    private UUID pairedMarkerId;

    /** 所属アリーナUUID（登録済みの場合） */
    @Nullable
    private UUID arenaId;

    /** 自分がアリーナの cornerA かどうか（登録済みの場合） */
    private boolean isCornerA;

    public InkAreaMarkerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlocks.INK_AREA_MARKER_BLOCK_ENTITY, pos, blockState);
        this.markerId = UUID.randomUUID();
    }

    public UUID getMarkerId() {
        return markerId;
    }

    @Nullable
    public UUID getPairedMarkerId() {
        return pairedMarkerId;
    }

    @Nullable
    public UUID getArenaId() {
        return arenaId;
    }

    public boolean isCornerA() {
        return isCornerA;
    }

    /**
     * アリーナ登録時に呼び出し、ペア情報を設定する。
     */
    public void setPaired(UUID pairedMarkerId, UUID arenaId, boolean isCornerA) {
        this.pairedMarkerId = pairedMarkerId;
        this.arenaId = arenaId;
        this.isCornerA = isCornerA;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * ペア情報を解除する（アリーナ削除時など）。冪等。
     */
    public void clearPairing() {
        if (this.pairedMarkerId == null && this.arenaId == null) {
            return; // すでに解除済み
        }
        this.pairedMarkerId = null;
        this.arenaId = null;
        this.isCornerA = false;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        Salmon.LOGGER.info("Cleared pairing for marker: markerId={}, pos={}", markerId, worldPosition);
    }

    /**
     * ブロック破壊時の処理。アリーナを削除する。
     */
    public void onBroken(@Nullable Player player) {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (arenaId != null) {
            InkArenaManager manager = InkArenaManager.getInstance();

            // 削除前にアリーナ情報を取得（削除後は取得不可になるため）
            UUID otherMarkerId = this.pairedMarkerId;
            UUID currentArenaId = this.arenaId;
            Optional<InkArena> optArena = manager.getArenaByMarker(markerId);
            BlockPos otherPos = null;
            if (optArena.isPresent()) {
                InkArena arena = optArena.get();
                UUID otherId = markerId.equals(arena.getMarkerAId())
                        ? arena.getMarkerBId() : arena.getMarkerAId();
                otherPos = otherId.equals(arena.getMarkerAId())
                        ? arena.getCornerA() : arena.getCornerB();
            }

            // アリーナを削除（戻り値で実際に削除されたか確認）
            boolean removed = manager.removeArenaByMarker(markerId);

            if (removed) {
                // ペア相手のマーカーのペア情報を解除
                if (otherPos != null && otherMarkerId != null) {
                    clearOtherMarkerPairing(otherPos, otherMarkerId);
                }

                // 削除前に取得したアリーナ情報でデバッグ表示同期ブロードキャスト
                if (optArena.isPresent() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    ArenaDebugSync.broadcastArenaRemoved(serverLevel, optArena.get());
                }

                // 自分のペア情報も解除（冪等）
                clearPairing();

                if (player != null) {
                    player.sendSystemMessage(
                            Component.literal("エリアマーカーが破壊されたため、インクアリーナを削除しました")
                    );
                }
                Salmon.LOGGER.info("Arena removed due to marker destruction: markerId={}, arenaId={}", markerId, currentArenaId);
            } else {
                // アリーナが既に削除済みの場合、自分のペア情報だけクリア
                clearPairing();
                Salmon.LOGGER.info("Marker broken but arena already removed: markerId={}", markerId);
            }
        }
    }

    /**
     * ペア相手のマーカー BlockEntity のペアリング情報を解除する。
     * 事前に取得したアリーナ情報を使用するため、削除済みデータに依存しない。
     *
     * @param otherPos       ペア相手のBlockPos
     * @param otherMarkerId  ペア相手のマーカーUUID
     */
    private void clearOtherMarkerPairing(BlockPos otherPos, UUID otherMarkerId) {
        if (level == null) return;

        if (level.isLoaded(otherPos)) {
            if (level.getBlockEntity(otherPos) instanceof InkAreaMarkerBlockEntity otherBe) {
                otherBe.clearPairing();
                Salmon.LOGGER.info("Cleared other marker pairing: markerId={}, pos={}", otherBe.getMarkerId(), otherPos);
            }
        } else {
            // チャンク未ロード。次回ロード時に loadAdditional で自己修復される。
            Salmon.LOGGER.info("Other marker chunk not loaded, pairing will self-repair on load: otherPos={}, otherMarkerId={}",
                    otherPos, otherMarkerId);
        }
    }

    // -----------------------------------------------------------------------
    // 永続化 (ValueInput / ValueOutput - MC 26.2: net.minecraft.world.level.storage)
    // -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // MC 26.2: store(String, Codec<T>, T) - 引数順序が String 先頭に変更
        output.store("MarkerId", UUIDUtil.STRING_CODEC, markerId);
        if (pairedMarkerId != null) {
            output.store("PairedMarkerId", UUIDUtil.STRING_CODEC, pairedMarkerId);
        }
        if (arenaId != null) {
            output.store("ArenaId", UUIDUtil.STRING_CODEC, arenaId);
            output.store("IsCornerA", Codec.BOOL, isCornerA);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // MC 26.2: read(String, Codec<T>) - 引数順序が String 先頭に変更
        // MarkerId 読み込み（存在しない場合は新規生成）
        Optional<UUID> storedMarkerId = input.read("MarkerId", UUIDUtil.STRING_CODEC);
        if (storedMarkerId.isPresent()) {
            this.markerId = storedMarkerId.get();
        } else {
            this.markerId = UUID.randomUUID();
            Salmon.LOGGER.warn("MarkerId missing in BlockEntity data at {}, generated new: {}", worldPosition, markerId);
        }

        Optional<UUID> storedPairedId = input.read("PairedMarkerId", UUIDUtil.STRING_CODEC);
        this.pairedMarkerId = storedPairedId.orElse(null);

        Optional<UUID> storedArenaId = input.read("ArenaId", UUIDUtil.STRING_CODEC);
        if (storedArenaId.isPresent()) {
            this.arenaId = storedArenaId.get();
            this.isCornerA = input.read("IsCornerA", Codec.BOOL).orElse(false);
        } else {
            this.arenaId = null;
            this.isCornerA = false;
        }

        // 自己修復: ArenaIdがあるのにPairedMarkerIdがない場合は不整合としてログ出力
        if (this.arenaId != null && this.pairedMarkerId == null) {
            Salmon.LOGGER.warn("Inconsistent marker data at {}: has ArenaId but no PairedMarkerId. ArenaId={}",
                    worldPosition, arenaId);
        }

        // 自己修復: 孤立参照の検出（arenaId が存在するが ArenaSavedData に登録がない）
        if (this.arenaId != null && this.pairedMarkerId != null) {
            if (!InkArenaManager.getInstance().arenaExists(this.arenaId)) {
                Salmon.LOGGER.warn("Self-repair: orphaned arena reference detected at {}. arenaId={}, pairedMarkerId={} -> clearing",
                        worldPosition, this.arenaId, this.pairedMarkerId);
                this.arenaId = null;
                this.pairedMarkerId = null;
                this.isCornerA = false;
                setChanged();
            }
        }
    }

    /**
     * MC 26.2: saveToUpdateTag(ValueOutput) は getUpdateTag(HolderLookup.Provider) に置き換え。
     * CompoundTag を直接返す方式に変更された。
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("MarkerId", markerId.toString());
        if (pairedMarkerId != null) {
            tag.putString("PairedMarkerId", pairedMarkerId.toString());
        }
        if (arenaId != null) {
            tag.putString("ArenaId", arenaId.toString());
        }
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // -----------------------------------------------------------------------
    // デバッグ情報
    // -----------------------------------------------------------------------
    @Override
    public String toString() {
        return "InkAreaMarkerBlockEntity{" +
                "markerId=" + markerId +
                ", pairedMarkerId=" + pairedMarkerId +
                ", arenaId=" + arenaId +
                ", pos=" + worldPosition +
                '}';
    }

    public static void register() {
        // BlockEntityType の登録は ModBlocks で行う
    }
}
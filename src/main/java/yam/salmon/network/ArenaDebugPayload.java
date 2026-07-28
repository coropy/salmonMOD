package yam.salmon.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import yam.salmon.Salmon;

import java.util.UUID;

/**
 * アリーナデバッグ表示用のネットワークペイロード。
 * サーバー→クライアント方向のみ。
 *
 * Action:
 * - FULL_SYNC(0): 現在ディメンションの完全スナップショット
 * - ADD(1): アリーナ追加差分
 * - REMOVE(2): アリーナ削除差分
 * - CLEAR(3): クライアントキャッシュ消去
 */
public record ArenaDebugPayload(
        int action,
        int arenaNumber,
        UUID arenaUuid,
        BlockPos min,
        BlockPos max,
        BlockPos cornerA,
        BlockPos cornerB,
        UUID markerAId,
        UUID markerBId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ArenaDebugPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("arena_debug"));

    public static final StreamCodec<FriendlyByteBuf, ArenaDebugPayload> STREAM_CODEC =
            StreamCodec.ofMember(ArenaDebugPayload::write, ArenaDebugPayload::read);

    // Actions
    public static final int ACTION_FULL_SYNC = 0;
    public static final int ACTION_ADD = 1;
    public static final int ACTION_REMOVE = 2;
    public static final int ACTION_CLEAR = 3;

    @Override
    public CustomPacketPayload.Type<ArenaDebugPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        if (action == ACTION_CLEAR) {
            return;
        }
        buf.writeVarInt(arenaNumber);
        buf.writeUUID(arenaUuid);
        buf.writeBlockPos(min);
        buf.writeBlockPos(max);
        buf.writeBlockPos(cornerA);
        buf.writeBlockPos(cornerB);
        buf.writeUUID(markerAId);
        buf.writeUUID(markerBId);
    }

    public static ArenaDebugPayload read(FriendlyByteBuf buf) {
        int action = buf.readVarInt();
        if (action == ACTION_CLEAR) {
            return new ArenaDebugPayload(action, 0, UUID.randomUUID(),
                    BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO,
                    UUID.randomUUID(), UUID.randomUUID());
        }
        return new ArenaDebugPayload(
                action,
                buf.readVarInt(),
                buf.readUUID(),
                buf.readBlockPos(),
                buf.readBlockPos(),
                buf.readBlockPos(),
                buf.readBlockPos(),
                buf.readUUID(),
                buf.readUUID()
        );
    }

    /**
     * 完全同期の先頭を示すペイロード。
     * arenaNumber = -1 で FULL_SYNC 開始、-2 で FULL_SYNC 終了。
     * 実際のアリーナデータは個別の FULL_SYNC(0) + arenaNumber >= 0 ペイロード。
     */
    public static ArenaDebugPayload fullSyncBegin() {
        return new ArenaDebugPayload(ACTION_FULL_SYNC, -1, UUID.randomUUID(),
                BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO,
                UUID.randomUUID(), UUID.randomUUID());
    }

    public static ArenaDebugPayload fullSyncEnd() {
        return new ArenaDebugPayload(ACTION_FULL_SYNC, -2, UUID.randomUUID(),
                BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO,
                UUID.randomUUID(), UUID.randomUUID());
    }

    public boolean isFullSyncBegin() {
        return action == ACTION_FULL_SYNC && arenaNumber == -1;
    }

    public boolean isFullSyncEnd() {
        return action == ACTION_FULL_SYNC && arenaNumber == -2;
    }
}
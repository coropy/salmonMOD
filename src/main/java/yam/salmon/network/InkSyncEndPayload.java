package yam.salmon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import yam.salmon.Salmon;

/**
 * インク完全同期終了ペイロード。
 * サーバー→クライアント方向。
 *
 * <p>このペイロードを受信したクライアントは、
 * バッファリングしていた {@link InkFaceUpdatePayload} を
 * キャッシュにコミットする。</p>
 *
 * @param sessionId 同期セッションID（InkSyncBeginPayload と一致）
 * @param faceCount 実際に送信された面数
 */
public record InkSyncEndPayload(
        long sessionId,
        int faceCount
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkSyncEndPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_sync_end"));

    public static final StreamCodec<FriendlyByteBuf, InkSyncEndPayload> STREAM_CODEC =
            StreamCodec.ofMember(InkSyncEndPayload::write, InkSyncEndPayload::read);

    @Override
    public CustomPacketPayload.Type<InkSyncEndPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarLong(sessionId);
        buf.writeVarInt(faceCount);
    }

    public static InkSyncEndPayload read(FriendlyByteBuf buf) {
        return new InkSyncEndPayload(
                buf.readVarLong(),
                buf.readVarInt()
        );
    }
}
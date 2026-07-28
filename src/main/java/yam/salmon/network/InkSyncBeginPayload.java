package yam.salmon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import yam.salmon.Salmon;

/**
 * インク完全同期開始ペイロード。
 * サーバー→クライアント方向。
 *
 * <p>このペイロードを受信したクライアントは、
 * 指定ディメンションの古いインクキャッシュをクリアし、
 * 後続の {@link InkFaceUpdatePayload} をバッファリングする。</p>
 *
 * @param dimensionId 同期対象ディメンション
 * @param sessionId   同期セッションID（サーバー側で毎回発行）
 * @param faceCount   送信する面の総数
 * @param revision    同期開始時点のアリーナリビジョン
 */
public record InkSyncBeginPayload(
        Identifier dimensionId,
        long sessionId,
        int faceCount,
        long revision
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkSyncBeginPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_sync_begin"));

    public static final StreamCodec<FriendlyByteBuf, InkSyncBeginPayload> STREAM_CODEC =
            StreamCodec.ofMember(InkSyncBeginPayload::write, InkSyncBeginPayload::read);

    @Override
    public CustomPacketPayload.Type<InkSyncBeginPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId.toString());
        buf.writeVarLong(sessionId);
        buf.writeVarInt(faceCount);
        buf.writeVarLong(revision);
    }

    public static InkSyncBeginPayload read(FriendlyByteBuf buf) {
        return new InkSyncBeginPayload(
                Identifier.parse(buf.readUtf()),
                buf.readVarLong(),
                buf.readVarInt(),
                buf.readVarLong()
        );
    }
}
package yam.salmon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import yam.salmon.Salmon;

import java.util.UUID;

/**
 * アリーナのインクデータ全消去ペイロード。
 * サーバー→クライアント方向。
 *
 * <p>クライアントは対象アリーナに属するすべての面をキャッシュから削除する。</p>
 *
 * @param arenaUuid   アリーナUUID
 * @param arenaNumber アリーナ番号
 * @param dimensionId ディメンション識別子
 * @param revision    最新リビジョン（古い面更新が復活しないように）
 */
public record InkArenaClearPayload(
        UUID arenaUuid,
        int arenaNumber,
        Identifier dimensionId,
        long revision
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkArenaClearPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_arena_clear"));

    public static final StreamCodec<FriendlyByteBuf, InkArenaClearPayload> STREAM_CODEC =
            StreamCodec.ofMember(InkArenaClearPayload::write, InkArenaClearPayload::read);

    @Override
    public CustomPacketPayload.Type<InkArenaClearPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(arenaUuid);
        buf.writeVarInt(arenaNumber);
        buf.writeUtf(dimensionId.toString());
        buf.writeVarLong(revision);
    }

    public static InkArenaClearPayload read(FriendlyByteBuf buf) {
        return new InkArenaClearPayload(
                buf.readUUID(),
                buf.readVarInt(),
                Identifier.parse(buf.readUtf()),
                buf.readVarLong()
        );
    }
}
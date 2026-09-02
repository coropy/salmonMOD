package yam.salmon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import yam.salmon.Salmon;
import yam.salmon.ink.PlayerInkState;

import java.util.UUID;

/**
 * プレイヤーのインク状態更新ペイロード。
 * サーバー→クライアント方向。
 *
 * <p>状態変化時のみ送信される。将来的に他プレイヤーへの同期を拡張するため
 * {@code playerId} を含める。</p>
 *
 * @param playerId 状態が変化したプレイヤーのUUID
 * @param state    新しいインク状態
 */
public record InkPlayerStatePayload(
        UUID playerId,
        PlayerInkState state
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkPlayerStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_player_state"));

    public static final StreamCodec<FriendlyByteBuf, InkPlayerStatePayload> STREAM_CODEC =
            StreamCodec.ofMember(InkPlayerStatePayload::write, InkPlayerStatePayload::read);

    @Override
    public CustomPacketPayload.Type<InkPlayerStatePayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeByte(PlayerInkState.toNetworkId(state));
    }

    public static InkPlayerStatePayload read(FriendlyByteBuf buf) {
        return new InkPlayerStatePayload(
                buf.readUUID(),
                PlayerInkState.fromNetworkId(buf.readByte())
        );
    }
}

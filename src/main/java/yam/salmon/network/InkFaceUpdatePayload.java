package yam.salmon.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import yam.salmon.Salmon;
import yam.salmon.ink.InkSurfacePatchId;

import java.util.UUID;

/**
 * 1面のインクデータ更新ペイロード。
 * サーバー→クライアント方向。
 *
 * <p>変更された1面の64セル全体を送信する。
 * 差分同期および完全同期のバッチ送信に使用する。</p>
 *
 * <p>Phase 8: Patch ID 全情報（normal, plane, minU, minV, maxU, maxV）を
 * ペイロードに含め、クライアントが部分ブロック面の正確な位置に描画できるようにする。</p>
 *
 * @param arenaUuid   アリーナUUID
 * @param arenaNumber アリーナ番号
 * @param dimensionId ディメンション識別子
 * @param blockPos    ブロック座標
 * @param faceName    面のシリアライズ名（後方互換用・描画には使用しない）
 * @param patchId     Surface Patch ID（normal, plane, minU, minV, maxU, maxV を含む）
 * @param cells       64セル配列（byte[64]）
 * @param revision    アリーナごとの単調増加リビジョン
 */
public record InkFaceUpdatePayload(
        UUID arenaUuid,
        int arenaNumber,
        Identifier dimensionId,
        BlockPos blockPos,
        String faceName,
        InkSurfacePatchId patchId,
        byte[] cells,
        long revision
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkFaceUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_face_update"));

    public static final StreamCodec<FriendlyByteBuf, InkFaceUpdatePayload> STREAM_CODEC =
            StreamCodec.ofMember(InkFaceUpdatePayload::write, InkFaceUpdatePayload::read);

    /**
     * 面のDirectionを取得する。不正な名前の場合はnull。
     * @deprecated 描画には {@link #patchId()} の normal フィールドを使用すること。
     */
    public Direction getFace() {
        return Direction.byName(faceName);
    }

    @Override
    public CustomPacketPayload.Type<InkFaceUpdatePayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(arenaUuid);
        buf.writeVarInt(arenaNumber);
        buf.writeUtf(dimensionId.toString());
        buf.writeBlockPos(blockPos);
        buf.writeUtf(faceName);
        patchId.writeToBuffer(buf);
        buf.writeByteArray(cells);
        buf.writeVarLong(revision);
    }

    public static InkFaceUpdatePayload read(FriendlyByteBuf buf) {
        return new InkFaceUpdatePayload(
                buf.readUUID(),
                buf.readVarInt(),
                Identifier.parse(buf.readUtf()),
                buf.readBlockPos(),
                buf.readUtf(),
                InkSurfacePatchId.readFromBuffer(buf),
                buf.readByteArray(),
                buf.readVarLong()
        );
    }
}
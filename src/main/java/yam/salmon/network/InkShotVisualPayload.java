package yam.salmon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import yam.salmon.Salmon;

/**
 * インクシューターの視覚弾道ペイロード。
 * サーバー→クライアント方向。発射時に1回だけ送信される。
 */
public record InkShotVisualPayload(
        int shooterEntityId,
        double startX, double startY, double startZ,
        double endX, double endY, double endZ,
        int travelTicks,
        int colorRgb,
        float size,
        byte hitType // 0=miss, 1=block, 2=entity
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkShotVisualPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_shot_visual"));

    public static final StreamCodec<FriendlyByteBuf, InkShotVisualPayload> STREAM_CODEC =
            StreamCodec.ofMember(InkShotVisualPayload::write, InkShotVisualPayload::read);

    /** 命中タイプ定数 */
    public static final byte HIT_MISS = 0;
    public static final byte HIT_BLOCK = 1;
    public static final byte HIT_ENTITY = 2;

    public Vec3 start() {
        return new Vec3(startX, startY, startZ);
    }

    public Vec3 end() {
        return new Vec3(endX, endY, endZ);
    }

    @Override
    public CustomPacketPayload.Type<InkShotVisualPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(shooterEntityId);
        buf.writeDouble(startX);
        buf.writeDouble(startY);
        buf.writeDouble(startZ);
        buf.writeDouble(endX);
        buf.writeDouble(endY);
        buf.writeDouble(endZ);
        buf.writeVarInt(travelTicks);
        buf.writeVarInt(colorRgb);
        buf.writeFloat(size);
        buf.writeByte(hitType);
    }

    public static InkShotVisualPayload read(FriendlyByteBuf buf) {
        return new InkShotVisualPayload(
                buf.readVarInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readByte()
        );
    }
}
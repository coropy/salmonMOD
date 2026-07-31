package yam.salmon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import yam.salmon.Salmon;

import java.util.UUID;

/**
 * トレイル滴のSpawn Payload。サーバー→クライアント。
 * 主弾の飛行中に滴が生成されたときに送信される。
 */
public record InkTrailDropSpawnPayload(
        UUID dropId,
        UUID parentShotId,
        UUID shooterId,
        Vec3 startPosition,
        Vec3 initialVelocity,
        double gravity,
        long serverSpawnGameTime,
        float size,
        int colorRgb
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkTrailDropSpawnPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_trail_drop_spawn"));

    public static final StreamCodec<FriendlyByteBuf, InkTrailDropSpawnPayload> STREAM_CODEC =
            StreamCodec.ofMember(InkTrailDropSpawnPayload::write, InkTrailDropSpawnPayload::read);

    @Override
    public CustomPacketPayload.Type<InkTrailDropSpawnPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(dropId);
        buf.writeUUID(parentShotId);
        buf.writeUUID(shooterId);
        buf.writeDouble(startPosition.x);
        buf.writeDouble(startPosition.y);
        buf.writeDouble(startPosition.z);
        buf.writeDouble(initialVelocity.x);
        buf.writeDouble(initialVelocity.y);
        buf.writeDouble(initialVelocity.z);
        buf.writeDouble(gravity);
        buf.writeVarLong(serverSpawnGameTime);
        buf.writeFloat(size);
        buf.writeVarInt(colorRgb);
    }

    public static InkTrailDropSpawnPayload read(FriendlyByteBuf buf) {
        UUID dropId = buf.readUUID();
        UUID parentShotId = buf.readUUID();
        UUID shooterId = buf.readUUID();
        Vec3 startPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 initVel = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double gravity = buf.readDouble();
        long gameTime = buf.readVarLong();
        float size = buf.readFloat();
        int color = buf.readVarInt();
        return new InkTrailDropSpawnPayload(dropId, parentShotId, shooterId, startPos, initVel, gravity, gameTime, size, color);
    }
}
package yam.salmon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import yam.salmon.Salmon;

import java.util.UUID;

/**
 * 主弾のSpawn Payload。サーバー→クライアント。
 * 発射時に送信され、クライアント側で主弾の視覚モデルを生成する。
 */
public record InkShotSpawnPayload(
        UUID shotId,
        UUID shooterId,
        Vec3 startPosition,
        Vec3 initialVelocity,
        double gravity,
        long serverSpawnGameTime,
        int colorRgb,
        float size
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkShotSpawnPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_shot_spawn"));

    public static final StreamCodec<FriendlyByteBuf, InkShotSpawnPayload> STREAM_CODEC =
            StreamCodec.ofMember(InkShotSpawnPayload::write, InkShotSpawnPayload::read);

    @Override
    public CustomPacketPayload.Type<InkShotSpawnPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(shotId);
        buf.writeUUID(shooterId);
        buf.writeDouble(startPosition.x);
        buf.writeDouble(startPosition.y);
        buf.writeDouble(startPosition.z);
        buf.writeDouble(initialVelocity.x);
        buf.writeDouble(initialVelocity.y);
        buf.writeDouble(initialVelocity.z);
        buf.writeDouble(gravity);
        buf.writeVarLong(serverSpawnGameTime);
        buf.writeVarInt(colorRgb);
        buf.writeFloat(size);
    }

    public static InkShotSpawnPayload read(FriendlyByteBuf buf) {
        UUID shotId = buf.readUUID();
        UUID shooterId = buf.readUUID();
        Vec3 startPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 initVel = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double gravity = buf.readDouble();
        long gameTime = buf.readVarLong();
        int color = buf.readVarInt();
        float size = buf.readFloat();
        return new InkShotSpawnPayload(shotId, shooterId, startPos, initVel, gravity, gameTime, color, size);
    }
}
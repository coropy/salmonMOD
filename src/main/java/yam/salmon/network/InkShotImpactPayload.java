package yam.salmon.network;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import yam.salmon.Salmon;
import yam.salmon.weapon.ProjectileFinishReason;

import java.util.UUID;

/**
 * 主弾のImpact Payload。サーバー→クライアント。
 * 着弾・Entityヒット・タイムアウト時に送信され、
 * クライアント側で視覚主弾を終了させる。
 */
public record InkShotImpactPayload(
        UUID shotId,
        Vec3 impactPosition,
        Direction impactFace,
        ProjectileFinishReason finishReason,
        long serverImpactGameTime
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkShotImpactPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_shot_impact"));

    public static final StreamCodec<FriendlyByteBuf, InkShotImpactPayload> STREAM_CODEC =
            StreamCodec.ofMember(InkShotImpactPayload::write, InkShotImpactPayload::read);

    @Override
    public CustomPacketPayload.Type<InkShotImpactPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(shotId);
        buf.writeDouble(impactPosition.x);
        buf.writeDouble(impactPosition.y);
        buf.writeDouble(impactPosition.z);
        buf.writeVarInt(impactFace.get3DDataValue());
        buf.writeUtf(finishReason.name());
        buf.writeVarLong(serverImpactGameTime);
    }

    public static InkShotImpactPayload read(FriendlyByteBuf buf) {
        UUID shotId = buf.readUUID();
        Vec3 impactPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Direction face = Direction.from3DDataValue(buf.readVarInt());
        ProjectileFinishReason reason = ProjectileFinishReason.valueOf(buf.readUtf());
        long gameTime = buf.readVarLong();
        return new InkShotImpactPayload(shotId, impactPos, face, reason, gameTime);
    }
}
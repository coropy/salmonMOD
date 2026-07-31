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
 * トレイル滴のImpact Payload。サーバー→クライアント。
 * 滴が着弾・タイムアウト時に送信され、
 * クライアント側で視覚滴を終了させる。
 */
public record InkTrailDropImpactPayload(
        UUID dropId,
        Vec3 impactPosition,
        Direction impactFace,
        ProjectileFinishReason finishReason,
        long serverImpactGameTime
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkTrailDropImpactPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_trail_drop_impact"));

    public static final StreamCodec<FriendlyByteBuf, InkTrailDropImpactPayload> STREAM_CODEC =
            StreamCodec.ofMember(InkTrailDropImpactPayload::write, InkTrailDropImpactPayload::read);

    @Override
    public CustomPacketPayload.Type<InkTrailDropImpactPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(dropId);
        buf.writeDouble(impactPosition.x);
        buf.writeDouble(impactPosition.y);
        buf.writeDouble(impactPosition.z);
        buf.writeVarInt(impactFace.get3DDataValue());
        buf.writeUtf(finishReason.name());
        buf.writeVarLong(serverImpactGameTime);
    }

    public static InkTrailDropImpactPayload read(FriendlyByteBuf buf) {
        UUID dropId = buf.readUUID();
        Vec3 impactPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Direction face = Direction.from3DDataValue(buf.readVarInt());
        ProjectileFinishReason reason = ProjectileFinishReason.valueOf(buf.readUtf());
        long gameTime = buf.readVarLong();
        return new InkTrailDropImpactPayload(dropId, impactPos, face, reason, gameTime);
    }
}
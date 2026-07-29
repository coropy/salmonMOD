package yam.salmon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import yam.salmon.Salmon;

import java.util.ArrayList;
import java.util.List;

/**
 * インクシューターの視覚弾道ペイロード。
 * サーバー→クライアント方向。発射時に1回だけ送信される。
 *
 * <p>サーバー側の確定した放物線軌道制御点をクライアントに送信し、
 * クライアント側で同じ経路を補間描画する。</p>
 */
public record InkShotVisualPayload(
        int shooterEntityId,
        List<Vec3> trajectoryPoints,
        int totalTicks,
        int colorRgb,
        float size,
        byte hitType, // 0=miss, 1=block, 2=entity
        List<InkTrailDropVisual> trailDrops
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InkShotVisualPayload> TYPE =
            new CustomPacketPayload.Type<>(Salmon.id("ink_shot_visual"));

    public static final StreamCodec<FriendlyByteBuf, InkShotVisualPayload> STREAM_CODEC =
            StreamCodec.ofMember(InkShotVisualPayload::write, InkShotVisualPayload::read);

    /** 命中タイプ定数 */
    public static final byte HIT_MISS = 0;
    public static final byte HIT_BLOCK = 1;
    public static final byte HIT_ENTITY = 2;

    /** 最大制御点数（帯域制限） */
    public static final int MAX_CONTROL_POINTS = 24;
    /** 最大滴ビジュアル数（帯域制限） */
    public static final int MAX_TRAIL_DROPS = 12;

    /** トレイル滴ビジュアル情報 */
    public record InkTrailDropVisual(Vec3 start, Vec3 end, int travelTicks, float size) {}

    public Vec3 start() {
        return trajectoryPoints.isEmpty() ? Vec3.ZERO : trajectoryPoints.get(0);
    }

    public Vec3 end() {
        return trajectoryPoints.isEmpty() ? Vec3.ZERO : trajectoryPoints.get(trajectoryPoints.size() - 1);
    }

    @Override
    public CustomPacketPayload.Type<InkShotVisualPayload> type() {
        return TYPE;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(shooterEntityId);

        // 制御点数（上限チェック付き）
        int pointCount = Math.min(trajectoryPoints.size(), MAX_CONTROL_POINTS);
        buf.writeVarInt(pointCount);
        for (int i = 0; i < pointCount; i++) {
            Vec3 p = trajectoryPoints.get(i);
            buf.writeDouble(p.x);
            buf.writeDouble(p.y);
            buf.writeDouble(p.z);
        }

        buf.writeVarInt(totalTicks);
        buf.writeVarInt(colorRgb);
        buf.writeFloat(size);
        buf.writeByte(hitType);

        // トレイル滴のビジュアル情報
        int dropCount = Math.min(trailDrops != null ? trailDrops.size() : 0, MAX_TRAIL_DROPS);
        buf.writeVarInt(dropCount);
        for (int i = 0; i < dropCount; i++) {
            InkTrailDropVisual drop = trailDrops.get(i);
            buf.writeDouble(drop.start().x);
            buf.writeDouble(drop.start().y);
            buf.writeDouble(drop.start().z);
            buf.writeDouble(drop.end().x);
            buf.writeDouble(drop.end().y);
            buf.writeDouble(drop.end().z);
            buf.writeVarInt(drop.travelTicks());
            buf.writeFloat(drop.size());
        }
    }

    public static InkShotVisualPayload read(FriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();

        int pointCount = buf.readVarInt();
        List<Vec3> points = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            points.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }

        int travelTicks = buf.readVarInt();
        int colorRgb = buf.readVarInt();
        float size = buf.readFloat();
        byte hitType = buf.readByte();

        int dropCount = buf.readVarInt();
        List<InkTrailDropVisual> drops = new ArrayList<>(dropCount);
        for (int i = 0; i < dropCount; i++) {
            Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 end = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            int dropTicks = buf.readVarInt();
            float dropSize = buf.readFloat();
            drops.add(new InkTrailDropVisual(start, end, dropTicks, dropSize));
        }

        return new InkShotVisualPayload(shooterId, points, travelTicks, colorRgb, size, hitType, drops);
    }
}

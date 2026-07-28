package yam.salmon.arena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * インクアリーナのデータモデル。
 * 2つのマーカーブロックの対角座標から直方体範囲を定義する。
 * 範囲は両端を含む inclusive 方式。
 */
public class InkArena {

    /**
     * Codec for serializing/deserializing InkArena via RecordCodecBuilder.
     * dataVersion=2: arenaNumber フィールドを追加（optional、デフォルト0で後方互換）。
     */
    public static final Codec<InkArena> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("arenaId").forGetter(InkArena::getArenaId),
            Codec.INT.optionalFieldOf("arenaNumber", 0).forGetter(InkArena::getArenaNumber),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(InkArena::getDimension),
            BlockPos.CODEC.fieldOf("cornerA").forGetter(InkArena::getCornerA),
            BlockPos.CODEC.fieldOf("cornerB").forGetter(InkArena::getCornerB),
            UUIDUtil.CODEC.fieldOf("markerAId").forGetter(InkArena::getMarkerAId),
            UUIDUtil.CODEC.fieldOf("markerBId").forGetter(InkArena::getMarkerBId)
    ).apply(instance, InkArena::new));

    private final UUID arenaId;
    private final int arenaNumber;
    private final ResourceKey<Level> dimension;
    private final BlockPos cornerA;
    private final BlockPos cornerB;
    private final BlockPos min;
    private final BlockPos max;
    private final UUID markerAId;
    private final UUID markerBId;

    public InkArena(UUID arenaId, int arenaNumber, ResourceKey<Level> dimension,
                    BlockPos cornerA, BlockPos cornerB,
                    UUID markerAId, UUID markerBId) {
        this.arenaId = arenaId;
        this.arenaNumber = arenaNumber;
        this.dimension = dimension;
        this.cornerA = cornerA;
        this.cornerB = cornerB;
        this.markerAId = markerAId;
        this.markerBId = markerBId;

        // 範囲計算を1か所に集約
        this.min = new BlockPos(
                Math.min(cornerA.getX(), cornerB.getX()),
                Math.min(cornerA.getY(), cornerB.getY()),
                Math.min(cornerA.getZ(), cornerB.getZ())
        );
        this.max = new BlockPos(
                Math.max(cornerA.getX(), cornerB.getX()),
                Math.max(cornerA.getY(), cornerB.getY()),
                Math.max(cornerA.getZ(), cornerB.getZ())
        );
    }

    public UUID getArenaId() {
        return arenaId;
    }

    public int getArenaNumber() {
        return arenaNumber;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getCornerA() {
        return cornerA;
    }

    public BlockPos getCornerB() {
        return cornerB;
    }

    public BlockPos getMin() {
        return min;
    }

    public BlockPos getMax() {
        return max;
    }

    public UUID getMarkerAId() {
        return markerAId;
    }

    public UUID getMarkerBId() {
        return markerBId;
    }

    /**
     * 指定された座標がアリーナ内に含まれるか判定する（両端 inclusive）。
     */
    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /**
     * 他のアリーナと重複しているか判定する。
     * inclusive座標で、1ブロックでも共有する場合は重複とみなす。
     */
    public boolean intersects(InkArena other) {
        return this.min.getX() <= other.max.getX()
                && this.max.getX() >= other.min.getX()
                && this.min.getY() <= other.max.getY()
                && this.max.getY() >= other.min.getY()
                && this.min.getZ() <= other.max.getZ()
                && this.max.getZ() >= other.min.getZ();
    }

    /**
     * 各辺のサイズを計算する（両端を含む）。
     */
    public int getSizeX() {
        return max.getX() - min.getX() + 1;
    }

    public int getSizeY() {
        return max.getY() - min.getY() + 1;
    }

    public int getSizeZ() {
        return max.getZ() - min.getZ() + 1;
    }

    /**
     * 体積を計算する（long でオーバーフロー防止）。
     */
    public long getVolume() {
        return (long) getSizeX() * getSizeY() * getSizeZ();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InkArena that)) return false;
        return arenaId.equals(that.arenaId);
    }

    @Override
    public int hashCode() {
        return arenaId.hashCode();
    }

    @Override
    public String toString() {
        return "InkArena{" +
                "arenaId=" + arenaId +
                ", arenaNumber=" + arenaNumber +
                ", dimension=" + dimension +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
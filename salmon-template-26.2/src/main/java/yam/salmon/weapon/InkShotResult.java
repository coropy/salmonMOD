package yam.salmon.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * インクシューターの1発の射撃結果。
 *
 * <p>sealed interface は使わず、record + enum の組み合わせで結果を表現する。
 * プロジェクトが Java 25+ であれば sealed も可能だが、互換性を考慮してこの方式をとる。</p>
 */
public final class InkShotResult {

    private InkShotResult() {}

    /** 結果種別 */
    public enum Type {
        /** 何にも当たらなかった（射程限界まで到達） */
        MISS,
        /** ブロックに命中 */
        BLOCK_HIT,
        /** Entityに命中 */
        ENTITY_HIT
    }

    /**
     * 何にも当たらなかった結果を生成する。
     */
    public static Result miss(Vec3 endPosition) {
        return new Result(Type.MISS, endPosition, null, null, -1, false);
    }

    /**
     * ブロック命中結果を生成する。
     */
    public static Result blockHit(BlockPos blockPos, Direction face, Vec3 hitPosition) {
        return new Result(Type.BLOCK_HIT, hitPosition, blockPos, face, -1, false);
    }

    /**
     * Entity命中結果を生成する。
     */
    public static Result entityHit(int entityId, Vec3 hitPosition, boolean damaged) {
        return new Result(Type.ENTITY_HIT, hitPosition, null, null, entityId, damaged);
    }

    /**
     * 射撃結果レコード。
     */
    public record Result(
            Type type,
            Vec3 endPosition,
            BlockPos blockPos,
            Direction face,
            int entityId,
            boolean damaged
    ) {
        public boolean isMiss() { return type == Type.MISS; }
        public boolean isBlockHit() { return type == Type.BLOCK_HIT; }
        public boolean isEntityHit() { return type == Type.ENTITY_HIT; }
    }
}
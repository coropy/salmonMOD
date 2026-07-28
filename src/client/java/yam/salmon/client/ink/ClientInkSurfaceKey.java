package yam.salmon.client.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import yam.salmon.ink.InkSurfacePatchId;

import java.util.UUID;

/**
 * クライアント側インク面の一意識別キー。
 * Phase 8: Patch ID を含むことで、同一ブロック内の異なるパッチ
 * （例：階段の下段踏面と上段踏面）を別々に保持できる。
 *
 * @param arenaId  アリーナUUID
 * @param blockPos ブロック座標
 * @param face     面方向（後方互換用・equals/hashCodeには使用しない）
 * @param patchId  Surface Patch ID（equals/hashCodeに使用）
 */
public record ClientInkSurfaceKey(
        UUID arenaId,
        BlockPos blockPos,
        Direction face,
        InkSurfacePatchId patchId
) {
    /**
     * 旧形式互換用：Patch IDから全フィールドを構築する。
     */
    public ClientInkSurfaceKey(UUID arenaId, BlockPos blockPos, InkSurfacePatchId patchId) {
        this(arenaId, blockPos, patchId.normal(), patchId);
    }

    /**
     * 旧形式からの移行用：DirectionからfullFaceパッチIDを生成する。
     */
    public static ClientInkSurfaceKey fromLegacy(UUID arenaId, BlockPos blockPos, Direction face) {
        return new ClientInkSurfaceKey(arenaId, blockPos, face,
                InkSurfacePatchId.fullFace(face));
    }
}
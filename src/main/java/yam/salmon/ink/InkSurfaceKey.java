package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 塗装面を一意に識別するキー。
 *
 * <p>Phase 7: Direction から InkSurfacePatchId に拡張。
 * フルキューブの各面は領域全体（0..16）の1パッチとして表現される。
 * 階段やハーフブロックは複数の部分パッチを持つ。</p>
 *
 * @param blockPos ブロック座標
 * @param patchId  ブロック内の表面パッチID
 */
public record InkSurfaceKey(
        BlockPos blockPos,
        InkSurfacePatchId patchId
) {
    /**
     * BlockPos.immutable() 相当で不変な参照を保持するコンストラクタ。
     * MC 26.2 の BlockPos は immutable なのでそのままOK。
     */
    public InkSurfaceKey {
        // BlockPos は immutable (Mojang mapping) なので防御コピー不要
    }

    /**
     * フルブロック面のキーを作成（後方互換用）。
     */
    public static InkSurfaceKey fullFace(BlockPos blockPos, Direction face) {
        return new InkSurfaceKey(blockPos, InkSurfacePatchId.fullFace(face));
    }

    /**
     * 法線方向を返す（ショートカット）。
     */
    public Direction getNormal() {
        return patchId.normal();
    }
}
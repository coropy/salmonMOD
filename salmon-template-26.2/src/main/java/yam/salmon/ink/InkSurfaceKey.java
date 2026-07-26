package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 塗装面を一意に識別するキー。
 * BlockPos と面方向の組み合わせ。
 */
public record InkSurfaceKey(
        BlockPos blockPos,
        Direction face
) {
    /**
     * BlockPos.immutable() 相当で不変な参照を保持するコンストラクタ。
     * MC 26.2 の BlockPos は immutable なのでそのままOK。
     */
    public InkSurfaceKey {
        // BlockPos は immutable (Mojang mapping) なので防御コピー不要
    }
}
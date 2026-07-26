package yam.salmon.client.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.UUID;

/**
 * クライアント側インク面の一意識別キー。
 *
 * @param arenaId  アリーナUUID
 * @param blockPos ブロック座標
 * @param face     面方向
 */
public record ClientInkSurfaceKey(
        UUID arenaId,
        BlockPos blockPos,
        Direction face
) {
}
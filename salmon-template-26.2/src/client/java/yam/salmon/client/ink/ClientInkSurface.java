package yam.salmon.client.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * クライアント側の1面インクデータ。
 *
 * @param arenaUuid   アリーナUUID
 * @param arenaNumber アリーナ番号
 * @param dimensionId ディメンション識別子
 * @param blockPos    ブロック座標
 * @param face        面方向
 * @param key         キャッシュキー
 * @param cells       64セル配列（防御コピー済み）
 * @param revision    リビジョン
 * @param teamACells  Team Aのセル数
 * @param teamBCells  Team Bのセル数
 */
public record ClientInkSurface(
        UUID arenaUuid,
        int arenaNumber,
        Identifier dimensionId,
        BlockPos blockPos,
        Direction face,
        ClientInkSurfaceKey key,
        byte[] cells,
        long revision,
        int teamACells,
        int teamBCells
) {
    /**
     * 指定セルのチーム値を取得する。
     * @param cellU 0..7
     * @param cellV 0..7
     * @return 0=NONE, 1=TEAM_A, 2=TEAM_B
     */
    public byte getCell(int cellU, int cellV) {
        int index = cellV * 8 + cellU;
        if (index < 0 || index >= cells.length) return 0;
        return cells[index];
    }

    /**
     * 面が実質的に空かどうか（全セルがNONE）。
     */
    public boolean isEmpty() {
        return teamACells == 0 && teamBCells == 0;
    }
}
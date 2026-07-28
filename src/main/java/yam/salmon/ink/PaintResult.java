package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 塗装操作の結果を表すレコード。
 *
 * @param success        塗装に成功したか
 * @param changedCells   変更されたセル数
 * @param arenaNumber    対象アリーナ番号（失敗時は0）
 * @param blockPos       対象ブロック座標（失敗時はnull可）
 * @param face           対象面（失敗時はnull可）
 * @param coordinates    セル座標情報（失敗時はnull可）
 * @param team           チーム値
 * @param failureReason  失敗理由（成功時はnull）
 */
public record PaintResult(
        boolean success,
        int changedCells,
        int arenaNumber,
        BlockPos blockPos,
        Direction face,
        InkFaceCoordinates coordinates,
        byte team,
        PaintFailureReason failureReason
) {
    /** 成功 */
    public static PaintResult success(int changedCells, int arenaNumber,
                                       BlockPos blockPos, Direction face,
                                       InkFaceCoordinates coordinates, byte team) {
        return new PaintResult(true, changedCells, arenaNumber,
                blockPos, face, coordinates, team, null);
    }

    /** 失敗 */
    public static PaintResult fail(PaintFailureReason reason) {
        return new PaintResult(false, 0, 0,
                null, null, null, InkTeam.NONE, reason);
    }

    /** 変更なし（同じチームですでに塗られている） */
    public static PaintResult noChange(int arenaNumber, BlockPos blockPos,
                                        Direction face, InkFaceCoordinates coordinates, byte team) {
        return new PaintResult(false, 0, arenaNumber,
                blockPos, face, coordinates, team, PaintFailureReason.NO_CHANGE);
    }
}
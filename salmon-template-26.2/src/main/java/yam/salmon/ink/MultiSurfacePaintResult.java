package yam.salmon.ink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * 複数面にまたがる塗装操作の結果。
 *
 * @param success             塗装に成功したか
 * @param changedSurfaceCount 変更された面数
 * @param changedCellCount    変更されたセル総数
 * @param updatedSurfaces     更新された面のリスト
 * @param failureReason       失敗理由（成功時はnull）
 */
public record MultiSurfacePaintResult(
        boolean success,
        int changedSurfaceCount,
        int changedCellCount,
        List<UpdatedInkSurface> updatedSurfaces,
        PaintFailureReason failureReason
) {
    public static MultiSurfacePaintResult success(int changedSurfaceCount, int changedCellCount,
                                                   List<UpdatedInkSurface> updatedSurfaces) {
        return new MultiSurfacePaintResult(true, changedSurfaceCount, changedCellCount,
                updatedSurfaces, null);
    }

    public static MultiSurfacePaintResult fail(PaintFailureReason reason) {
        return new MultiSurfacePaintResult(false, 0, 0, List.of(), reason);
    }

    /**
     * 1面の更新結果。
     *
     * @param blockPos     ブロック座標
     * @param face         面方向
     * @param cells        更新後のセル配列（防御コピー）
     * @param changedCells この面での変更セル数
     */
    public record UpdatedInkSurface(
            BlockPos blockPos,
            Direction face,
            byte[] cells,
            int changedCells
    ) {
        public static UpdatedInkSurface from(BlockPos blockPos, Direction face,
                                              InkFaceData faceData, int changedCells) {
            return new UpdatedInkSurface(blockPos, face, faceData.copyCells(), changedCells);
        }
    }
}
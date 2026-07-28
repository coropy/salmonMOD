package yam.salmon.ink;

import net.minecraft.core.Direction;

/**
 * パッチ平面上の1枚の矩形Quadを構成する4頂点と法線。
 *
 * <p>頂点順序は p00, p10, p11, p01（法線方向から見てCCW）。</p>
 *
 * @param p00    minU, minV の頂点
 * @param p10    maxU, minV の頂点
 * @param p11    maxU, maxV の頂点
 * @param p01    minU, maxV の頂点
 * @param normal 法線方向
 */
public record InkCellQuad(
        float p00x, float p00y, float p00z,
        float p10x, float p10y, float p10z,
        float p11x, float p11y, float p11z,
        float p01x, float p01y, float p01z,
        Direction normal
) {
    /** Quadの色情報付きバリアント */
    public record Colored(
            InkCellQuad quad,
            float r, float g, float b, float a
    ) {}
}
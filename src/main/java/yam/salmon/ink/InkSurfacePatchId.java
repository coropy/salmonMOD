package yam.salmon.ink;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Comparator;

/**
 * ブロック内部の矩形表面パッチを一意に識別するID。
 *
 * <p>1ブロック = 16 units の固定分解能で表現する。
 * 0.0→0, 0.5→8, 1.0→16 と対応する。</p>
 *
 * @param normal 面法線方向
 * @param plane  法線方向軸上の固定座標（0〜16）
 * @param minU   面上U座標の最小値（0〜16）
 * @param minV   面上V座標の最小値（0〜16）
 * @param maxU   面上U座標の最大値（0〜16）
 * @param maxV   面上V座標の最大値（0〜16）
 */
public record InkSurfacePatchId(
        Direction normal,
        int plane,
        int minU,
        int minV,
        int maxU,
        int maxV
) {
    /** ブロック内部の固定分解能 */
    public static final int BLOCK_RESOLUTION = 16;

    /** 全ブロック面を表すパッチID */
    public static InkSurfacePatchId fullFace(Direction face) {
        int plane = (face.getAxisDirection() == Direction.AxisDirection.POSITIVE)
                ? BLOCK_RESOLUTION : 0;
        return new InkSurfacePatchId(face, plane, 0, 0, BLOCK_RESOLUTION, BLOCK_RESOLUTION);
    }

    public InkSurfacePatchId {
        if (minU < 0 || minU > BLOCK_RESOLUTION) throw new IllegalArgumentException("minU out of range: " + minU);
        if (minV < 0 || minV > BLOCK_RESOLUTION) throw new IllegalArgumentException("minV out of range: " + minV);
        if (maxU < 0 || maxU > BLOCK_RESOLUTION) throw new IllegalArgumentException("maxU out of range: " + maxU);
        if (maxV < 0 || maxV > BLOCK_RESOLUTION) throw new IllegalArgumentException("maxV out of range: " + maxV);
        if (maxU <= minU) throw new IllegalArgumentException("maxU <= minU");
        if (maxV <= minV) throw new IllegalArgumentException("maxV <= minV");
        if (plane < 0 || plane > BLOCK_RESOLUTION) throw new IllegalArgumentException("plane out of range: " + plane);
    }

    /** パッチのU方向幅（units） */
    public int width() {
        return maxU - minU;
    }

    /** パッチのV方向高さ（units） */
    public int height() {
        return maxV - minV;
    }

    /** パッチが16×16の単位正方形全体をカバーするか */
    public boolean coversFullUnitSquare() {
        return minU == 0 && minV == 0
                && maxU == BLOCK_RESOLUTION && maxV == BLOCK_RESOLUTION;
    }

    /** パッチがブロック外周面か（planeが0または16） */
    public boolean isOuterBoundaryFace() {
        return plane == 0 || plane == BLOCK_RESOLUTION;
    }

    /** フルキューブの正準外周面か（rect 0..16 × 0..16 かつ plane=0/16） */
    public boolean isCanonicalFullCubeFace() {
        return coversFullUnitSquare() && isOuterBoundaryFace();
    }

    /**
     * @deprecated 意味が曖昧なため非推奨。
     *             {@link #coversFullUnitSquare()} または
     *             {@link #isCanonicalFullCubeFace()} を使用してください。
     */
    @Deprecated
    public boolean isFullFace() {
        return coversFullUnitSquare();
    }

    /**
     * このパッチIDが面上の指定されたUV座標を含むか。
     * @param u ブロックローカルU座標（0.0〜1.0, 範囲外も許容）
     * @param v ブロックローカルV座標（0.0〜1.0, 範囲外も許容）
     */
    public boolean containsUV(double u, double v) {
        int ui = (int) Math.round(u * BLOCK_RESOLUTION);
        int vi = (int) Math.round(v * BLOCK_RESOLUTION);
        return ui >= minU && ui <= maxU && vi >= minV && vi <= maxV;
    }

    /**
     * 指定された世界座標（ブロックローカル座標）の法線方向位置が
     * このパッチの平面上にあるか。
     */
    public boolean containsPlane(double localCoord) {
        int coord = (int) Math.round(localCoord * BLOCK_RESOLUTION);
        return coord >= plane - 1 && coord <= plane + 1; // 許容範囲
    }

    /** 面上のU座標をパッチローカルUV（0.0〜1.0）に変換 */
    public double toPatchU(double blockLocalU) {
        return (blockLocalU * BLOCK_RESOLUTION - minU) / (double) width();
    }

    /** 面上のV座標をパッチローカルUV（0.0〜1.0）に変換 */
    public double toPatchV(double blockLocalV) {
        return (blockLocalV * BLOCK_RESOLUTION - minV) / (double) height();
    }

    /** パッチローカルUVをブロック面上のU座標に変換 */
    public double toBlockU(double patchLocalU) {
        return (minU + patchLocalU * width()) / (double) BLOCK_RESOLUTION;
    }

    /** パッチローカルUVをブロック面上のV座標に変換 */
    public double toBlockV(double patchLocalV) {
        return (minV + patchLocalV * height()) / (double) BLOCK_RESOLUTION;
    }

    /**
     * 決定的ソート順。
     * normal ordinal → plane → minU → minV → maxU → maxV
     */
    public static Comparator<InkSurfacePatchId> comparator() {
        return Comparator
                .comparingInt(InkSurfacePatchId::normalOrdinal)
                .thenComparingInt(InkSurfacePatchId::plane)
                .thenComparingInt(InkSurfacePatchId::minU)
                .thenComparingInt(InkSurfacePatchId::minV)
                .thenComparingInt(InkSurfacePatchId::maxU)
                .thenComparingInt(InkSurfacePatchId::maxV);
    }

    private int normalOrdinal() {
        return normal.ordinal();
    }

    // ========================================================================
    // ネットワーク用シリアライズ
    // ========================================================================

    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeEnum(normal);
        buf.writeVarInt(plane);
        buf.writeVarInt(minU);
        buf.writeVarInt(minV);
        buf.writeVarInt(maxU);
        buf.writeVarInt(maxV);
    }

    public static InkSurfacePatchId readFromBuffer(FriendlyByteBuf buf) {
        Direction normal = buf.readEnum(Direction.class);
        int plane = buf.readVarInt();
        int minU = buf.readVarInt();
        int minV = buf.readVarInt();
        int maxU = buf.readVarInt();
        int maxV = buf.readVarInt();
        return new InkSurfacePatchId(normal, plane, minU, minV, maxU, maxV);
    }
}
package yam.salmon.weapon;

/**
 * トレイル塗装（弾道下へのインク滴）の設定。
 *
 * <p>主弾の軌道から小さなインク滴を下方向へ落とし、
 * 弾道の真下にある床や段差を小さく塗装する。</p>
 *
 * @param enabled                   トレイル塗装を有効にするか
 * @param sampleSpacing             軌道上で何ブロック進むごとに滴候補を生成するか
 * @param downwardRange             軌道位置から真下へ探索する最大距離（ブロック単位）
 * @param paintRadius               滴1個が床へ作る塗装半径（ブロック単位）
 * @param horizontalJitter          滴を落とす開始位置の横方向ランダムずれ（ブロック単位）
 * @param verticalStartOffset       軌道点から滴レイキャストを開始するときのY方向補正
 * @param minimumDistanceFromMuzzle 銃口直後からの最低距離（これ未満は滴生成しない）
 * @param minimumDistanceFromImpact 着弾点直前の最低距離（これ以内は滴生成しない）
 * @param maxTrailDropsPerShot      1発あたりの滴数上限
 * @param paintChance               候補地点ごとに実際に滴を生成する確率（0.0〜1.0）
 */
public record InkTrailPaintConfig(
        boolean enabled,
        double sampleSpacing,
        double downwardRange,
        double paintRadius,
        double horizontalJitter,
        double verticalStartOffset,
        double minimumDistanceFromMuzzle,
        double minimumDistanceFromImpact,
        int maxTrailDropsPerShot,
        double paintChance
) {
    /** 標準シューター用トレイル設定 */
    public static final InkTrailPaintConfig STANDARD = new InkTrailPaintConfig(
            true, 0.55, 2.25, 0.10, 0.04, 0.05, 0.75, 0.45, 10, 0.85
    );

    /** 短射程シューター用トレイル設定 */
    public static final InkTrailPaintConfig SHORT_RANGE = new InkTrailPaintConfig(
            true, 0.4, 1.5, 0.13, 0.07, 0.05, 0.5, 0.3, 10, 0.9
    );

    /** 長射程シューター用トレイル設定 */
    public static final InkTrailPaintConfig LONG_RANGE = new InkTrailPaintConfig(
            true, 0.8, 2.75, 0.07, 0.025, 0.05, 1.0, 0.6, 8, 0.75
    );

    /** トレイル塗装無効 */
    public static final InkTrailPaintConfig DISABLED = new InkTrailPaintConfig(
            false, 0.5, 2.0, 0.1, 0.04, 0.05, 0.5, 0.3, 0, 0.0
    );

    /** 1発あたりの安全上限 */
    public static final int MAX_SAFE_DROPS = 16;

    /** 描画用滴サイズ（主弾より小さい） */
    public static final float VISUAL_DROP_SIZE = 0.06f;

    /**
     * 設定値の妥当性を検証する。
     */
    public InkTrailPaintConfig {
        if (enabled) {
            if (sampleSpacing <= 0)
                throw new IllegalArgumentException("sampleSpacing must be > 0, got " + sampleSpacing);
            if (downwardRange <= 0)
                throw new IllegalArgumentException("downwardRange must be > 0, got " + downwardRange);
            if (paintRadius <= 0)
                throw new IllegalArgumentException("paintRadius must be > 0, got " + paintRadius);
            if (horizontalJitter < 0)
                throw new IllegalArgumentException("horizontalJitter must be >= 0, got " + horizontalJitter);
            if (minimumDistanceFromMuzzle < 0)
                throw new IllegalArgumentException("minimumDistanceFromMuzzle must be >= 0, got " + minimumDistanceFromMuzzle);
            if (minimumDistanceFromImpact < 0)
                throw new IllegalArgumentException("minimumDistanceFromImpact must be >= 0, got " + minimumDistanceFromImpact);
            if (maxTrailDropsPerShot < 0 || maxTrailDropsPerShot > MAX_SAFE_DROPS)
                throw new IllegalArgumentException("maxTrailDropsPerShot must be 0.." + MAX_SAFE_DROPS + ", got " + maxTrailDropsPerShot);
            if (paintChance < 0 || paintChance > 1.0)
                throw new IllegalArgumentException("paintChance must be 0.0..1.0, got " + paintChance);
        }
    }
}
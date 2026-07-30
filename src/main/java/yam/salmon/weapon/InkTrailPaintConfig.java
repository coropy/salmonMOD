package yam.salmon.weapon;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * トレイル塗装（弾道下へのインク滴）の設定。
 *
 * <p>主弾の軌道から小さなインク滴を下方向へ落とし、
 * 弾道の真下にある床や段差を小さく塗装する。</p>
 *
 * @param enabled                   トレイル塗装を有効にするか
 * @param minTrailDropSpacing       滴生成の最小間隔（ワールド距離、ブロック単位）
 * @param maxTrailDropSpacing       滴生成の最大間隔（ワールド距離、ブロック単位）
 * @param downwardRange             軌道位置から真下へ探索する最大距離（ブロック単位）
 * @param paintRadius               滴1個が床へ作る塗装半径（ブロック単位）
 * @param horizontalJitter          滴を落とす開始位置の横方向ランダムずれ（ブロック単位）
 * @param verticalStartOffset       軌道点から滴レイキャストを開始するときのY方向補正
 * @param minimumDistanceFromMuzzle 銃口直後からの最低距離（これ未満は滴生成しない）
 * @param minimumDistanceFromImpact 着弾点直前の最低距離（これ以内は滴生成しない）
 * @param maxTrailDropsPerShot      1発あたりの滴数上限
 * @param paintChance               候補地点ごとに実際に滴を生成する確率（0.0〜1.0）
 * @param visualDropSize            クライアント表示用滴サイズ（ブロック単位）
 */
public record InkTrailPaintConfig(
        boolean enabled,
        double minTrailDropSpacing,
        double maxTrailDropSpacing,
        double downwardRange,
        double paintRadius,
        double horizontalJitter,
        double verticalStartOffset,
        double minimumDistanceFromMuzzle,
        double minimumDistanceFromImpact,
        int maxTrailDropsPerShot,
        double paintChance,
        float visualDropSize
) {
    /** 標準シューター用トレイル設定（低頻度・大粒・ランダム間隔） */
    public static final InkTrailPaintConfig STANDARD = new InkTrailPaintConfig(
            true,   // enabled
            0.9,    // minTrailDropSpacing
            1.5,    // maxTrailDropSpacing
            2.5,    // downwardRange
            0.18,   // paintRadius
            0.06,   // horizontalJitter
            0.05,   // verticalStartOffset
            0.75,   // minimumDistanceFromMuzzle
            0.35,   // minimumDistanceFromImpact
            6,      // maxTrailDropsPerShot
            0.9,    // paintChance
            0.09f   // visualDropSize
    );

    /** 短射程シューター用トレイル設定 */
    public static final InkTrailPaintConfig SHORT_RANGE = new InkTrailPaintConfig(
            true,   // enabled
            0.7,    // minTrailDropSpacing
            1.2,    // maxTrailDropSpacing
            1.5,    // downwardRange
            0.22,   // paintRadius
            0.08,   // horizontalJitter
            0.05,   // verticalStartOffset
            0.5,    // minimumDistanceFromMuzzle
            0.3,    // minimumDistanceFromImpact
            5,      // maxTrailDropsPerShot
            0.9,    // paintChance
            0.11f   // visualDropSize
    );

    /** 長射程シューター用トレイル設定 */
    public static final InkTrailPaintConfig LONG_RANGE = new InkTrailPaintConfig(
            true,   // enabled
            1.1,    // minTrailDropSpacing
            1.7,    // maxTrailDropSpacing
            2.75,   // downwardRange
            0.12,   // paintRadius
            0.03,   // horizontalJitter
            0.05,   // verticalStartOffset
            1.0,    // minimumDistanceFromMuzzle
            0.5,    // minimumDistanceFromImpact
            7,      // maxTrailDropsPerShot
            0.75,   // paintChance
            0.06f   // visualDropSize
    );

    /** トレイル塗装無効 */
    public static final InkTrailPaintConfig DISABLED = new InkTrailPaintConfig(
            false,  // enabled
            0.5,    // minTrailDropSpacing
            1.0,    // maxTrailDropSpacing
            2.0,    // downwardRange
            0.1,    // paintRadius
            0.04,   // horizontalJitter
            0.05,   // verticalStartOffset
            0.5,    // minimumDistanceFromMuzzle
            0.3,    // minimumDistanceFromImpact
            0,      // maxTrailDropsPerShot
            0.0,    // paintChance
            0.06f   // visualDropSize
    );

    /** 1発あたりの安全上限 */
    public static final int MAX_SAFE_DROPS = 16;

    /**
     * 設定値の妥当性を検証する。
     */
    public InkTrailPaintConfig {
        if (enabled) {
            if (minTrailDropSpacing <= 0)
                throw new IllegalArgumentException("minTrailDropSpacing must be > 0, got " + minTrailDropSpacing);
            if (maxTrailDropSpacing < minTrailDropSpacing)
                throw new IllegalArgumentException("maxTrailDropSpacing must be >= minTrailDropSpacing, got " + maxTrailDropSpacing);
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
            if (visualDropSize <= 0)
                throw new IllegalArgumentException("visualDropSize must be > 0, got " + visualDropSize);
        }
    }

    /**
     * ランダムなサンプル間隔を生成する。
     *
     * @param random 乱数源
     * @return minTrailDropSpacing と maxTrailDropSpacing の間のランダム値
     */
    public double randomSpacing(RandomSource random) {
        return Mth.lerp(
                random.nextDouble(),
                minTrailDropSpacing,
                maxTrailDropSpacing
        );
    }
}
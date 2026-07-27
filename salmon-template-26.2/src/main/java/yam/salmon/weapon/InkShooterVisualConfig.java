package yam.salmon.weapon;

/**
 * インクシューターの視覚弾道設定。
 *
 * <p>全パラメータを1か所に集約し、見た目の調整を容易にする。</p>
 */
public record InkShooterVisualConfig(
        /** 視覚弾の移動速度（ブロック/tick） */
        double speedBlocksPerTick,

        /** 最小飛翔tick数 */
        int minTravelTicks,

        /** 最大飛翔tick数 */
        int maxTravelTicks,

        /** 主弾の描画サイズ（ブロック単位） */
        float projectileSize,

        /** 軌跡粒子の描画サイズ */
        float trailSize,

        /** 軌跡生成間隔（tick単位） */
        int trailIntervalTicks,

        /** 放物線の高さ（ブロック単位、弱め） */
        double arcHeight,

        /** 視覚弾の発射位置オフセット（視線前方） */
        double launchForwardOffset,

        /** 一人称視点での右方向オフセット */
        double firstPersonRightOffset,

        /** 一人称視点での下方向オフセット */
        double firstPersonDownOffset
) {
    public static final InkShooterVisualConfig DEFAULT = new InkShooterVisualConfig(
            5.0,    // speedBlocksPerTick
            1,      // minTravelTicks
            8,      // maxTravelTicks
            0.14f,  // projectileSize
            0.04f,  // trailSize
            1,      // trailIntervalTicks
            0.05,   // arcHeight
            0.8,    // launchForwardOffset
            0.25,   // firstPersonRightOffset
            0.20    // firstPersonDownOffset
    );
}
package yam.salmon.weapon;

/**
 * インクシューターの武器設定。
 *
 * <p>全パラメータを1か所に集約し、武器ごとの調整を容易にする。
 * 将来のフェーズで複数武器種を追加する際は、このレコードを継承または
 * 別レコードでオーバーライド可能。</p>
 */
public record InkShooterConfig(
        /** 発射間隔（tick単位）。3tick = 約0.15秒 */
        int fireIntervalTicks,

        /** 最大射程（ブロック単位） */
        double range,

        /** ダメージ量（ハート半分単位。2.0 = ハート1つ） */
        float damage,

        /** 塗装ブラシ半径（ブロック単位） */
        double brushRadius,

        /** 拡散角度（度数法）。0で完全にまっすぐ */
        double spreadDegrees,

        /** Entity貫通数（0=最初の1体で停止、未使用） */
        int maxPierceCount
) {
    /** デフォルト設定 */
    public static final InkShooterConfig DEFAULT = new InkShooterConfig(
            3,      // fireIntervalTicks
            28.0,   // range
            2.0f,   // damage
            0.25,   // brushRadius (8x8グリッドで直径約4セル相当)
            1.5,    // spreadDegrees
            0       // maxPierceCount
    );
}
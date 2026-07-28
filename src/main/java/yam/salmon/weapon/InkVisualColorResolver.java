package yam.salmon.weapon;

import net.minecraft.server.level.ServerPlayer;

/**
 * インク視覚表示の色解決。
 *
 * <p>現段階では Team A = 青、Team B = オレンジ の固定色を返す。
 * 将来のチーム色実装時に、このクラスを拡張する。</p>
 */
public final class InkVisualColorResolver {

    /** Team A 青 (ARGB) */
    public static final int COLOR_TEAM_A = 0xFF0C59FF;
    /** Team B オレンジ (ARGB) */
    public static final int COLOR_TEAM_B = 0xFFFF5905;
    /** デフォルト色 (Team A) */
    public static final int COLOR_DEFAULT = COLOR_TEAM_A;

    private InkVisualColorResolver() {}

    /**
     * シューター発射時の視覚弾の色を解決する。
     *
     * @param shooter 発射者
     * @return ARGB色
     */
    public static int resolveShotColor(ServerPlayer shooter) {
        // 現段階: Shift押下でTeam B、それ以外はTeam A
        // 将来はチーム所属に基づいて判定
        return shooter.isShiftKeyDown() ? COLOR_TEAM_B : COLOR_TEAM_A;
    }
}
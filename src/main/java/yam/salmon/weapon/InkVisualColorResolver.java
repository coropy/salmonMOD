package yam.salmon.weapon;

import net.minecraft.server.level.ServerPlayer;
import yam.salmon.ink.InkTeam;
import yam.salmon.team.TeamManager;

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
        // TEAM_B ならオレンジ、それ以外（TEAM_A / NONE）は青
        return TeamManager.getInstance().getTeam(shooter) == InkTeam.TEAM_B
                ? COLOR_TEAM_B
                : COLOR_TEAM_A;
    }
}
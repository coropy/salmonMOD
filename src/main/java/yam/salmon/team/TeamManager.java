package yam.salmon.team;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yam.salmon.Salmon;
import yam.salmon.ink.InkTeam;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーUUID → InkTeam byte値 のマッピングを管理するサーバー側シングルトン。
 *
 * <p>ゲーム管理・スコア・タイマー等の責務は持たない。
 * チーム割り当ての取得元を「isShiftKeyDown による仮判定」から
 * 正式なサーバー側マッピングへ置き換えるための最小クラス。</p>
 */
public final class TeamManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(Salmon.MOD_ID + ".team");

    private static final TeamManager INSTANCE = new TeamManager();

    /** プレイヤーUUID → InkTeam byte値 */
    private final Map<UUID, Byte> playerTeamMap = new ConcurrentHashMap<>();

    private TeamManager() {
    }

    public static TeamManager getInstance() {
        return INSTANCE;
    }

    /**
     * プレイヤーの現在のチームを返す。
     */
    public byte getTeam(ServerPlayer player) {
        return getTeam(player.getUUID());
    }

    /**
     * プレイヤーの現在のチームを返す。
     * 未登録の場合は {@link InkTeam#NONE} を返す（null は返さない）。
     */
    public byte getTeam(UUID playerId) {
        return playerTeamMap.getOrDefault(playerId, InkTeam.NONE);
    }

    /**
     * プレイヤーをチームへ割り当てる。
     * 無効なチーム値は無視する（ログのみ）。
     */
    public void assignTeam(ServerPlayer player, byte team) {
        if (!InkTeam.isValidTeam(team)) {
            LOGGER.warn("Ignoring invalid team assignment: player={} team={}",
                    player.getUUID(), team);
            return;
        }
        playerTeamMap.put(player.getUUID(), team);
        LOGGER.info("Assigned team for player: {} -> {}", player.getUUID(), InkTeam.toName(team));
    }

    /**
     * プレイヤーのチームマッピングを削除する。
     */
    public void removePlayer(UUID playerId) {
        if (playerTeamMap.remove(playerId) != null) {
            LOGGER.info("Removed team mapping for player: {}", playerId);
        }
    }

    /**
     * 全チームマッピングを消去する。
     */
    public void clear() {
        playerTeamMap.clear();
        LOGGER.info("Cleared all team mappings");
    }
}
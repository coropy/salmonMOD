package yam.salmon.state;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import yam.salmon.ink.InkStandingChecker;
import yam.salmon.ink.PlayerInkState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーごとのインク状態（{@link PlayerInkState}）をサーバー側で管理する。
 *
 * <p>毎サーバーtick、全プレイヤーを検査して状態を更新する。
 * 状態変化がない場合はマップへの書き込みを行わない（最小限の処理）。</p>
 *
 * <p>しゃがみ（イカ状態への遷移）はこのマネージャーで判定する。
 * {@link InkStandingChecker} は「自チームのインク上か」までを返すため、
 * ここで {@code ON_OWN_INK && player.isCrouching()} のときだけ
 * {@link PlayerInkState#CROUCHING_ON_OWN_INK} に変換する。</p>
 */
public final class PlayerInkStateManager {

    private static final PlayerInkStateManager INSTANCE = new PlayerInkStateManager();

    /** プレイヤーUUID → 現在のインク状態 */
    private final Map<UUID, PlayerInkState> states = new ConcurrentHashMap<>();

    private boolean registered = false;

    private PlayerInkStateManager() {}

    public static PlayerInkStateManager getInstance() {
        return INSTANCE;
    }

    /**
     * Fabricのサーバーtickイベントに登録する。
     * {@code yam.salmon.Salmon#onInitialize()} から1度だけ呼ぶこと。
     */
    public void register() {
        if (registered) return;
        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(this::tickServer);
    }

    /**
     * プレイヤーの現在のインク状態を返す。未登録は {@link PlayerInkState#NORMAL}。
     */
    public PlayerInkState getState(ServerPlayer player) {
        return getState(player.getUUID());
    }

    /**
     * プレイヤーの現在のインク状態を返す。未登録は {@link PlayerInkState#NORMAL}。
     */
    public PlayerInkState getState(UUID playerId) {
        return states.getOrDefault(playerId, PlayerInkState.NORMAL);
    }

    /**
     * プレイヤーの状態マッピングを削除する（切断時）。
     */
    public void removePlayer(UUID playerId) {
        states.remove(playerId);
    }

    /**
     * 全状態マッピングを消去する（サーバー停止時）。
     */
    public void clearAll() {
        states.clear();
    }

    private void tickServer(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private void tickPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerInkState previous = states.getOrDefault(playerId, PlayerInkState.NORMAL);

        // 自チームのインク上に立っているか
        PlayerInkState current = InkStandingChecker.determineState(player);

        // 自分のインク上でしゃがんでいる場合はイカ状態トリガーへ
        if (current == PlayerInkState.ON_OWN_INK && player.isCrouching()) {
            current = PlayerInkState.CROUCHING_ON_OWN_INK;
        }

        // 状態変化がない場合は何もしない
        if (current == previous) {
            return;
        }

        states.put(playerId, current);
    }
}
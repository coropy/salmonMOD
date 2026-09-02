package yam.salmon.state;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import yam.salmon.Salmon;
import yam.salmon.ink.InkStandingChecker;
import yam.salmon.ink.PlayerInkState;
import yam.salmon.network.InkPlayerStatePayload;

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
 *
 * <p>状態変更は本人だけでなく<strong>全接続クライアントへ配信</strong>する。
 * これにより他プレイヤーのクライアントでもイカ状態が参照できる。
 * また {@link #onPlayerJoin} では、参加者の状態を全員へ配信するとともに、
 * 既存プレイヤーの現在状態を参加者へ送る途中参加者の初期同期も行う。</p>
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
     * プレイヤー参加時に現在の状態を計算して全員へ配信し、
     * さらに既存プレイヤーの状態を参加者へ初期同期する。
     *
     * <p>未登録プレイヤーの初回tickでは状態変化がなく送信されないため、
     * ログイン直後に明示的に行う。</p>
     */
    public void onPlayerJoin(ServerPlayer player) {
        UUID playerId = player.getUUID();
        MinecraftServer server = player.level().getServer();

        PlayerInkState current = computeState(player);
        states.put(playerId, current);

        // 参加者自身の状態を全接続クライアントへ配信
        broadcastState(server, playerId, current);

        // 途中参加者の初期同期: 既存プレイヤーの現在状態を参加者へ送る
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            UUID otherId = other.getUUID();
            if (otherId.equals(playerId)) {
                continue;
            }
            PlayerInkState otherState = getState(otherId);
            if (otherState != PlayerInkState.NORMAL) {
                sendStateTo(player, otherId, otherState);
            }
        }

        Salmon.LOGGER.debug("Player ink state initial sync: player={} state={}",
                playerId, current);
    }

    /**
     * リスポーン・次元移動後に現在の状態を再計算し、全クライアントへ同期する。
     */
    public void onPlayerRespawn(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerInkState previous = states.getOrDefault(playerId, PlayerInkState.NORMAL);
        PlayerInkState current = computeState(player);

        states.put(playerId, current);
        broadcastState(player.level().getServer(), playerId, current);

        if (current != previous) {
            Salmon.LOGGER.debug("Player ink state changed on respawn: player={} {} -> {}",
                    playerId, previous, current);
        }
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
            tickPlayer(player, server);
        }
    }

    private void tickPlayer(ServerPlayer player, MinecraftServer server) {
        UUID playerId = player.getUUID();
        PlayerInkState previous = states.getOrDefault(playerId, PlayerInkState.NORMAL);
        PlayerInkState current = computeState(player);

        // イカ速度モディファイヤは状態変化の有無に関係なく毎tick冪等に付け外しする
        // （リスポーン・次元移動で属性がリセットされても次tickで自己修復される）
        SquidSpeedHandler.apply(player, current);

        if (current == previous) {
            return;
        }

        states.put(playerId, current);
        broadcastState(server, playerId, current);
        Salmon.LOGGER.debug("Player ink state changed: player={} {} -> {}",
                playerId, previous, current);
    }

    private static PlayerInkState computeState(ServerPlayer player) {
        PlayerInkState current = InkStandingChecker.determineState(player);
        if (current == PlayerInkState.ON_OWN_INK && player.isCrouching()) {
            current = PlayerInkState.CROUCHING_ON_OWN_INK;
        }
        return current;
    }

    /**
     * プレイヤーの状態変更を全接続クライアントへ配信する。
     */
    private static void broadcastState(MinecraftServer server, UUID playerId, PlayerInkState state) {
        InkPlayerStatePayload payload = new InkPlayerStatePayload(playerId, state);
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(target, payload);
        }
    }

    /**
     * 特定クライアントへ1プレイヤーの状態を送信する。
     */
    private static void sendStateTo(ServerPlayer target, UUID playerId, PlayerInkState state) {
        ServerPlayNetworking.send(target, new InkPlayerStatePayload(playerId, state));
    }
}

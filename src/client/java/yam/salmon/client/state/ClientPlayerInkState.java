package yam.salmon.client.state;

import yam.salmon.Salmon;
import yam.salmon.ink.PlayerInkState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアント側のプレイヤーインク状態キャッシュ。
 *
 * <p>サーバーから受信した {@link yam.salmon.network.InkPlayerStatePayload} を保持し、
 * 将来のイカモデル表示などの視覚効果で参照する。</p>
 */
public final class ClientPlayerInkState {

    private static final ClientPlayerInkState INSTANCE = new ClientPlayerInkState();

    private final Map<UUID, PlayerInkState> states = new ConcurrentHashMap<>();

    private ClientPlayerInkState() {}

    public static ClientPlayerInkState getInstance() {
        return INSTANCE;
    }

    /**
     * プレイヤーの現在のインク状態を返す。未受信は {@link PlayerInkState#NORMAL}。
     */
    public PlayerInkState get(UUID playerId) {
        return states.getOrDefault(playerId, PlayerInkState.NORMAL);
    }

    /**
     * サーバーから受信した状態を適用する。
     */
    public void apply(UUID playerId, PlayerInkState state) {
        states.put(playerId, state);
        Salmon.LOGGER.debug("[Client] Player ink state updated: player={} state={}", playerId, state);
    }

    /**
     * 指定プレイヤーの状態を削除する。
     */
    public void remove(UUID playerId) {
        states.remove(playerId);
    }

    /**
     * 全状態を消去する（切断時）。
     */
    public void clear() {
        states.clear();
    }
}

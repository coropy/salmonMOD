package yam.salmon.ink;

/**
 * プレイヤーのインク状態。
 *
 * <p>サーバー側で判定され、1プレイヤーにつき1つの状態を持たせる。
 * しゃがみ＋自チームのインク上という条件は将来の「イカ状態」のトリガーとして使う。</p>
 *
 * <ul>
 *   <li>{@link #NORMAL} - 通常状態</li>
 *   <li>{@link #ON_OWN_INK} - 自分のチームのインク上に立っている</li>
 *   <li>{@link #CROUCHING_ON_OWN_INK} - 自分のチームのインク上でしゃがんでいる</li>
 * </ul>
 */
public enum PlayerInkState {
    NORMAL,
    ON_OWN_INK,
    CROUCHING_ON_OWN_INK;

    /** ネットワーク送信用の安定ID（enum ordinal とは独立）。 */
    public byte toNetworkId() {
        return switch (this) {
            case NORMAL -> 0;
            case ON_OWN_INK -> 1;
            case CROUCHING_ON_OWN_INK -> 2;
        };
    }

    /**
     * ネットワーク受信用IDから状態を復元する。未知のIDは {@link #NORMAL}。
     */
    public static PlayerInkState fromNetworkId(byte id) {
        return switch (id) {
            case 1 -> ON_OWN_INK;
            case 2 -> CROUCHING_ON_OWN_INK;
            default -> NORMAL;
        };
    }

    public static byte toNetworkId(PlayerInkState state) {
        return state.toNetworkId();
    }
}
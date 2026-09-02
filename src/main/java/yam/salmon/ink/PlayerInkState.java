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
    CROUCHING_ON_OWN_INK
}
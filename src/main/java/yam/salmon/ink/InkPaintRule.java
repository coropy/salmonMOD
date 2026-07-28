package yam.salmon.ink;

/**
 * ブロック単位の塗装許可ルール。
 *
 * <p>将来のゲーム内設定（GUI、コマンド）で使用するための列挙型。
 * 現在はデフォルト判定のみ使用。</p>
 *
 * <p>優先順位（将来実装時）:
 * <ol>
 *   <li>ブロック単位の明示設定</li>
 *   <li>ブロックタグ単位の設定（ink_paintable / ink_unpaintable）</li>
 *   <li>デフォルト判定（形状・BlockEntity・液体等のヒューリスティック）</li>
 * </ol>
 */
public enum InkPaintRule {
    /** デフォルト判定に従う */
    DEFAULT,
    /** 常に塗装許可 */
    ALLOW,
    /** 常に塗装拒否 */
    DENY
}
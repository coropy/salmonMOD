package yam.salmon.ink;

/**
 * インクチーム定数。
 * byte値でセル状態を保持する。
 */
public final class InkTeam {
    public static final byte NONE = 0;
    public static final byte TEAM_A = 1;
    public static final byte TEAM_B = 2;

    /** セル表示用文字 */
    public static char toChar(byte team) {
        return switch (team) {
            case TEAM_A -> 'A';
            case TEAM_B -> 'B';
            default -> '.';
        };
    }

    /** チーム名（日本語） */
    public static String toName(byte team) {
        return switch (team) {
            case TEAM_A -> "Team A";
            case TEAM_B -> "Team B";
            default -> "NONE";
        };
    }

    /** 値が有効なチームか */
    public static boolean isValidTeam(byte team) {
        return team == TEAM_A || team == TEAM_B;
    }

    /** 無効な値をNONEに正規化 */
    public static byte normalize(byte value) {
        return isValidTeam(value) ? value : NONE;
    }

    private InkTeam() {}
}
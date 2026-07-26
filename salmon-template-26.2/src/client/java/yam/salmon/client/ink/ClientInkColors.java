package yam.salmon.client.ink;

/**
 * インク描画用の色定義。
 * クライアント側のみで使用する。
 */
public final class ClientInkColors {

    /** Team A: 青 */
    public static final float[] TEAM_A = { 0.05f, 0.35f, 1.0f, 0.92f };

    /** Team B: オレンジ */
    public static final float[] TEAM_B = { 1.0f, 0.35f, 0.02f, 0.92f };

    /**
     * チーム値に対応する色を返す。
     * @param team 0=NONE, 1=TEAM_A, 2=TEAM_B
     * @return float[4] = {r, g, b, a}、未塗装の場合はnull
     */
    public static float[] getColor(byte team) {
        return switch (team) {
            case 1 -> TEAM_A;
            case 2 -> TEAM_B;
            default -> null;
        };
    }

    private ClientInkColors() {}
}
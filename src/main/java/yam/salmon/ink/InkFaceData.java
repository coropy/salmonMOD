package yam.salmon.ink;

import java.util.Arrays;

/**
 * ブロック1面の16×16インクグリッドデータ。
 * 各セルは byte でチーム値を保持する。
 *
 * <p>内部配列は cellIndex = v * GRID_SIZE + u</p>
 */
public final class InkFaceData {
    public static final int GRID_SIZE = 16;
    public static final int CELL_COUNT = GRID_SIZE * GRID_SIZE;

    /**
     * セルと円の交差判定方式。
     */
    public enum IntersectionMode {
        /** セル中心点が円内にあるか判定（v3互換） */
        CELL_CENTER,
        /** セル矩形と円の交差判定（推奨 / Phase 4 既定） */
        CELL_RECTANGLE_INTERSECTION
    }

    /** デフォルトの交差判定方式 */
    public static final IntersectionMode DEFAULT_INTERSECTION_MODE =
            IntersectionMode.CELL_RECTANGLE_INTERSECTION;

    private final byte[] cells;

    /** 空の面データを作成（全セルNONE） */
    public InkFaceData() {
        this.cells = new byte[CELL_COUNT];
    }

    /** 既存のセル配列から作成（配列長チェック付き） */
    public InkFaceData(byte[] cells) {
        if (cells.length != CELL_COUNT) {
            this.cells = new byte[CELL_COUNT];
        } else {
            this.cells = new byte[CELL_COUNT];
            for (int i = 0; i < CELL_COUNT; i++) {
                this.cells[i] = InkTeam.normalize(cells[i]);
            }
        }
    }

    /**
     * セル値を取得する。
     * @param u 0..GRID_SIZE-1
     * @param v 0..GRID_SIZE-1
     */
    public byte getCell(int u, int v) {
        return cells[index(u, v)];
    }

    /**
     * セル値をインデックスで取得する。
     * @param index 0..CELL_COUNT-1
     */
    public byte getCellByIndex(int index) {
        if (index < 0 || index >= CELL_COUNT) return InkTeam.NONE;
        return cells[index];
    }

    /**
     * セル値を設定する。
     * @return 値が実際に変化した場合のみ true
     */
    public boolean setCell(int u, int v, byte team) {
        int idx = index(u, v);
        byte normalized = InkTeam.normalize(team);
        if (cells[idx] != normalized) {
            cells[idx] = normalized;
            return true;
        }
        return false;
    }

    /**
     * 円形塗装を行う。デフォルトの交差判定方式を使用する。
     *
     * <p>centerU, centerV は面ローカルUV座標で指定する。
     * 0.0〜1.0の範囲外も許容し、隣接ブロックへの塗装分配を可能にする。</p>
     *
     * @param centerU 中心 U（面ローカル / 範囲外可）
     * @param centerV 中心 V（面ローカル / 範囲外可）
     * @param radius  半径（UV座標系 / 0.25 で直径約4セル）
     * @param team    チーム値
     * @return 変更されたセル数
     */
    public int paintCircle(double centerU, double centerV, double radius, byte team) {
        return paintCircle(centerU, centerV, radius, team, DEFAULT_INTERSECTION_MODE);
    }

    /**
     * 円形塗装を行う（交差判定方式を指定）。
     *
     * <p>centerU, centerV は面ローカルUV座標で指定する。
     * 0.0〜1.0の範囲外も許容し、隣接ブロックへの塗装分配を可能にする。</p>
     *
     * @param centerU 中心 U（面ローカル / 範囲外可）
     * @param centerV 中心 V（面ローカル / 範囲外可）
     * @param radius  半径（UV座標系 / 0.25 で直径約4セル）
     * @param team    チーム値
     * @param mode    交差判定方式
     * @return 変更されたセル数
     */
    public int paintCircle(double centerU, double centerV, double radius, byte team,
                            IntersectionMode mode) {
        byte normalized = InkTeam.normalize(team);
        int changed = 0;

        double cellSize = 1.0 / GRID_SIZE;

        for (int v = 0; v < GRID_SIZE; v++) {
            for (int u = 0; u < GRID_SIZE; u++) {
                boolean intersects;

                if (mode == IntersectionMode.CELL_RECTANGLE_INTERSECTION) {
                    intersects = cellIntersectsCircle(u, v, centerU, centerV, radius, cellSize);
                } else {
                    // CELL_CENTER: セル中心点と円中心の距離
                    double cellCenterU = (u + 0.5) * cellSize;
                    double cellCenterV = (v + 0.5) * cellSize;
                    double du = cellCenterU - centerU;
                    double dv = cellCenterV - centerV;
                    intersects = du * du + dv * dv <= radius * radius;
                }

                if (intersects) {
                    int idx = index(u, v);
                    if (cells[idx] != normalized) {
                        cells[idx] = normalized;
                        changed++;
                    }
                }
            }
        }

        return changed;
    }

    /**
     * セル矩形が円と交差するか判定する。
     *
     * <p>円中心からセル矩形までの最近点距離を計算し、半径と比較する。
     * clamp(center, cellMin, cellMax) で矩形内の最近点を求め、
     * 円中心との距離の2乗が radius^2 以下なら交差。</p>
     */
    private static boolean cellIntersectsCircle(int cellU, int cellV,
                                                 double centerU, double centerV,
                                                 double radius, double cellSize) {
        double cellMinU = cellU * cellSize;
        double cellMaxU = (cellU + 1) * cellSize;
        double cellMinV = cellV * cellSize;
        double cellMaxV = (cellV + 1) * cellSize;

        // 円中心から矩形への最近点
        double nearestU = clamp(centerU, cellMinU, cellMaxU);
        double nearestV = clamp(centerV, cellMinV, cellMaxV);

        double du = centerU - nearestU;
        double dv = centerV - nearestV;

        return du * du + dv * dv <= radius * radius;
    }

    /**
     * value を [min, max] の範囲に clamp する。
     */
    static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * すべてのセルが NONE かどうかを判定する。
     */
    public boolean isEmpty() {
        for (byte cell : cells) {
            if (cell != InkTeam.NONE) return false;
        }
        return true;
    }

    /**
     * 全セルを NONE にリセットする。
     */
    public void clear() {
        Arrays.fill(cells, InkTeam.NONE);
    }

    /**
     * セル配列の防御コピーを返す。
     */
    public byte[] copyCells() {
        return Arrays.copyOf(cells, CELL_COUNT);
    }

    /**
     * 16×16の文字列表現を返す（デバッグ表示用）。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int v = 0; v < GRID_SIZE; v++) {
            for (int u = 0; u < GRID_SIZE; u++) {
                sb.append(InkTeam.toChar(cells[index(u, v)]));
            }
            if (v < GRID_SIZE - 1) sb.append('\n');
        }
        return sb.toString();
    }

    private static int index(int u, int v) {
        return v * GRID_SIZE + u;
    }
}
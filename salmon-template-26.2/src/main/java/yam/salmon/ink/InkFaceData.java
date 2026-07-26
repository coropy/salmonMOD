package yam.salmon.ink;

import java.util.Arrays;

/**
 * ブロック1面の8×8インクグリッドデータ。
 * 各セルは byte でチーム値を保持する。
 *
 * <p>内部配列は cellIndex = v * GRID_SIZE + u</p>
 */
public final class InkFaceData {
    public static final int GRID_SIZE = 8;
    public static final int CELL_COUNT = 64;

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
     * 円形塗装を行う。
     * 中心 (centerU, centerV) を UV座標 (0.0..1.0) で指定し、半径 radius 内のセルを塗る。
     *
     * @param centerU 中心 U (0.0..1.0)
     * @param centerV 中心 V (0.0..1.0)
     * @param radius 半径（UV座標系 / 0.24 で直径約4セル）
     * @param team チーム値
     * @return 変更されたセル数
     */
    public int paintCircle(double centerU, double centerV, double radius, byte team) {
        byte normalized = InkTeam.normalize(team);
        int changed = 0;

        // 各セルの中心座標を計算し、円内かどうか判定
        double cellSize = 1.0 / GRID_SIZE;
        for (int v = 0; v < GRID_SIZE; v++) {
            for (int u = 0; u < GRID_SIZE; u++) {
                double cellCenterU = (u + 0.5) * cellSize;
                double cellCenterV = (v + 0.5) * cellSize;

                double du = cellCenterU - centerU;
                double dv = cellCenterV - centerV;
                double dist = Math.sqrt(du * du + dv * dv);

                if (dist <= radius) {
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
     * 8×8の文字列表現を返す（デバッグ表示用）。
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
package minesweeper;

import java.util.List;

public class GameLogic {

    public static void initCloseMines(Grid grid) {
        for (int i = 0; i < grid.columns; i++) {
            for (int j = 0; j < grid.rows; j++) {
                computeCloseMines(grid, i, j);
            }
        }
    }

    public static void computeCloseMines(Grid grid, int col, int row) {
        List<int[]> adjacent = grid.getAdjacentCoords(col, row);
        int count = 0;
        for (int x = 1; x < adjacent.size(); x++) {
            int[] a = adjacent.get(x);
            if (grid.matrix[a[0]][a[1]].mine) {
                count++;
            }
        }
        grid.matrix[col][row].closeMines = count;
    }

    public static int getCloseFlags(Grid grid, int col, int row) {
        List<int[]> adjacent = grid.getAdjacentCoords(col, row);
        int close = 0;
        for (int x = 1; x < adjacent.size(); x++) {
            int[] a = adjacent.get(x);
            if (grid.matrix[a[0]][a[1]].flagged) {
                close++;
            }
        }
        return close;
    }

    public static boolean removeSquare(Grid grid, int[] square) {
        Square sq = grid.matrix[square[0]][square[1]];
        if (sq.removed || sq.flagged) return false;
        sq.removed = true;
        grid.numRemoved++;
        clearField(grid, square);
        return true;
    }

    public static void clearField(Grid grid, int[] square) {
        if (grid.matrix[square[0]][square[1]].closeMines != 0) return;
        List<int[]> adjacent = grid.getAdjacentCoords(square[0], square[1]);
        for (int[] a : adjacent) {
            if (!grid.matrix[a[0]][a[1]].flagged) {
                removeSquare(grid, a);
            }
        }
    }

    public static boolean allMinesFlagged(Grid grid) {
        for (int c = 0; c < grid.columns; c++)
            for (int r = 0; r < grid.rows; r++)
                if (grid.matrix[c][r].mine && !grid.matrix[c][r].flagged)
                    return false;
        return true;
    }

    public static boolean placeFlag(Grid grid, int[] square) {
        Square sq = grid.matrix[square[0]][square[1]];
        if (!sq.removed) {
            if (!sq.flagged) {
                if (grid.numFlags > 0) {
                    grid.numFlags--;
                    sq.flagged = true;
                    return true;
                }
            } else {
                grid.numFlags++;
                sq.flagged = false;
            }
        }
        return false;
    }
}

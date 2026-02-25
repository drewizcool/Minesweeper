package minesweeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

/**
 * Self-contained grid model. No dependency on Minesweeper statics.
 */
public class Grid {

    public static final int SQUARE_SIZE = 30;

    // DIFFICULTIES[difficulty] = {width, height, numMines}
    public static final int[][] DIFFICULTIES = {
        null,           // index 0 unused
        {240, 240, 10}, // 1: beginner 1
        {300, 300, 10}, // 2: beginner 2
        {360, 360, 15}, // 3: beginner 3
        {480, 480, 40}, // 4: intermediate 1
        {540, 540, 60}, // 5: intermediate 2
        {900, 480, 99}, // 6: expert
        {1800, 900, 375}, // 7: extreme
        {810, 420, 50}, // 8: test
        {1350, 600, 99} // 9: dungeon (45x20 = 900 squares, wider than tall)
    };

    public final int difficulty;
    public final int originX;
    public final int originY;
    public final int W;
    public final int H;
    public final int numSquares;
    public int numRemoved;
    public final int numMines;
    public int numFlags;
    public final int columns;
    public final int rows;

    // matrix[col][row] = Square
    public final Square[][] matrix;

    // The first square clicked (used to exclude from mine generation)
    public int[] firstSquare; // {col, row} or null

    // Optional seeded Random for deterministic testing
    private final Random rng;

    /** Convenience constructor using default origin. */
    public Grid(int difficulty) {
        this(difficulty, Minesweeper.ORIGIN_X, Minesweeper.ORIGIN_Y);
    }

    public Grid(int difficulty, int originX, int originY) {
        this(difficulty, originX, originY, null);
    }

    /** Constructor with explicit seed for deterministic testing. */
    public Grid(int difficulty, int originX, int originY, Long seed) {
        this.difficulty = difficulty;
        this.originX = originX;
        this.originY = originY;
        this.rng = (seed != null) ? new Random(seed) : new Random();
        int[] diff = DIFFICULTIES[difficulty];
        this.W = diff[0];
        this.H = diff[1];
        this.numMines = diff[2];
        this.columns = W / SQUARE_SIZE;
        this.rows = H / SQUARE_SIZE;
        this.numSquares = columns * rows;
        this.numRemoved = 0;
        this.numFlags = numMines;
        this.firstSquare = null;

        // Build matrix[col][row] with pixel coordinates
        matrix = new Square[columns][rows];
        for (int col = 0; col < columns; col++) {
            for (int row = 0; row < rows; row++) {
                int px = originX + col * SQUARE_SIZE;
                int py = originY + row * SQUARE_SIZE;
                matrix[col][row] = new Square(px, py);
            }
        }
    }

    /**
     * O(1) pixel-to-grid lookup. Returns {col, row} or null if outside the grid.
     */
    public int[] selectSquare(int mx, int my) {
        if (mx < originX || my < originY) return null;
        int col = (mx - originX) / SQUARE_SIZE;
        int row = (my - originY) / SQUARE_SIZE;
        if (col < 0 || col >= columns || row < 0 || row >= rows) return null;
        return new int[]{col, row};
    }

    /**
     * Self + up to 8 immediate neighbors.
     * adjacent[0] is always the square itself (column, row).
     */
    public List<int[]> getAdjacentCoords(int column, int row) {
        List<int[]> adjacent = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                if (i == 0 && j == 0) {
                    adjacent.add(new int[]{column, row});
                } else {
                    if (column + i < columns && row - j >= 0)
                        adjacent.add(new int[]{column + i, row - j});
                    if (column - i >= 0 && row + j < rows)
                        adjacent.add(new int[]{column - i, row + j});
                    if (i != 0 && j != 0) {
                        if (column + i < columns && row + j < rows)
                            adjacent.add(new int[]{column + i, row + j});
                        if (column - i >= 0 && row - j >= 0)
                            adjacent.add(new int[]{column - i, row - j});
                    }
                }
            }
        }
        return adjacent;
    }

    /**
     * Up to 3-ring of neighbors (used for first-square exclusion zone in Python,
     * and for getSurrounding in dungeon logic).
     */
    public List<int[]> getSurroundingCoords(int column, int row) {
        List<int[]> surrounding = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i == 0 && j == 0) {
                    surrounding.add(new int[]{column, row});
                } else {
                    if (column + i < columns && row - j >= 0)
                        surrounding.add(new int[]{column + i, row - j});
                    if (column - i >= 0 && row + j < rows)
                        surrounding.add(new int[]{column - i, row + j});
                    if (i != 0 && j != 0) {
                        if (column + i < columns && row + j < rows)
                            surrounding.add(new int[]{column + i, row + j});
                        if (column - i >= 0 && row - j >= 0)
                            surrounding.add(new int[]{column - i, row - j});
                    }
                }
            }
        }
        return surrounding;
    }

    public void generateMines() {
        Set<String> excluded = new HashSet<>();
        if (firstSquare != null) {
            List<int[]> adj = getAdjacentCoords(firstSquare[0], firstSquare[1]);
            for (int[] a : adj) {
                excluded.add(a[0] + "," + a[1]);
            }
        }

        int count = 0;
        while (count < numMines) {
            int x = rng.nextInt(columns);
            int y = rng.nextInt(rows);
            if (!matrix[x][y].mine && !excluded.contains(x + "," + y)) {
                count++;
                matrix[x][y].mine = true;
            }
        }
    }

    public void setFirstSquare(int col, int row) {
        this.firstSquare = new int[]{col, row};
    }

    public List<int[]> placeKeys(int count) {
        List<int[]> result = new ArrayList<>();
        int placed = 0;
        while (placed < count) {
            int col = rng.nextInt(columns);
            int row = rng.nextInt(rows);
            Square sq = matrix[col][row];
            if (!sq.mine && !sq.isStairs && !sq.isKey && sq.closeMines == 0) {
                sq.isKey = true;
                result.add(new int[]{col, row});
                placed++;
            }
        }
        return result;
    }

    public List<int[]> placeStairs(int count) {
        List<int[]> result = new ArrayList<>();
        int placed = 0;
        while (placed < count) {
            int col = rng.nextInt(columns);
            int row = rng.nextInt(rows);
            Square sq = matrix[col][row];
            if (!sq.mine && !sq.isStairs && sq.closeMines == 0) {
                sq.isStairs = true;
                result.add(new int[]{col, row});
                placed++;
            }
        }
        return result;
    }
}

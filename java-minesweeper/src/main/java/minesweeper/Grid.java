package minesweeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

/**
 * Direct port of the Python Grid class.
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
        {810, 420, 50}  // 8: test
    };

    public final int difficulty;
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

    // coordMatrix[col][row] = list of (x,y) pixel coords for that square
    // Used for binary search in selectSquare
    public final List<List<List<int[]>>> coordMatrix;

    // The first square clicked (used to exclude from mine generation)
    public int[] firstSquare; // {col, row} or null

    public Grid(int difficulty) {
        this.difficulty = difficulty;
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

        // Build the flat list of squares (same order as Python list comprehension)
        // Python: for x in range(ORIGIN[0], W+ORIGIN[0], squareSize)
        //         for y in range(ORIGIN[1], H+ORIGIN[1], squareSize)
        // Then matrix = squares[x:rows+x] for x in range(0, len, rows)
        // So matrix[col][row]
        List<Square> squares = new ArrayList<>();
        for (int x = Minesweeper.ORIGIN_X; x < W + Minesweeper.ORIGIN_X; x += SQUARE_SIZE) {
            for (int y = Minesweeper.ORIGIN_Y; y < H + Minesweeper.ORIGIN_Y; y += SQUARE_SIZE) {
                squares.add(new Square(x, y));
            }
        }

        // matrix[col][row]
        matrix = new Square[columns][rows];
        for (int col = 0; col < columns; col++) {
            for (int row = 0; row < rows; row++) {
                matrix[col][row] = squares.get(col * rows + row);
            }
        }

        // Build coordMatrix: for each square, list all 900 pixel coordinates
        // (30x30 pixels per square)
        coordMatrix = new ArrayList<>();
        for (int col = 0; col < columns; col++) {
            List<List<int[]>> colList = new ArrayList<>();
            for (int row = 0; row < rows; row++) {
                Square sq = matrix[col][row];
                List<int[]> coords = new ArrayList<>(900);
                for (int px = sq.x; px < sq.x + SQUARE_SIZE; px++) {
                    for (int py = sq.y; py < sq.y + SQUARE_SIZE; py++) {
                        coords.add(new int[]{px, py});
                    }
                }
                colList.add(coords);
            }
            coordMatrix.add(colList);
        }
    }

    public void generateMines() {
        Set<String> excluded = new HashSet<>();
        if (firstSquare != null) {
            List<int[]> adj = Minesweeper.getAdjacentCoords(firstSquare[0], firstSquare[1]);
            for (int[] a : adj) {
                excluded.add(a[0] + "," + a[1]);
            }
        }

        Random rng = new Random();
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
}

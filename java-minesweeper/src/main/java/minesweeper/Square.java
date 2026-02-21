package minesweeper;

/**
 * Replaces the Python 5-element list:
 *   square = [(x,y), False, False, False, 0]
 *   index:     [0]    [1]    [2]    [3]   [4]
 *   meaning:  coords removed flagged mine closeMines
 */
public class Square {
    // COORDINATES = 0
    public int x;
    public int y;

    // REMOVED = 1
    public boolean removed;

    // FLAGGED = 2
    public boolean flagged;

    // MINE = 3
    public boolean mine;

    // CLOSE_MINES = 4
    public int closeMines;

    public Square(int x, int y) {
        this.x = x;
        this.y = y;
        this.removed = false;
        this.flagged = false;
        this.mine = false;
        this.closeMines = 0;
    }
}

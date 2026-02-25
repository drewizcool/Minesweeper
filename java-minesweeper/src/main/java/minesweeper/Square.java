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

    // STAIRS — true when this square is a dungeon staircase
    public boolean isStairs = false;

    // KEY — true when this square is a key that unlocks the stairs on this floor
    public boolean isKey = false;

    // KEY_COLLECTED — true after the player clicks a revealed key to pick it up
    public boolean keyCollected = false;

    // STAIR_UNLOCKED — true after the player spends a key to unlock this stair
    public boolean stairUnlocked = false;

    // STAIR_MARKED — green flag marker on a revealed stair (does NOT affect numFlags or getCloseFlags)
    public boolean stairMarked = false;

    public Square(int x, int y) {
        this.x = x;
        this.y = y;
        this.removed = false;
        this.flagged = false;
        this.mine = false;
        this.closeMines = 0;
    }
}

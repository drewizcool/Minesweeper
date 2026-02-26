package minesweeper;

/**
 * Player character for dungeon mode. Moves freely in pixel space,
 * blocked by unrevealed squares (walls). Can only place flags within
 * a 2-cell Chebyshev radius.
 *
 * x,y is the top-left corner of the sprite (same coordinate system as Square).
 */
public class Player {

    public static final int SIZE = Grid.SQUARE_SIZE;

    public double x, y;
    public final int speed;
    public int facingDx = 0, facingDy = 1; // default facing down
    private final int originX, originY;

    public Player(double x, double y, int originX, int originY) {
        this.x = x;
        this.y = y;
        this.speed = 5;
        this.originX = originX;
        this.originY = originY;
    }

    /**
     * Move by dx*speed, dy*speed in pixel space.
     * Blocked if any corner of the sprite would land on an unrevealed square,
     * a locked stair, or outside the grid bounds.
     */
    public void move(int dx, int dy, Grid grid) {
        double newX = x + dx * speed;
        double newY = y + dy * speed;

        // Clamp to grid pixel bounds (entire sprite must stay inside)
        if (newX < grid.originX || newX + SIZE > grid.originX + grid.W
                || newY < grid.originY || newY + SIZE > grid.originY + grid.H) {
            return;
        }

        // Check all four corners of the sprite
        if (!isPassable(grid, (int) newX, (int) newY)
                || !isPassable(grid, (int) (newX + SIZE - 1), (int) newY)
                || !isPassable(grid, (int) newX, (int) (newY + SIZE - 1))
                || !isPassable(grid, (int) (newX + SIZE - 1), (int) (newY + SIZE - 1))) {
            return;
        }

        x = newX;
        y = newY;
    }

    private static boolean isPassable(Grid grid, int px, int py) {
        int[] cell = grid.selectSquare(px, py);
        if (cell == null) return false;
        Square sq = grid.matrix[cell[0]][cell[1]];
        if (!sq.removed) return false;
        if (sq.isStairs && !sq.stairUnlocked) return false;
        return true;
    }

    /** Which grid column the player's center is in. */
    public int gridCol() {
        return ((int) (x + SIZE / 2.0) - originX) / Grid.SQUARE_SIZE;
    }

    /** Which grid row the player's center is in. */
    public int gridRow() {
        return ((int) (y + SIZE / 2.0) - originY) / Grid.SQUARE_SIZE;
    }

    /**
     * True if the target cell is within a 2-cell Chebyshev distance
     * of the player's current grid cell.
     */
    public boolean canPlaceFlag(int col, int row) {
        int dc = Math.abs(gridCol() - col);
        int dr = Math.abs(gridRow() - row);
        return Math.max(dc, dr) <= 2;
    }
}

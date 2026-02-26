package minesweeper;

/**
 * Player character for dungeon mode. Moves freely in pixel space,
 * blocked by unrevealed squares (walls). Can only place flags within
 * a 2-cell Chebyshev radius.
 */
public class Player {

    public double x, y;
    public final int speed;
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
     * Blocked if the destination pixel lands on an unrevealed square or outside grid bounds.
     */
    public void move(int dx, int dy, Grid grid) {
        double newX = x + dx * speed;
        double newY = y + dy * speed;

        // Clamp to grid pixel bounds
        double minX = grid.originX;
        double minY = grid.originY;
        double maxX = grid.originX + grid.W - 1;
        double maxY = grid.originY + grid.H - 1;

        if (newX < minX || newX > maxX || newY < minY || newY > maxY) {
            return;
        }

        // Check if destination cell is revealed
        int[] cell = grid.selectSquare((int) newX, (int) newY);
        if (cell == null) {
            return;
        }
        if (!grid.matrix[cell[0]][cell[1]].removed) {
            return;
        }

        x = newX;
        y = newY;
    }

    /** Which grid column the player is currently in. */
    public int gridCol() {
        return ((int) x - originX) / Grid.SQUARE_SIZE;
    }

    /** Which grid row the player is currently in. */
    public int gridRow() {
        return ((int) y - originY) / Grid.SQUARE_SIZE;
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

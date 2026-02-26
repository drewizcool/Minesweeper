package minesweeper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    // Difficulty 1 = 8x8 grid, SQUARE_SIZE = 30
    private static final int ORIGIN_X = 20;
    private static final int ORIGIN_Y = 90;

    /**
     * Create a small grid with a revealed 4x4 region for player movement tests.
     */
    private Grid makeGrid() {
        Grid g = new Grid(1, ORIGIN_X, ORIGIN_Y);
        for (int c = 0; c < 4; c++)
            for (int r = 0; r < 4; r++)
                g.matrix[c][r].removed = true;
        return g;
    }

    /** Player at the pixel center of grid cell (1,1). */
    private Player makePlayer() {
        double px = ORIGIN_X + 1 * 30 + 15;
        double py = ORIGIN_Y + 1 * 30 + 15;
        return new Player(px, py, ORIGIN_X, ORIGIN_Y);
    }

    // --- Movement basics ---

    @Test
    void move_right_increasesX() {
        Grid g = makeGrid();
        Player p = makePlayer();
        double before = p.x;
        p.move(1, 0, g);
        assertEquals(before + p.speed, p.x);
    }

    @Test
    void move_down_increasesY() {
        Grid g = makeGrid();
        Player p = makePlayer();
        double before = p.y;
        p.move(0, 1, g);
        assertEquals(before + p.speed, p.y);
    }

    @Test
    void move_left_decreasesX() {
        Grid g = makeGrid();
        Player p = makePlayer();
        double before = p.x;
        p.move(-1, 0, g);
        assertEquals(before - p.speed, p.x);
    }

    @Test
    void move_up_decreasesY() {
        Grid g = makeGrid();
        Player p = makePlayer();
        double before = p.y;
        p.move(0, -1, g);
        assertEquals(before - p.speed, p.y);
    }

    // --- Boundary enforcement ---

    @Test
    void move_blockedByUnrevealedSquare() {
        Grid g = makeGrid();
        // Place player at right edge of revealed region: center of cell (3,1)
        // Cell (4,1) is unrevealed (wall)
        double px = ORIGIN_X + 3 * 30 + 29; // right edge of cell (3,1)
        double py = ORIGIN_Y + 1 * 30 + 15;
        Player p = new Player(px, py, ORIGIN_X, ORIGIN_Y);
        double beforeX = p.x;
        p.move(1, 0, g); // try to move right into unrevealed cell (4,1)
        assertEquals(beforeX, p.x, "Player should be blocked by unrevealed square");
    }

    @Test
    void move_allowedOnRevealedSquare() {
        Grid g = makeGrid();
        // Player in center of (1,1), moving right stays in revealed area
        Player p = makePlayer();
        double before = p.x;
        p.move(1, 0, g);
        assertNotEquals(before, p.x, "Player should move into revealed square");
    }

    @Test
    void move_clampedToGridBounds() {
        Grid g = makeGrid();
        // Place player at top-left corner of grid
        double px = ORIGIN_X;
        double py = ORIGIN_Y;
        Player p = new Player(px, py, ORIGIN_X, ORIGIN_Y);
        p.move(-1, 0, g); // try to move left, out of grid
        assertEquals(px, p.x, "Player should not move outside grid bounds");
        p.move(0, -1, g); // try to move up, out of grid
        assertEquals(py, p.y, "Player should not move outside grid bounds");
    }

    // --- Flag placement radius ---

    @Test
    void canPlaceFlag_withinRadius_returnsTrue() {
        Player p = makePlayer(); // at cell (1,1)
        assertTrue(p.canPlaceFlag(2, 2), "1 cell diagonal should be in radius");
        assertTrue(p.canPlaceFlag(3, 1), "2 cells right should be in radius");
        assertTrue(p.canPlaceFlag(1, 3), "2 cells down should be in radius");
        assertTrue(p.canPlaceFlag(3, 3), "2 cells diagonal should be in radius");
    }

    @Test
    void canPlaceFlag_outsideRadius_returnsFalse() {
        Player p = makePlayer(); // at cell (1,1)
        assertFalse(p.canPlaceFlag(4, 1), "3 cells right should be out of radius");
        assertFalse(p.canPlaceFlag(1, 4), "3 cells down should be out of radius");
        assertFalse(p.canPlaceFlag(4, 4), "3 cells diagonal should be out of radius");
    }

    @Test
    void canPlaceFlag_atPlayerSquare_returnsTrue() {
        Player p = makePlayer(); // at cell (1,1)
        assertTrue(p.canPlaceFlag(1, 1), "Player's own cell should be in radius");
    }
}

package minesweeper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    // Difficulty 1 = 8x8 grid, SQUARE_SIZE = 30
    private static final int ORIGIN_X = 20;
    private static final int ORIGIN_Y = 90;
    private static final int SQ = Grid.SQUARE_SIZE; // 30

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

    /** Player at the top-left of grid cell (1,1). */
    private Player makePlayer() {
        double px = ORIGIN_X + 1 * SQ;
        double py = ORIGIN_Y + 1 * SQ;
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
        // Place player at top-left of cell (3,1). Sprite right edge = 3*30+30 = cell boundary.
        // Moving right by 5 would push right edge into unrevealed cell (4,1).
        double px = ORIGIN_X + 3 * SQ;
        double py = ORIGIN_Y + 1 * SQ;
        Player p = new Player(px, py, ORIGIN_X, ORIGIN_Y);
        double beforeX = p.x;
        p.move(1, 0, g); // try to move right into unrevealed cell (4,1)
        assertEquals(beforeX, p.x, "Player should be blocked by unrevealed square");
    }

    @Test
    void move_allowedOnRevealedSquare() {
        Grid g = makeGrid();
        Player p = makePlayer(); // at cell (1,1), moving right stays in revealed area
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
        Player p = makePlayer(); // top-left at cell (1,1), center in cell (1,1)
        assertTrue(p.canPlaceFlag(2, 2), "1 cell diagonal should be in radius");
        assertTrue(p.canPlaceFlag(3, 1), "2 cells right should be in radius");
        assertTrue(p.canPlaceFlag(1, 3), "2 cells down should be in radius");
        assertTrue(p.canPlaceFlag(3, 3), "2 cells diagonal should be in radius");
    }

    @Test
    void canPlaceFlag_outsideRadius_returnsFalse() {
        Player p = makePlayer(); // top-left at cell (1,1), center in cell (1,1)
        assertFalse(p.canPlaceFlag(4, 1), "3 cells right should be out of radius");
        assertFalse(p.canPlaceFlag(1, 4), "3 cells down should be out of radius");
        assertFalse(p.canPlaceFlag(4, 4), "3 cells diagonal should be out of radius");
    }

    @Test
    void canPlaceFlag_atPlayerSquare_returnsTrue() {
        Player p = makePlayer(); // top-left at cell (1,1)
        assertTrue(p.canPlaceFlag(1, 1), "Player's own cell should be in radius");
    }

    @Test
    void move_blockedByLockedStair() {
        Grid g = makeGrid();
        // Cell (2,1) is revealed but has a locked stair
        Square sq = g.matrix[2][1];
        sq.isStairs = true;
        sq.stairUnlocked = false;
        // sq.removed is already true from makeGrid()

        // Place player flush against right edge: top-left at cell boundary minus 1 pixel
        // so sprite right edge just touches cell (2,1)
        double px = ORIGIN_X + 2 * SQ - SQ; // = cell (1,1) top-left
        double py = ORIGIN_Y + 1 * SQ;
        Player p = new Player(px, py, ORIGIN_X, ORIGIN_Y);
        // Sprite occupies exactly cell (1,1). Moving right by speed=5 would
        // push right edge into cell (2,1) which is a locked stair.
        double beforeX = p.x;
        p.move(1, 0, g); // try to move right into locked stair
        assertEquals(beforeX, p.x, "Player should be blocked by locked stair");
    }
}

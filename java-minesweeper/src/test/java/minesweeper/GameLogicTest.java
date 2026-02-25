package minesweeper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameLogicTest {

    private Grid makeSmallGrid() {
        Grid g = new Grid(1, 20, 90, 123L);
        g.setFirstSquare(0, 0);
        g.generateMines();
        GameLogic.initCloseMines(g);
        return g;
    }

    @Test
    void initCloseMines_cornerWithOneMineNeighbor() {
        Grid g = new Grid(1, 20, 90);
        g.matrix[1][0].mine = true;
        GameLogic.initCloseMines(g);
        assertEquals(1, g.matrix[0][0].closeMines);
    }

    @Test
    void initCloseMines_noMines_allZero() {
        Grid g = new Grid(1, 20, 90);
        GameLogic.initCloseMines(g);
        for (int c = 0; c < g.columns; c++)
            for (int r = 0; r < g.rows; r++)
                assertEquals(0, g.matrix[c][r].closeMines);
    }

    @Test
    void computeCloseMines_countsSurroundingMines() {
        Grid g = new Grid(1, 20, 90);
        g.matrix[0][0].mine = true;
        g.matrix[2][0].mine = true;
        g.matrix[1][1].mine = true;
        GameLogic.computeCloseMines(g, 1, 0);
        assertEquals(3, g.matrix[1][0].closeMines); // self not mine, 3 adjacent mines
    }

    @Test
    void removeSquare_flaggedSquare_returnsFalse() {
        Grid g = makeSmallGrid();
        g.matrix[3][3].flagged = true;
        assertFalse(GameLogic.removeSquare(g, new int[]{3, 3}));
    }

    @Test
    void removeSquare_alreadyRemoved_returnsFalse() {
        Grid g = makeSmallGrid();
        g.matrix[3][3].removed = true;
        int before = g.numRemoved;
        assertFalse(GameLogic.removeSquare(g, new int[]{3, 3}));
        assertEquals(before, g.numRemoved);
    }

    @Test
    void removeSquare_normalSquare_returnsTrue() {
        Grid g = makeSmallGrid();
        // Find a non-mine square
        int[] target = null;
        for (int c = 0; c < g.columns && target == null; c++)
            for (int r = 0; r < g.rows && target == null; r++)
                if (!g.matrix[c][r].mine && g.matrix[c][r].closeMines > 0)
                    target = new int[]{c, r};
        assertNotNull(target);
        assertTrue(GameLogic.removeSquare(g, target));
        assertTrue(g.matrix[target[0]][target[1]].removed);
    }

    @Test
    void clearField_floodFillsAllZeros() {
        Grid g = new Grid(1, 20, 90);
        // No mines at all -> every square has closeMines=0
        GameLogic.initCloseMines(g);
        GameLogic.removeSquare(g, new int[]{4, 4});
        assertEquals(g.numSquares, g.numRemoved);
    }

    @Test
    void placeFlag_reducesNumFlags() {
        Grid g = makeSmallGrid();
        int before = g.numFlags;
        GameLogic.placeFlag(g, new int[]{2, 2});
        assertEquals(before - 1, g.numFlags);
        assertTrue(g.matrix[2][2].flagged);
    }

    @Test
    void placeFlag_unflag_restoresNumFlags() {
        Grid g = makeSmallGrid();
        int before = g.numFlags;
        GameLogic.placeFlag(g, new int[]{2, 2});
        GameLogic.placeFlag(g, new int[]{2, 2});
        assertEquals(before, g.numFlags);
        assertFalse(g.matrix[2][2].flagged);
    }

    @Test
    void placeFlag_onRemovedSquare_returnsFalse() {
        Grid g = makeSmallGrid();
        g.matrix[2][2].removed = true;
        assertFalse(GameLogic.placeFlag(g, new int[]{2, 2}));
    }

    @Test
    void placeFlag_noFlagsLeft_returnsFalse() {
        Grid g = makeSmallGrid();
        g.numFlags = 0;
        assertFalse(GameLogic.placeFlag(g, new int[]{2, 2}));
        assertFalse(g.matrix[2][2].flagged);
    }

    @Test
    void getCloseFlags_countsCorrectly() {
        Grid g = new Grid(1, 20, 90);
        g.matrix[1][0].flagged = true;
        g.matrix[0][1].flagged = true;
        assertEquals(2, GameLogic.getCloseFlags(g, 0, 0));
    }

    @Test
    void getCloseFlags_noFlags_returnsZero() {
        Grid g = new Grid(1, 20, 90);
        assertEquals(0, GameLogic.getCloseFlags(g, 4, 4));
    }

    @Test
    void allMinesFlagged_noMines_returnsTrue() {
        Grid g = new Grid(1, 20, 90);
        assertTrue(GameLogic.allMinesFlagged(g));
    }

    @Test
    void allMinesFlagged_unflaggedMine_returnsFalse() {
        Grid g = new Grid(1, 20, 90);
        g.matrix[0][0].mine = true;
        assertFalse(GameLogic.allMinesFlagged(g));
    }

    @Test
    void allMinesFlagged_allFlagged_returnsTrue() {
        Grid g = new Grid(1, 20, 90);
        g.matrix[0][0].mine = true;
        g.matrix[0][0].flagged = true;
        g.matrix[2][3].mine = true;
        g.matrix[2][3].flagged = true;
        assertTrue(GameLogic.allMinesFlagged(g));
    }

    @Test
    void stairMarked_doesNotAffectCloseFlags() {
        Grid g = new Grid(1, 20, 90);
        g.matrix[1][0].isStairs = true;
        g.matrix[1][0].stairMarked = true;
        // stairMarked should NOT be counted by getCloseFlags
        assertEquals(0, GameLogic.getCloseFlags(g, 0, 0));
    }
}

package minesweeper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GridTest {

    @Test
    void adjacentCoordsOfCenter_returns9() {
        Grid g = new Grid(3, 20, 90);
        // grid is 12x12; center square (5,5) should have 9 adjacent (self + 8 neighbors)
        List<int[]> adj = g.getAdjacentCoords(5, 5);
        assertEquals(9, adj.size());
        assertArrayEquals(new int[]{5, 5}, adj.get(0));
    }

    @Test
    void adjacentCoordsOfCorner_returns4() {
        Grid g = new Grid(1, 20, 90);
        // (0,0) corner: self + 3 neighbors
        List<int[]> adj = g.getAdjacentCoords(0, 0);
        assertEquals(4, adj.size());
        assertArrayEquals(new int[]{0, 0}, adj.get(0));
    }

    @Test
    void adjacentCoordsOfEdge_returns6() {
        Grid g = new Grid(3, 20, 90);
        // top edge, non-corner: (3,0) has self + 5 neighbors
        List<int[]> adj = g.getAdjacentCoords(3, 0);
        assertEquals(6, adj.size());
        assertArrayEquals(new int[]{3, 0}, adj.get(0));
    }

    @Test
    void surroundingCoords_largerThanAdjacent() {
        Grid g = new Grid(3, 20, 90);
        List<int[]> surr = g.getSurroundingCoords(6, 6);
        List<int[]> adj = g.getAdjacentCoords(6, 6);
        assertTrue(surr.size() > adj.size());
    }

    @Test
    void selectSquare_pixelInGrid_returnsCorrectCell() {
        Grid g = new Grid(1, 20, 90);
        // Square (0,0) occupies pixels x:[20..49], y:[90..119]
        int[] result = g.selectSquare(25, 95);
        assertNotNull(result);
        assertArrayEquals(new int[]{0, 0}, result);
    }

    @Test
    void selectSquare_lastSquare() {
        Grid g = new Grid(1, 20, 90); // 8x8
        // Square (7,7) starts at x:20+7*30=230, y:90+7*30=300
        int[] result = g.selectSquare(230, 300);
        assertNotNull(result);
        assertArrayEquals(new int[]{7, 7}, result);
    }

    @Test
    void selectSquare_pixelOutsideGrid_returnsNull() {
        Grid g = new Grid(1, 20, 90);
        assertNull(g.selectSquare(5, 5));
    }

    @Test
    void selectSquare_pixelBelowGrid_returnsNull() {
        Grid g = new Grid(1, 20, 90); // 8x8, H=240
        assertNull(g.selectSquare(25, 90 + 240 + 1));
    }

    @Test
    void selectSquare_pixelRightOfGrid_returnsNull() {
        Grid g = new Grid(1, 20, 90); // W=240
        assertNull(g.selectSquare(20 + 240 + 1, 95));
    }

    @Test
    void generateMines_withSeed_isDeterministic() {
        Grid g1 = new Grid(3, 20, 90, 42L);
        g1.setFirstSquare(0, 0);
        g1.generateMines();
        Grid g2 = new Grid(3, 20, 90, 42L);
        g2.setFirstSquare(0, 0);
        g2.generateMines();
        for (int c = 0; c < g1.columns; c++)
            for (int r = 0; r < g1.rows; r++)
                assertEquals(g1.matrix[c][r].mine, g2.matrix[c][r].mine,
                    "Mine mismatch at " + c + "," + r);
    }

    @Test
    void generateMines_excludesFirstSquareNeighborhood() {
        Grid g = new Grid(1, 20, 90, 42L); // 8x8, 10 mines
        g.setFirstSquare(4, 4);
        g.generateMines();
        List<int[]> adj = g.getAdjacentCoords(4, 4);
        for (int[] a : adj) {
            assertFalse(g.matrix[a[0]][a[1]].mine,
                "Mine found in first-click exclusion zone at " + a[0] + "," + a[1]);
        }
    }

    @Test
    void generateMines_correctCount() {
        Grid g = new Grid(3, 20, 90, 99L);
        g.setFirstSquare(0, 0);
        g.generateMines();
        int mineCount = 0;
        for (int c = 0; c < g.columns; c++)
            for (int r = 0; r < g.rows; r++)
                if (g.matrix[c][r].mine) mineCount++;
        assertEquals(g.numMines, mineCount);
    }

    @Test
    void squarePixelCoordinates_matchOrigin() {
        Grid g = new Grid(1, 100, 200);
        assertEquals(100, g.matrix[0][0].x);
        assertEquals(200, g.matrix[0][0].y);
        assertEquals(100 + 30, g.matrix[1][0].x);
        assertEquals(200 + 30, g.matrix[0][1].y);
    }
}

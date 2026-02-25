package minesweeper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void reset_clearsDungeonState() {
        Grid g = new Grid(9, 20, 90);
        GameState state = new GameState(g);
        state.dungeonMode = true;
        state.dungeonFloorDepth = 5;
        state.keysFound = 3;
        state.keysNeeded = 2;
        state.currentFloorIsDead = true;
        state.deadEndDepth = 3;
        state.newFloorPlaced = true;
        state.liveRoomDepth = 7;
        state.dungeonStack.push(new DungeonFloor(g, state.subFloorDifficulty,
                true, true, null, false,
                new java.util.HashMap<>(), 1L,
                1, false, 0, false, 0, 0, 1));

        state.reset(new Grid(3, 20, 90));
        assertFalse(state.dungeonMode);
        assertEquals(0, state.keysFound);
        assertEquals(0, state.keysNeeded);
        assertTrue(state.dungeonStack.isEmpty());
        assertEquals(1, state.dungeonFloorDepth);
        assertFalse(state.currentFloorIsDead);
        assertEquals(0, state.deadEndDepth);
        assertFalse(state.newFloorPlaced);
        assertEquals(0, state.liveRoomDepth);
        assertTrue(state.playing);
        assertFalse(state.generated);
        assertNull(state.exploded);
        assertFalse(state.win);
    }

    @Test
    void reset_setsDungeonModeForDifficulty9() {
        GameState state = new GameState(new Grid(3, 20, 90));
        state.reset(new Grid(9, 20, 90));
        assertTrue(state.dungeonMode);
    }

    @Test
    void checkWinCondition_allNonMinesRemoved() {
        Grid g = new Grid(1, 20, 90);
        GameState state = new GameState(g);
        g.numRemoved = g.numSquares - g.numMines;
        assertTrue(state.checkWinCondition());
    }

    @Test
    void checkWinCondition_notAllRemoved() {
        Grid g = new Grid(1, 20, 90);
        GameState state = new GameState(g);
        g.numRemoved = 0;
        assertFalse(state.checkWinCondition());
    }

    @Test
    void checkWinCondition_dungeonMode_alwaysFalse() {
        Grid g = new Grid(1, 20, 90, 42L);
        g.setFirstSquare(0, 0);
        g.generateMines();
        GameLogic.initCloseMines(g);
        GameState state = new GameState(g);
        state.dungeonMode = true;
        // Even with all mines flagged, checkWinCondition returns false in dungeon mode
        for (int c = 0; c < g.columns; c++)
            for (int r = 0; r < g.rows; r++)
                if (g.matrix[c][r].mine) g.matrix[c][r].flagged = true;
        assertFalse(state.checkWinCondition());
    }

    @Test
    void checkDungeonRoomCleared_allMinesFlagged() {
        Grid g = new Grid(1, 20, 90, 42L);
        g.setFirstSquare(0, 0);
        g.generateMines();
        GameLogic.initCloseMines(g);
        GameState state = new GameState(g);
        state.dungeonMode = true;
        assertFalse(state.checkDungeonRoomCleared());
        for (int c = 0; c < g.columns; c++)
            for (int r = 0; r < g.rows; r++)
                if (g.matrix[c][r].mine) g.matrix[c][r].flagged = true;
        assertTrue(state.checkDungeonRoomCleared());
    }

    @Test
    void checkDungeonWin_requiresFloor5AndBigDungeon() {
        Grid g = new Grid(9, 20, 90, 42L);
        g.setFirstSquare(5, 5);
        g.generateMines();
        GameLogic.initCloseMines(g);
        GameState state = new GameState(g);
        state.dungeonMode = true;
        for (int c = 0; c < g.columns; c++)
            for (int r = 0; r < g.rows; r++)
                if (g.matrix[c][r].mine) g.matrix[c][r].flagged = true;

        // Floor 1 — not a win yet
        state.dungeonFloorDepth = 1;
        assertFalse(state.checkDungeonWin());

        // Floor at MAX_DUNGEON_FLOORS on big dungeon — win
        state.dungeonFloorDepth = GameState.MAX_DUNGEON_FLOORS;
        assertTrue(state.checkDungeonWin());
    }

    @Test
    void checkDungeonWin_smallRoom_neverWins() {
        Grid g = new Grid(3, 20, 90, 42L);
        g.setFirstSquare(2, 2);
        g.generateMines();
        GameLogic.initCloseMines(g);
        GameState state = new GameState(g);
        state.dungeonMode = true;
        state.dungeonFloorDepth = GameState.MAX_DUNGEON_FLOORS;
        for (int c = 0; c < g.columns; c++)
            for (int r = 0; r < g.rows; r++)
                if (g.matrix[c][r].mine) g.matrix[c][r].flagged = true;
        assertFalse(state.checkDungeonWin());
    }

    @Test
    void resetCurrentFloor_preservesDungeonStack() {
        Grid g = new Grid(9, 20, 90);
        GameState state = new GameState(g);
        state.dungeonMode = true;
        state.dungeonStack.push(new DungeonFloor(g, state.subFloorDifficulty,
                true, true, null, false,
                new java.util.HashMap<>(), 1L,
                1, false, 0, false, 0, 0, 1));
        int stackSize = state.dungeonStack.size();
        int floorDepth = state.dungeonFloorDepth;
        boolean isDead = state.currentFloorIsDead;

        state.resetCurrentFloor();

        assertEquals(stackSize, state.dungeonStack.size());
        assertEquals(floorDepth, state.dungeonFloorDepth);
        assertEquals(isDead, state.currentFloorIsDead);
        assertTrue(state.playing);
        assertFalse(state.generated);
        assertNull(state.exploded);
        assertFalse(state.win);
        assertEquals(0, state.keysNeeded);
        assertEquals(0, state.keysFound);
    }

    @Test
    void encodeStair_roundtrip() {
        long encoded = GameState.encodeStair(42, 17);
        int col = (int) (encoded / 10000);
        int row = (int) (encoded % 10000);
        assertEquals(42, col);
        assertEquals(17, row);
    }

}

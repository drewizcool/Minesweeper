package minesweeper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class GameState {

    // Core game state
    public volatile Grid grid;
    public volatile boolean playing;
    public volatile boolean generated;
    public volatile Square exploded;
    public volatile boolean win;

    // Dungeon mode state
    public boolean dungeonMode = false;
    public final Deque<DungeonFloor> dungeonStack = new ArrayDeque<>();
    public Map<Long, Integer> subFloorDifficulty = new HashMap<>();
    public Map<Long, DungeonFloor> currentSubFloorCache = new HashMap<>();
    public int dungeonFloorDepth = 1;
    public boolean currentFloorIsDead = false;
    public int deadEndDepth = 0;
    public boolean newFloorPlaced = false;
    public int liveRoomDepth = 0;
    public volatile int keysNeeded = 0;
    public volatile int keysFound = 0;

    // Floor ID tracking — each newly created floor gets a unique ID
    public int nextFloorId = 2;    // 1 is reserved for the initial big dungeon
    public int currentFloorId = 1;

    public static final int MAX_DEAD_END_DEPTH = 5;
    public static final int MAX_LIVE_ROOM_DEPTH = 10;

    public GameState(Grid initialGrid) {
        this.grid = initialGrid;
        this.playing = true;
        this.generated = false;
        this.exploded = null;
        this.win = false;
    }

    public void reset(Grid newGrid) {
        this.dungeonMode = (newGrid.difficulty == 9);
        this.dungeonStack.clear();
        this.subFloorDifficulty = new HashMap<>();
        this.currentSubFloorCache = new HashMap<>();
        this.dungeonFloorDepth = 1;
        this.currentFloorIsDead = false;
        this.deadEndDepth = 0;
        this.newFloorPlaced = false;
        this.liveRoomDepth = 0;
        this.keysNeeded = 0;
        this.keysFound = 0;
        this.nextFloorId = 2;
        this.currentFloorId = 1;
        this.grid = newGrid;
        this.playing = true;
        this.generated = false;
        this.exploded = null;
        this.win = false;
    }

    public void resetCurrentFloor() {
        this.grid = new Grid(grid.difficulty);
        this.subFloorDifficulty = new HashMap<>();
        this.currentSubFloorCache = new HashMap<>();
        this.keysNeeded = 0;
        this.keysFound = 0;
        this.playing = true;
        this.generated = false;
        this.exploded = null;
        this.win = false;
    }

    public static final int MAX_DUNGEON_FLOORS = 2;

    public boolean checkWinCondition() {
        if (dungeonMode) return false;
        return grid.numRemoved == grid.numSquares - grid.numMines;
    }

    public boolean checkDungeonRoomCleared() {
        return dungeonMode && GameLogic.allMinesFlagged(grid);
    }

    public boolean checkDungeonWin() {
        return dungeonMode && dungeonFloorDepth >= MAX_DUNGEON_FLOORS
            && grid.difficulty == 9 && GameLogic.allMinesFlagged(grid);
    }

    public static long encodeStair(int col, int row) {
        return (long) col * 10000 + row;
    }
}

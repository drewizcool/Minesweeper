package minesweeper;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class DungeonManager {

    public static void placeDungeonStairs(GameState state) {
        placeDungeonStairs(state, new Random());
    }

    public static void placeDungeonStairs(GameState state, Random rng) {
        Grid grid = state.grid;

        if (state.currentFloorIsDead && state.deadEndDepth >= GameState.MAX_DEAD_END_DEPTH) {
            printRoomStats("DEAD_END[CAP]", 0, false, state);
            return;
        }

        int count = 2 + rng.nextInt(3);
        List<int[]> stairCoords = grid.placeStairs(count);

        if (grid.difficulty == 9) {
            int liveIdx = rng.nextInt(stairCoords.size());
            for (int i = 0; i < stairCoords.size(); i++) {
                int[] c = stairCoords.get(i);
                int roomDiff = 1 + rng.nextInt(5);
                int dest = (i == liveIdx) ? roomDiff : (100 + roomDiff);
                state.subFloorDifficulty.put(GameState.encodeStair(c[0], c[1]), dest);
            }
            printRoomStats("BIG_DUNGEON", stairCoords.size(), false, state);
        } else if (state.currentFloorIsDead) {
            for (int[] c : stairCoords) {
                state.subFloorDifficulty.put(GameState.encodeStair(c[0], c[1]), 100 + (1 + rng.nextInt(5)));
            }
            printRoomStats("DEAD_END", stairCoords.size(), false, state);
        } else if (state.liveRoomDepth >= GameState.MAX_LIVE_ROOM_DEPTH) {
            for (int[] c : stairCoords) {
                state.subFloorDifficulty.put(GameState.encodeStair(c[0], c[1]), 100 + (1 + rng.nextInt(5)));
            }
            printRoomStats("LIVE[CAP]", stairCoords.size(), false, state);
        } else {
            int newFloorIdx = -1;
            if (!state.newFloorPlaced && rng.nextInt(3 + state.dungeonFloorDepth) == 0) {
                newFloorIdx = rng.nextInt(stairCoords.size());
                state.newFloorPlaced = true;
            }
            for (int i = 0; i < stairCoords.size(); i++) {
                int[] c = stairCoords.get(i);
                int dest = (i == newFloorIdx) ? 9 : (1 + rng.nextInt(5));
                state.subFloorDifficulty.put(GameState.encodeStair(c[0], c[1]), dest);
            }
            printRoomStats("LIVE", stairCoords.size(), newFloorIdx >= 0, state);
        }

        state.keysNeeded = stairCoords.size();
        grid.placeKeys(state.keysNeeded);
    }

    public static void enterDungeonFloor(GameState state, int col, int row) {
        long key = GameState.encodeStair(col, row);
        DungeonFloor savedChild = state.currentSubFloorCache.get(key);

        boolean parentNewFloorPlaced = state.newFloorPlaced;
        int parentLiveRoomDepth = state.liveRoomDepth;

        state.dungeonStack.push(new DungeonFloor(state.grid, state.subFloorDifficulty,
                state.generated, state.playing, state.exploded, state.win,
                new HashMap<>(state.currentSubFloorCache), key,
                state.dungeonFloorDepth, state.currentFloorIsDead, state.deadEndDepth,
                parentNewFloorPlaced, parentLiveRoomDepth,
                state.keysNeeded, state.currentFloorId, state.player));

        if (savedChild != null) {
            state.grid = savedChild.grid;
            state.subFloorDifficulty = savedChild.subFloorDifficulty;
            state.generated = savedChild.generated;
            state.playing = savedChild.playing;
            state.exploded = savedChild.exploded;
            state.win = savedChild.win;
            state.currentSubFloorCache = new HashMap<>(savedChild.subFloorCache);
            state.dungeonFloorDepth = savedChild.floorDepth;
            state.currentFloorIsDead = savedChild.isDead;
            state.deadEndDepth = savedChild.deadEndDepth;
            state.newFloorPlaced = parentNewFloorPlaced || savedChild.newFloorPlaced;
            state.liveRoomDepth = savedChild.liveRoomDepth;
            state.keysNeeded = savedChild.keysNeeded;
            state.currentFloorId = savedChild.floorId;
            state.player = savedChild.player;
            String pathStatus;
            if (state.currentFloorIsDead) {
                pathStatus = "DEAD END";
            } else if (state.grid.difficulty == 9) {
                pathStatus = "NEXT LEVEL";
            } else if (state.newFloorPlaced) {
                pathStatus = "DEAD END (next level placed elsewhere)";
            } else {
                pathStatus = "EXPLORING (next level not yet found)";
            }
            System.out.printf(
                "[RESTORE] Returning to floor #%d (%s) | Level %d/%d | %s | mines=%d  stack=%d%n",
                savedChild.floorId, floorLabel(savedChild),
                state.dungeonFloorDepth, GameState.MAX_DUNGEON_FLOORS,
                pathStatus,
                state.grid.numMines, state.dungeonStack.size());
        } else {
            int destValue = state.subFloorDifficulty.getOrDefault(key, 3);
            boolean enteringDeadEnd = (destValue >= 100);
            int newDifficulty = enteringDeadEnd ? (destValue - 100) : destValue;

            if (newDifficulty == 9) {
                state.dungeonFloorDepth++;
                state.currentFloorIsDead = false;
                state.deadEndDepth = 0;
                state.newFloorPlaced = false;
                state.liveRoomDepth = 0;
            } else if (enteringDeadEnd) {
                state.currentFloorIsDead = true;
                state.deadEndDepth = state.deadEndDepth + 1;
                state.newFloorPlaced = false;
                state.liveRoomDepth = 0;
            } else {
                state.currentFloorIsDead = false;
                state.deadEndDepth = 0;
                state.liveRoomDepth = parentLiveRoomDepth + 1;
            }

            int newId = state.nextFloorId++;
            state.currentFloorId = newId;

            String roomType = newDifficulty == 9 ? "Big Dungeon" :
                              (enteringDeadEnd ? "Dead End" : "Live Room");
            String pathStatus;
            if (enteringDeadEnd) {
                pathStatus = "DEAD END";
            } else if (newDifficulty == 9) {
                pathStatus = "NEXT LEVEL";
            } else if (state.newFloorPlaced) {
                pathStatus = "DEAD END (next level placed elsewhere)";
            } else {
                pathStatus = "EXPLORING (next level not yet found)";
            }
            System.out.printf(
                "[ENTER]   Created floor #%d (%s, difficulty=%d) | Level %d/%d | %s | stack=%d%n",
                newId, roomType, newDifficulty,
                state.dungeonFloorDepth, GameState.MAX_DUNGEON_FLOORS,
                pathStatus, state.dungeonStack.size());

            state.grid = new Grid(newDifficulty);
            state.subFloorDifficulty = new HashMap<>();
            state.currentSubFloorCache = new HashMap<>();
            state.keysNeeded = 0;
            state.player = null;
            state.playing = true;
            state.generated = false;
            state.exploded = null;
            state.win = false;
        }
    }

    public static boolean exitDungeonFloor(GameState state) {
        if (state.dungeonStack.isEmpty()) return false;

        DungeonFloor childSnapshot = new DungeonFloor(state.grid, state.subFloorDifficulty,
                state.generated, state.playing, state.exploded, state.win,
                new HashMap<>(state.currentSubFloorCache), -1,
                state.dungeonFloorDepth, state.currentFloorIsDead, state.deadEndDepth,
                state.newFloorPlaced, state.liveRoomDepth,
                state.keysNeeded, state.currentFloorId, state.player);

        DungeonFloor parentFrame = state.dungeonStack.pop();

        System.out.printf(
            "[EXIT]    Leaving floor #%d, returning to floor #%d | Level %d/%d | stack=%d%n",
            state.currentFloorId, parentFrame.floorId,
            parentFrame.floorDepth, GameState.MAX_DUNGEON_FLOORS,
            state.dungeonStack.size());

        state.grid = parentFrame.grid;
        state.subFloorDifficulty = parentFrame.subFloorDifficulty;
        state.generated = parentFrame.generated;
        state.playing = parentFrame.playing;
        state.exploded = parentFrame.exploded;
        state.win = parentFrame.win;
        state.currentSubFloorCache = new HashMap<>(parentFrame.subFloorCache);
        state.dungeonFloorDepth = parentFrame.floorDepth;
        state.currentFloorIsDead = parentFrame.isDead;
        state.deadEndDepth = parentFrame.deadEndDepth;
        state.liveRoomDepth = parentFrame.liveRoomDepth;
        state.keysNeeded = parentFrame.keysNeeded;
        state.currentFloorId = parentFrame.floorId;
        state.player = parentFrame.player;

        // Reposition player onto the entry stair they descended from
        if (state.player != null && parentFrame.entryKey != -1) {
            int stairCol = (int) (parentFrame.entryKey / 10000);
            int stairRow = (int) (parentFrame.entryKey % 10000);
            Square stairSq = state.grid.matrix[stairCol][stairRow];
            state.player.x = stairSq.x;
            state.player.y = stairSq.y;
        }

        boolean childFoundNewFloor = childSnapshot.subFloorDifficulty.containsValue(9)
                || childSnapshot.newFloorPlaced;
        state.newFloorPlaced = parentFrame.newFloorPlaced || childFoundNewFloor;

        if (parentFrame.entryKey != -1) {
            state.currentSubFloorCache.put(parentFrame.entryKey, childSnapshot);
        }

        return true;
    }

    private static void printRoomStats(String type, int stairs, boolean hasNewFloor, GameState state) {
        System.out.printf(
            "[ROOM]    Floor #%d (%s) placed %d stairs | Level %d/%d | mines=%d  deadEndDepth=%d  liveDepth=%d  stack=%d%s%n",
            state.currentFloorId, type, stairs,
            state.dungeonFloorDepth, GameState.MAX_DUNGEON_FLOORS,
            state.grid.numMines, state.deadEndDepth, state.liveRoomDepth,
            state.dungeonStack.size(), hasNewFloor ? "  [NEW FLOOR STAIR]" : "");
    }

    private static String floorLabel(DungeonFloor floor) {
        if (floor.isDead) return "Dead End";
        if (floor.grid.difficulty == 9) return "Big Dungeon";
        return "Live Room";
    }
}

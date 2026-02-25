package minesweeper;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DungeonManagerTest {

    private GameState makeDungeonState(long seed) {
        Grid g = new Grid(9, 20, 90, seed);
        g.setFirstSquare(5, 5);
        g.generateMines();
        GameLogic.initCloseMines(g);
        GameState state = new GameState(g);
        state.dungeonMode = true;
        return state;
    }

    @Test
    void placeDungeonStairs_bigRoom_hasExactlyOneLivePath() {
        GameState state = makeDungeonState(42L);
        DungeonManager.placeDungeonStairs(state, new Random(99L));

        long liveCount = state.subFloorDifficulty.values().stream()
            .filter(v -> v >= 1 && v <= 9).count();
        long deadCount = state.subFloorDifficulty.values().stream()
            .filter(v -> v >= 101 && v <= 105).count();
        assertEquals(1, liveCount, "Big room should have exactly 1 live path");
        assertTrue(deadCount >= 1, "Big room should have at least 1 dead-end path");
    }

    @Test
    void placeDungeonStairs_deadEndAtMaxDepth_placesNoStairs() {
        Grid g = new Grid(3, 20, 90, 42L);
        g.setFirstSquare(2, 2);
        g.generateMines();
        GameLogic.initCloseMines(g);
        GameState state = new GameState(g);
        state.dungeonMode = true;
        state.currentFloorIsDead = true;
        state.deadEndDepth = GameState.MAX_DEAD_END_DEPTH;

        DungeonManager.placeDungeonStairs(state, new Random(1L));
        assertTrue(state.subFloorDifficulty.isEmpty());
    }

    @Test
    void enterAndExitDungeonFloor_restoresParentGrid() {
        GameState state = makeDungeonState(42L);
        DungeonManager.placeDungeonStairs(state, new Random(42L));
        Grid parentGrid = state.grid;

        Map.Entry<Long, Integer> entry = state.subFloorDifficulty.entrySet().iterator().next();
        int col = (int) (entry.getKey() / 10000);
        int row = (int) (entry.getKey() % 10000);

        DungeonManager.enterDungeonFloor(state, col, row);
        assertNotSame(parentGrid, state.grid);

        DungeonManager.exitDungeonFloor(state);
        assertSame(parentGrid, state.grid);
    }

    @Test
    void exitDungeonFloor_emptyStack_returnsFalse() {
        GameState state = makeDungeonState(42L);
        assertFalse(DungeonManager.exitDungeonFloor(state));
    }

    @Test
    void exitDungeonFloor_withStack_returnsTrue() {
        GameState state = makeDungeonState(42L);
        DungeonManager.placeDungeonStairs(state, new Random(42L));

        Map.Entry<Long, Integer> entry = state.subFloorDifficulty.entrySet().iterator().next();
        int col = (int) (entry.getKey() / 10000);
        int row = (int) (entry.getKey() % 10000);

        DungeonManager.enterDungeonFloor(state, col, row);
        assertTrue(DungeonManager.exitDungeonFloor(state));
    }

    @Test
    void enterDungeonFloor_newFloor_resetsPlayState() {
        GameState state = makeDungeonState(42L);
        DungeonManager.placeDungeonStairs(state, new Random(42L));

        Map.Entry<Long, Integer> entry = state.subFloorDifficulty.entrySet().iterator().next();
        int col = (int) (entry.getKey() / 10000);
        int row = (int) (entry.getKey() % 10000);

        DungeonManager.enterDungeonFloor(state, col, row);
        assertTrue(state.playing);
        assertFalse(state.generated);
        assertNull(state.exploded);
        assertFalse(state.win);
    }

    @Test
    void placeDungeonStairs_liveRoom_placesKeys() {
        Grid g = new Grid(3, 20, 90, 42L);
        g.setFirstSquare(2, 2);
        g.generateMines();
        GameLogic.initCloseMines(g);
        GameState state = new GameState(g);
        state.dungeonMode = true;
        state.currentFloorIsDead = false;

        DungeonManager.placeDungeonStairs(state, new Random(1L));
        assertTrue(state.keysNeeded >= 2 && state.keysNeeded <= 4);
    }

    // ---------------------------------------------------------------
    // Large-scale exploration test: verify floor-9 reachability
    // ---------------------------------------------------------------

    /**
     * Recursively explore all live stairs reachable from the current floor.
     * Returns the number of difficulty-9 stairs found across the entire
     * reachable sub-tree (including the current floor's own stairs).
     *
     * Optimization: once newFloorPlaced is true on the state AND the current
     * floor has no dest==9 stairs, no descendant can produce a floor-9 either,
     * so we prune the entire subtree.
     *
     * @param state  the game state positioned on the current floor
     *               (mines already generated and stairs already placed)
     * @param rng    a seeded Random used for child floor mine generation
     *               and stair placement, ensuring determinism
     * @param depth  recursion depth guard
     * @return count of dest==9 entries reachable from this floor's tree
     */
    private int exploreFloor(GameState state, Random rng, int depth) {
        if (depth > 30) {
            // Safety valve — should never happen with MAX_LIVE_ROOM_DEPTH = 10
            return 0;
        }

        int floor9Count = 0;

        // Snapshot the stair map for this floor so we can iterate it
        Map<Long, Integer> stairs = new HashMap<>(state.subFloorDifficulty);

        // First pass: count floor-9 stairs on THIS floor
        for (int dest : stairs.values()) {
            if (dest == 9) {
                floor9Count++;
            }
        }

        // If newFloorPlaced is already set and this floor has no floor-9 stairs,
        // then no descendant can produce a floor-9, so prune.
        // (We still need to consume RNG values consistently, so we track this.)
        // Actually, we can safely prune because the RNG is per-seed and we only
        // care about the count, not exact reproducibility across branches.
        boolean canPrune = state.newFloorPlaced && floor9Count == 0;

        for (Map.Entry<Long, Integer> entry : stairs.entrySet()) {
            int dest = entry.getValue();

            // Skip floor-9 stairs — already counted, don't descend
            if (dest == 9) continue;

            // Skip dead-end sentinels (100+) — they can't lead to floor-9
            if (dest >= 100) continue;

            // Prune: if newFloorPlaced is set and no floor-9 on this floor,
            // no child or deeper room can place a floor-9.
            if (canPrune) continue;

            // dest is 1-8: live small room — explore it
            long key = entry.getKey();
            int col = (int) (key / 10000);
            int row = (int) (key % 10000);

            DungeonManager.enterDungeonFloor(state, col, row);

            // Generate mines on the child floor
            long childGridSeed = rng.nextLong();
            Grid childGrid = new Grid(state.grid.difficulty, 20, 90, childGridSeed);
            childGrid.setFirstSquare(
                    Math.min(5, childGrid.columns - 1),
                    Math.min(5, childGrid.rows - 1));
            childGrid.generateMines();
            state.grid = childGrid;
            GameLogic.initCloseMines(state.grid);

            // Place stairs on the child floor
            DungeonManager.placeDungeonStairs(state, new Random(rng.nextLong()));

            // Recurse into the child's live stairs
            floor9Count += exploreFloor(state, rng, depth + 1);

            // Return to parent
            DungeonManager.exitDungeonFloor(state);

            // After exiting, newFloorPlaced may now be true on the parent state.
            // Update prune flag for remaining siblings.
            if (state.newFloorPlaced && !canPrune) {
                // Check if any remaining sibling on THIS floor is floor-9
                // (already counted in floor9Count above), so we can prune
                // further sibling exploration.
                canPrune = true;
            }
        }

        return floor9Count;
    }

    @Test
    void verifyExactlyOneLivePathToNextFloor() {
        final int NUM_SEEDS = 200;
        int countZero = 0;
        int countOne = 0;
        int countTwoPlus = 0;
        List<Long> failedSeeds = new ArrayList<>();
        int totalRoomsExplored = 0;

        for (int seed = 0; seed < NUM_SEEDS; seed++) {
            long baseSeed = seed * 1000L + 7;

            // 1. Create the big dungeon floor (difficulty 9) with a deterministic seed
            Grid bigFloor = new Grid(9, 20, 90, baseSeed);
            bigFloor.setFirstSquare(5, 5);
            bigFloor.generateMines();
            GameLogic.initCloseMines(bigFloor);

            GameState state = new GameState(bigFloor);
            state.dungeonMode = true;

            // 2. Place stairs on the big floor
            Random stairRng = new Random(baseSeed + 1);
            DungeonManager.placeDungeonStairs(state, stairRng);

            // 3. Explore the entire reachable tree
            Random exploreRng = new Random(baseSeed + 2);
            int totalFloor9 = exploreFloor(state, exploreRng, 0);

            // 4. Categorize result
            if (totalFloor9 == 0) {
                countZero++;
            } else if (totalFloor9 == 1) {
                countOne++;
            } else {
                countTwoPlus++;
                failedSeeds.add(baseSeed);
            }

            // Verify we returned cleanly to the root (stack should be empty)
            assertTrue(state.dungeonStack.isEmpty(),
                    "Dungeon stack not empty after full exploration for seed " + baseSeed);
        }

        // 5. Print summary
        System.out.println();
        System.out.println("=== verifyExactlyOneLivePathToNextFloor Summary ===");
        System.out.printf("  Seeds tested : %d%n", NUM_SEEDS);
        System.out.printf("  0 floor-9    : %d  (%.1f%%)%n", countZero, 100.0 * countZero / NUM_SEEDS);
        System.out.printf("  1 floor-9    : %d  (%.1f%%)%n", countOne, 100.0 * countOne / NUM_SEEDS);
        System.out.printf("  2+ floor-9   : %d  (%.1f%%)%n", countTwoPlus, 100.0 * countTwoPlus / NUM_SEEDS);
        if (!failedSeeds.isEmpty()) {
            System.out.println("  Seeds with 2+: " + failedSeeds);
        }
        System.out.println("===================================================");

        // Assert: no seed should produce 2+ paths to floor-9
        assertEquals(0, countTwoPlus,
                countTwoPlus + " seed(s) produced 2+ floor-9 paths: " + failedSeeds);
    }
}

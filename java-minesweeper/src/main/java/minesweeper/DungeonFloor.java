package minesweeper;

import java.util.Map;

/**
 * Snapshot of a dungeon floor's state — pushed onto the stack when the player
 * descends a staircase, popped when they press ESC to go back up.
 *
 * subFloorCache maps each stair key on THIS floor to the saved state of the
 * child floor it leads to, so revisiting a stair restores progress.
 *
 * entryKey is the stair key in THIS (parent) floor that was clicked to enter
 * the child floor. exitDungeonFloor uses it to write the child's snapshot back
 * into the restored parent's cache.
 *
 * floorDepth is the dungeon-floor depth counter while on this floor.
 *
 * isDead is true when this floor is in a dead-end branch — all outgoing stairs
 * also lead to dead ends, so there is no path to the next dungeon floor here.
 *
 * newFloorPlaced is true when some live room in this chain already placed a
 * floor-9 stair, preventing duplicate paths to the next big dungeon floor.
 *
 * liveRoomDepth counts consecutive live small rooms from the last big floor.
 * When it reaches MAX_LIVE_ROOM_DEPTH all stairs become dead-end sentinels.
 *
 * Sentinel encoding in subFloorDifficulty:
 *   1–5  → live small room of that difficulty
 *   9    → new big dungeon floor
 *   101–105 → dead-end small room of difficulty (value - 100)
 */
public class DungeonFloor {
    public final Grid grid;
    public final Map<Long, Integer> subFloorDifficulty;
    public final boolean generated;
    public final boolean playing;
    public final Square exploded;
    public final boolean win;
    public final Map<Long, DungeonFloor> subFloorCache;
    public final long entryKey;    // stair key in THIS floor used to descend into the child
    public final int floorDepth;   // dungeonFloorDepth while on this floor
    public final boolean isDead;   // true when this room is in a dead-end branch
    public final int deadEndDepth; // how many consecutive dead-end rooms deep (0 = not dead-end)
    public final boolean newFloorPlaced; // true when a floor-9 stair exists somewhere in this chain
    public final int liveRoomDepth;      // consecutive live small rooms since last big floor
    public final int keysNeeded;         // number of keys required to unlock stairs on this floor
    public final int floorId;            // unique ID for this floor instance

    public DungeonFloor(Grid grid, Map<Long, Integer> subFloorDifficulty,
                        boolean generated, boolean playing,
                        Square exploded, boolean win,
                        Map<Long, DungeonFloor> subFloorCache, long entryKey,
                        int floorDepth, boolean isDead, int deadEndDepth,
                        boolean newFloorPlaced, int liveRoomDepth,
                        int keysNeeded, int floorId) {
        this.grid               = grid;
        this.subFloorDifficulty = subFloorDifficulty;
        this.generated          = generated;
        this.playing            = playing;
        this.exploded           = exploded;
        this.win                = win;
        this.subFloorCache      = subFloorCache;
        this.entryKey           = entryKey;
        this.floorDepth         = floorDepth;
        this.isDead             = isDead;
        this.deadEndDepth       = deadEndDepth;
        this.newFloorPlaced     = newFloorPlaced;
        this.liveRoomDepth      = liveRoomDepth;
        this.keysNeeded         = keysNeeded;
        this.floorId            = floorId;
    }
}

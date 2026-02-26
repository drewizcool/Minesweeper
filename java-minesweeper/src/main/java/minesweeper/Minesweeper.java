package minesweeper;

import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Minesweeper {

    public static final int ORIGIN_X = 20;
    public static final int ORIGIN_Y = 90;

    public static ImageAssets images;
    public static GameState state;

    public static volatile Screen gameScreen;
    public static volatile SettingsMenu gameSettings;

    public static final LinkedBlockingQueue<GameEvent> eventQueue = new LinkedBlockingQueue<>();

    private static JFrame frame;
    private static JFrame hudFrame = null;
    public  static volatile HudPanel hudPanel = null;

    // Arrow key tracking: last-pressed key wins
    private static final Set<Integer> ARROW_KEYS = Set.of(
            KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D);
    private static final Set<Integer> heldArrows = new HashSet<>();
    private static final Deque<Integer> arrowStack = new ArrayDeque<>(); // most recent on top
    private static final long MOVE_INTERVAL_MS = 33; // ~30 fps movement
    private static long lastMoveTime = 0;

    // Action mode: false = reveal, true = flag
    private static boolean flagMode = false;

    // =========================================================================
    // main()
    // =========================================================================

    public static void main(String[] args) {
        images = new ImageAssets(ImageAssets.resolveBaseDir());

        Grid initialGrid = new Grid(3);
        state = new GameState(initialGrid);

        SwingUtilities.invokeLater(() -> {
            gameScreen = new Screen(state.grid, images, state.dungeonMode);
            gameSettings = new SettingsMenu(state.grid, images);

            frame = new JFrame("Minesweeper");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(gameScreen);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);

            attachListeners(gameScreen);

            Thread gameThread = new Thread(Minesweeper::gameLoop, "game-loop");
            gameThread.setDaemon(true);
            gameThread.start();
        });
    }

    // =========================================================================
    // Game loop
    // =========================================================================

    private static void gameLoop() {
        GameLogic.initCloseMines(state.grid);

        state.playing = true;
        state.generated = false;
        state.exploded = null;
        state.win = false;

        boolean run = true;

        while (run) {
            boolean needsRedraw = false;
            GameEvent event;
            try {
                boolean wantsMovement = !arrowStack.isEmpty() && state.dungeonMode
                        && state.player != null && state.playing;
                if (wantsMovement) {
                    long elapsed = System.currentTimeMillis() - lastMoveTime;
                    long remaining = Math.max(1, MOVE_INTERVAL_MS - elapsed);
                    event = eventQueue.poll(remaining, TimeUnit.MILLISECONDS);
                } else {
                    event = eventQueue.take();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            if (event != null && event.type == GameEvent.EventType.QUIT) {
                run = false;

            } else if (event != null && event.type == GameEvent.EventType.KEY_PRESS) {
                if (ARROW_KEYS.contains(event.keyCode)) {
                    if (heldArrows.add(event.keyCode)) {
                        arrowStack.push(event.keyCode);
                    }
                    // Immediate first move on key press
                    lastMoveTime = 0;
                } else if (event.keyCode == KeyEvent.VK_F2) {
                    if (state.dungeonMode) {
                        state.resetCurrentFloor();
                        heldArrows.clear();
                        arrowStack.clear();
                        rebuildScreen();
                    } else {
                        rebuildGame(state.grid.difficulty);
                    }
                    needsRedraw = true;
                } else if (event.keyCode == KeyEvent.VK_ESCAPE && state.dungeonMode && !state.dungeonStack.isEmpty()) {
                    DungeonManager.exitDungeonFloor(state);
                    heldArrows.clear();
                    arrowStack.clear();
                    rebuildScreen();
                    needsRedraw = true;
                } else if (event.keyCode == KeyEvent.VK_ENTER && state.dungeonMode
                        && state.player != null && state.playing) {
                    int col = state.player.gridCol();
                    int row = state.player.gridRow();
                    if (col >= 0 && col < state.grid.columns && row >= 0 && row < state.grid.rows) {
                        Square sqObj = state.grid.matrix[col][row];
                        if (sqObj.isStairs && sqObj.removed && sqObj.stairUnlocked) {
                            DungeonManager.enterDungeonFloor(state, col, row);
                            heldArrows.clear();
                            arrowStack.clear();
                            rebuildScreen();
                            needsRedraw = true;
                        }
                    }
                } else if (event.keyCode == KeyEvent.VK_SHIFT && state.dungeonMode) {
                    flagMode = !flagMode;
                    needsRedraw = true;
                } else if (event.keyCode == KeyEvent.VK_SPACE && state.dungeonMode
                        && state.player != null && state.playing && state.generated) {
                    int targetCol = state.player.gridCol() + state.player.facingDx;
                    int targetRow = state.player.gridRow() + state.player.facingDy;
                    if (targetCol >= 0 && targetCol < state.grid.columns
                            && targetRow >= 0 && targetRow < state.grid.rows) {
                        int[] target = {targetCol, targetRow};
                        Square sqObj = state.grid.matrix[targetCol][targetRow];
                        if (flagMode) {
                            if (state.player.canPlaceFlag(targetCol, targetRow)) {
                                if (sqObj.isStairs && sqObj.removed && sqObj.stairUnlocked) {
                                    sqObj.stairMarked = !sqObj.stairMarked;
                                } else {
                                    GameLogic.placeFlag(state.grid, target);
                                    if (state.checkDungeonWin()) {
                                        state.playing = false;
                                        state.win = true;
                                    } else if (state.checkDungeonRoomCleared()) {
                                        state.win = true;
                                    } else {
                                        state.win = false;
                                    }
                                }
                            }
                        } else {
                            if (!sqObj.removed && !sqObj.flagged) {
                                boolean removed = GameLogic.removeSquare(state.grid, target);
                                if (removed) {
                                    if (sqObj.mine) {
                                        state.exploded = sqObj;
                                        state.playing = false;
                                        state.win = false;
                                        state.generated = false;
                                    } else if (state.checkWinCondition()) {
                                        state.playing = false;
                                        state.win = true;
                                    }
                                }
                            }
                        }
                        needsRedraw = true;
                    }
                }

            } else if (event != null && event.type == GameEvent.EventType.KEY_RELEASE) {
                if (ARROW_KEYS.contains(event.keyCode)) {
                    heldArrows.remove(event.keyCode);
                    arrowStack.remove(event.keyCode);
                }

            } else if (event != null && event.type == GameEvent.EventType.MOUSE_DOWN) {
                needsRedraw = true;
                int mx = event.x;
                int my = event.y;
                int btn = event.button;
                int[] square = state.grid.selectSquare(mx, my);

                if (btn == 1) {
                    if (gameScreen.resetButton.contains(mx, my)) {
                        if (holdButton(gameScreen.resetButton)) {
                            if (state.dungeonMode) {
                                state.resetCurrentFloor();
                                heldArrows.clear();
                                arrowStack.clear();
                                rebuildScreen();
                            } else {
                                rebuildGame(state.grid.difficulty);
                            }
                        }

                    } else if (gameScreen.settingsButton.contains(mx, my)) {
                        if (holdButton(gameScreen.settingsButton)) {
                            int setting = openSettings();
                            if (setting != state.grid.difficulty && setting != -1) {
                                rebuildGame(setting);
                            }
                        }

                    } else if (state.dungeonMode && gameScreen.bagButton != null
                            && gameScreen.bagButton.contains(mx, my)) {
                        if (holdButton(gameScreen.bagButton)) {
                            toggleHudWindow();
                        }

                    } else if (square != null && state.playing) {
                        if (!state.generated) {
                            state.grid.setFirstSquare(square[0], square[1]);
                            state.grid.generateMines();
                            GameLogic.initCloseMines(state.grid);
                            if (state.dungeonMode) DungeonManager.placeDungeonStairs(state);
                            state.generated = true;
                            if (state.dungeonMode) {
                                Square firstSq = state.grid.matrix[square[0]][square[1]];
                                state.player = new Player(
                                    firstSq.x, firstSq.y,
                                    state.grid.originX, state.grid.originY);
                            }
                        }
                        List<int[]> adjacent = holdSquare(square);
                        if (adjacent != null) {
                            int[] sq0 = adjacent.get(0);
                            Square sqObj = state.grid.matrix[sq0[0]][sq0[1]];
                            if (state.dungeonMode && sqObj.isStairs && sqObj.removed
                                    && !sqObj.stairUnlocked && state.keysFound > 0) {
                                state.keysFound--;
                                sqObj.stairUnlocked = true;
                            } else if (!(state.dungeonMode && sqObj.isKey && sqObj.removed)
                                    && !(state.dungeonMode && sqObj.isStairs && sqObj.removed)) {
                                boolean removed = GameLogic.removeSquare(state.grid, sq0);
                                if (removed) {
                                    if (state.grid.matrix[sq0[0]][sq0[1]].mine) {
                                        state.exploded = state.grid.matrix[sq0[0]][sq0[1]];
                                        state.playing = false;
                                        state.win = false;
                                        state.generated = false;
                                    } else if (state.checkWinCondition()) {
                                        state.playing = false;
                                        state.win = true;
                                    }
                                }
                            }
                        }

                    }

                } else if (btn == 2 && state.playing) {
                    if (square != null) {
                        List<int[]> adjacent = holdAdjacent(square);
                        if (adjacent != null && state.grid.matrix[square[0]][square[1]].removed) {
                            int close = GameLogic.getCloseFlags(state.grid, square[0], square[1]);
                            if (close == state.grid.matrix[square[0]][square[1]].closeMines && close > 0) {
                                for (int[] adj : adjacent) {
                                    boolean removed = GameLogic.removeSquare(state.grid, adj);
                                    if (removed) {
                                        if (state.grid.matrix[adj[0]][adj[1]].mine) {
                                            state.exploded = state.grid.matrix[adj[0]][adj[1]];
                                            state.playing = false;
                                            state.win = false;
                                        } else if (state.checkWinCondition()) {
                                            state.playing = false;
                                            state.win = true;
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else if (btn == 3 && state.playing) {
                    if (square != null) {
                        Square sqObj = state.grid.matrix[square[0]][square[1]];
                        if (state.dungeonMode && state.player != null
                                && !state.player.canPlaceFlag(square[0], square[1])) {
                            // Outside flag placement radius — ignore
                        } else if (state.dungeonMode && sqObj.isStairs && sqObj.removed && sqObj.stairUnlocked) {
                            sqObj.stairMarked = !sqObj.stairMarked;
                        } else {
                            GameLogic.placeFlag(state.grid, square);
                            if (state.checkDungeonWin()) {
                                state.playing = false;
                                state.win = true;
                            } else if (state.checkDungeonRoomCleared()) {
                                state.win = true;
                            } else if (state.dungeonMode) {
                                state.win = false;
                            }
                        }
                    }
                }
            }

            // Continuous movement tick based on held arrow keys (time-gated)
            long now = System.currentTimeMillis();
            if (!arrowStack.isEmpty() && state.dungeonMode
                    && state.player != null && state.playing
                    && now - lastMoveTime >= MOVE_INTERVAL_MS) {
                lastMoveTime = now;
                int activeKey = arrowStack.peek();
                int dx = 0, dy = 0;
                switch (activeKey) {
                    case KeyEvent.VK_UP:    case KeyEvent.VK_W: dy = -1; break;
                    case KeyEvent.VK_DOWN:  case KeyEvent.VK_S: dy =  1; break;
                    case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: dx = -1; break;
                    case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: dx =  1; break;
                }
                if (dx != 0 || dy != 0) {
                    state.player.move(dx, dy, state.grid);
                    state.player.facingDx = dx;
                    state.player.facingDy = dy;
                    checkKeyCollection();
                    needsRedraw = true;
                }
            }

            if (needsRedraw) drawAll();
        }

        SwingUtilities.invokeLater(() -> System.exit(0));
    }

    // =========================================================================
    // rebuildGame
    // =========================================================================

    private static void rebuildGame(int difficulty) {
        boolean wasDungeon = state.dungeonMode;
        state.reset(new Grid(difficulty));
        heldArrows.clear();
        arrowStack.clear();
        flagMode = false;
        rebuildScreen();
        if (!state.dungeonMode && wasDungeon) closeHudWindow();
        drawAll();
    }

    private static void attachListeners(Screen screen) {
        screen.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                screen.requestFocusInWindow();
                eventQueue.add(GameEvent.mouseDown(e.getX(), e.getY(), swingToButton(e.getButton())));
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                eventQueue.add(GameEvent.mouseUp(e.getX(), e.getY(), swingToButton(e.getButton())));
            }
        });
        screen.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                eventQueue.add(GameEvent.mouseMove(e.getX(), e.getY()));
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                eventQueue.add(GameEvent.mouseMove(e.getX(), e.getY()));
            }
        });
        screen.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                eventQueue.add(GameEvent.keyPress(e.getKeyCode()));
            }
            @Override
            public void keyReleased(KeyEvent e) {
                eventQueue.add(GameEvent.keyRelease(e.getKeyCode()));
            }
        });
        screen.setFocusable(true);
    }

    // =========================================================================
    // Hold functions
    // =========================================================================

    static boolean holdButton(Button button) {
        boolean wasClickImage = button.clickImage != null;
        BufferedImage origImage = button.image;
        if (wasClickImage) {
            button.image = button.clickImage;
        }
        gameScreen.showClickOverlay();

        boolean result = false;
        boolean done = false;
        while (!done) {
            GameEvent e;
            try {
                e = eventQueue.take();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            switch (e.type) {
                case MOUSE_UP:
                    result = button.contains(e.x, e.y);
                    done = true;
                    break;
                case MOUSE_MOVE:
                    if (wasClickImage) {
                        button.image = button.contains(e.x, e.y) ? button.clickImage : origImage;
                    }
                    gameScreen.repaint();
                    break;
                case QUIT:
                    eventQueue.add(e);
                    done = true;
                    break;
                default:
                    break;
            }
        }

        button.image = origImage;
        gameScreen.clearOverlays();
        return result;
    }

    static List<int[]> holdSquare(int[] square) {
        List<int[]> adjacent = state.grid.getAdjacentCoords(square[0], square[1]);

        gameScreen.showClickOverlay();
        showZeroForSquare(adjacent.get(0));

        boolean clicked = true;
        while (clicked) {
            GameEvent e;
            try {
                e = eventQueue.take();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            switch (e.type) {
                case MOUSE_MOVE:
                case MOUSE_DOWN: {
                    int[] newSquare = state.grid.selectSquare(e.x, e.y);
                    if (newSquare != null) {
                        square = newSquare;
                        adjacent = state.grid.getAdjacentCoords(square[0], square[1]);
                        gameScreen.showClickOverlay();
                        showZeroForSquare(adjacent.get(0));
                    } else {
                        adjacent = null;
                        gameScreen.clearOverlays();
                        drawAll();
                    }
                    break;
                }
                case MOUSE_UP: {
                    int[] newSquare = state.grid.selectSquare(e.x, e.y);
                    if (newSquare != null) {
                        square = newSquare;
                        adjacent = state.grid.getAdjacentCoords(square[0], square[1]);
                    } else {
                        adjacent = null;
                    }
                    clicked = false;
                    break;
                }
                case QUIT:
                    eventQueue.add(e);
                    adjacent = null;
                    clicked = false;
                    break;
                default:
                    break;
            }
        }

        gameScreen.clearOverlays();
        return adjacent;
    }

    static List<int[]> holdAdjacent(int[] square) {
        List<int[]> adjacent = state.grid.getAdjacentCoords(square[0], square[1]);

        gameScreen.showClickOverlay();
        showZeroForAdjacent(adjacent);

        boolean clicked = true;
        while (clicked) {
            GameEvent e;
            try {
                e = eventQueue.take();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            switch (e.type) {
                case MOUSE_MOVE:
                case MOUSE_DOWN: {
                    int[] newSquare = state.grid.selectSquare(e.x, e.y);
                    if (newSquare != null) {
                        square = newSquare;
                        adjacent = state.grid.getAdjacentCoords(square[0], square[1]);
                        gameScreen.showClickOverlay();
                        showZeroForAdjacent(adjacent);
                    } else {
                        adjacent = null;
                        gameScreen.clearOverlays();
                        drawAll();
                    }
                    break;
                }
                case MOUSE_UP: {
                    int[] newSquare = state.grid.selectSquare(e.x, e.y);
                    if (newSquare != null) {
                        square = newSquare;
                        adjacent = state.grid.getAdjacentCoords(square[0], square[1]);
                    } else {
                        adjacent = null;
                    }
                    clicked = false;
                    break;
                }
                case QUIT:
                    eventQueue.add(e);
                    adjacent = null;
                    clicked = false;
                    break;
                default:
                    break;
            }
        }

        gameScreen.clearOverlays();
        return adjacent;
    }

    private static void showZeroForSquare(int[] gridCoord) {
        Square sq = state.grid.matrix[gridCoord[0]][gridCoord[1]];
        if (!sq.removed && !sq.flagged) {
            List<int[]> positions = new ArrayList<>(1);
            positions.add(new int[]{sq.x, sq.y});
            gameScreen.showZeroAtPositions(positions);
        }
    }

    private static void showZeroForAdjacent(List<int[]> adjacent) {
        List<int[]> positions = new ArrayList<>();
        for (int[] a : adjacent) {
            Square sq = state.grid.matrix[a[0]][a[1]];
            if (!sq.removed && !sq.flagged) {
                positions.add(new int[]{sq.x, sq.y});
            }
        }
        gameScreen.showZeroAtPositions(positions);
    }

    // =========================================================================
    // openSettings
    // =========================================================================

    static int openSettings() {
        gameScreen.drawSettings(gameSettings);

        while (true) {
            GameEvent e;
            try {
                e = eventQueue.take();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                gameScreen.clearSettings();
                return state.grid.difficulty;
            }

            if (e.type == GameEvent.EventType.QUIT) {
                eventQueue.add(e);
                gameScreen.clearSettings();
                return -1;
            }

            if (e.type == GameEvent.EventType.MOUSE_DOWN && e.button == 1) {
                int mx = e.x;
                int my = e.y;

                for (Button btn : gameSettings.buttons) {
                    if (btn.contains(mx, my)) {
                        gameScreen.clearSettings();
                        return btn.code;
                    }
                }

                if (gameScreen.settingsButton.contains(mx, my)) {
                    if (holdButton(gameScreen.settingsButton)) {
                        gameScreen.clearSettings();
                        return state.grid.difficulty;
                    }
                    gameScreen.drawSettings(gameSettings);
                } else {
                    gameScreen.clearSettings();
                    return state.grid.difficulty;
                }
            }
            gameScreen.drawSettings(gameSettings);
        }
    }



    // =========================================================================
    // UI helpers
    // =========================================================================

    private static void checkKeyCollection() {
        if (state.player == null) return;
        int col = state.player.gridCol();
        int row = state.player.gridRow();
        if (col >= 0 && col < state.grid.columns && row >= 0 && row < state.grid.rows) {
            Square sq = state.grid.matrix[col][row];
            if (sq.isKey && sq.removed && !sq.keyCollected) {
                sq.keyCollected = true;
                state.keysFound++;
            }
        }
    }

    private static void drawAll() {
        gameScreen.draw(state.playing, state.exploded, state.win, state.player, flagMode);
        if (hudPanel != null) {
            hudPanel.setKeysFound(state.keysFound);
            hudPanel.repaint();
        }
    }

    private static void ensureHudWindow() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (hudFrame != null) return;
                hudPanel = new HudPanel(images);
                hudFrame = new JFrame("Inventory");
                hudFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                hudFrame.add(hudPanel);
                hudFrame.pack();
                hudFrame.setResizable(false);
                int hx = Math.max(0, frame.getX() - hudFrame.getWidth() - 10);
                hudFrame.setLocation(hx, frame.getY());
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void toggleHudWindow() {
        ensureHudWindow();
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (hudFrame.isVisible()) {
                    hudFrame.setVisible(false);
                } else {
                    int hx = Math.max(0, frame.getX() - hudFrame.getWidth() - 10);
                    hudFrame.setLocation(hx, frame.getY());
                    hudFrame.setVisible(true);
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void closeHudWindow() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (hudFrame != null) { hudFrame.dispose(); hudFrame = null; }
                hudPanel = null;
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void rebuildScreen() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame.remove(gameScreen);
                gameScreen = new Screen(state.grid, images, state.dungeonMode);
                gameSettings = new SettingsMenu(state.grid, images);
                frame.add(gameScreen);
                frame.setResizable(true);
                frame.pack();
                frame.setResizable(false);
                frame.setLocationRelativeTo(null);
                attachListeners(gameScreen);
                gameScreen.setFocusable(true);
                gameScreen.requestFocusInWindow();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        eventQueue.clear();
    }

    private static int swingToButton(int swingButton) {
        switch (swingButton) {
            case MouseEvent.BUTTON1: return 1;
            case MouseEvent.BUTTON2: return 2;
            case MouseEvent.BUTTON3: return 3;
            default: return swingButton;
        }
    }
}

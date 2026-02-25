package minesweeper;

import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

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
            GameEvent event;
            try {
                event = eventQueue.take();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            if (event.type == GameEvent.EventType.QUIT) {
                run = false;

            } else if (event.type == GameEvent.EventType.KEY_PRESS) {
                if (event.keyCode == KeyEvent.VK_F2) {
                    if (state.dungeonMode) {
                        state.resetCurrentFloor();
                        rebuildScreen();
                        drawAll();
                    } else {
                        rebuildGame(state.grid.difficulty);
                    }
                } else if (event.keyCode == KeyEvent.VK_ESCAPE && state.dungeonMode && !state.dungeonStack.isEmpty()) {
                    DungeonManager.exitDungeonFloor(state);
                    rebuildScreen();
                    drawAll();
                }

            } else if (event.type == GameEvent.EventType.MOUSE_DOWN) {
                int mx = event.x;
                int my = event.y;
                int btn = event.button;
                int[] square = state.grid.selectSquare(mx, my);

                if (btn == 1) {
                    if (gameScreen.resetButton.contains(mx, my)) {
                        if (holdButton(gameScreen.resetButton)) {
                            if (state.dungeonMode) {
                                state.resetCurrentFloor();
                                rebuildScreen();
                                drawAll();
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
                        }
                        List<int[]> adjacent = holdSquare(square);
                        if (adjacent != null) {
                            int[] sq0 = adjacent.get(0);
                            Square sqObj = state.grid.matrix[sq0[0]][sq0[1]];
                            if (state.dungeonMode && sqObj.isKey && sqObj.removed && !sqObj.keyCollected) {
                                sqObj.keyCollected = true;
                                state.keysFound++;
                            } else if (state.dungeonMode && sqObj.isStairs && sqObj.removed) {
                                if (sqObj.stairUnlocked) {
                                    DungeonManager.enterDungeonFloor(state, sq0[0], sq0[1]);
                                    rebuildScreen();
                                    drawAll();
                                } else if (state.keysFound > 0) {
                                    state.keysFound--;
                                    sqObj.stairUnlocked = true;
                                }
                            } else {
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
                        if (state.dungeonMode && sqObj.isStairs && sqObj.removed && sqObj.stairUnlocked) {
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

            drawAll();
        }

        SwingUtilities.invokeLater(() -> System.exit(0));
    }

    // =========================================================================
    // rebuildGame
    // =========================================================================

    private static void rebuildGame(int difficulty) {
        boolean wasDungeon = state.dungeonMode;
        state.reset(new Grid(difficulty));
        rebuildScreen();
        if (!state.dungeonMode && wasDungeon) closeHudWindow();
        drawAll();
    }

    private static void attachListeners(Screen screen) {
        screen.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
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

    private static void drawAll() {
        gameScreen.draw(state.playing, state.exploded, state.win);
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

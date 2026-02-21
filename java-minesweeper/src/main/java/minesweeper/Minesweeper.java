package minesweeper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Main class. Ports Python global state, global functions, and the main game loop.
 *
 * Threading: game loop runs on a background thread; Swing listeners push GameEvents
 * onto a LinkedBlockingQueue. The game thread calls queue.take() (blocking), which
 * mirrors pygame's event.get() pattern and allows the hold loops to work identically.
 */
public class Minesweeper {

    // Python: ORIGIN = (20, 60)
    public static final int ORIGIN_X = 20;
    public static final int ORIGIN_Y = 60;

    // -------------------------------------------------------------------------
    // Images — loaded once at startup
    // -------------------------------------------------------------------------
    public static BufferedImage Corner_image;
    public static BufferedImage CornerFlip_image;
    public static BufferedImage Square_image;
    public static BufferedImage Zero_image;
    public static BufferedImage Flag_image;
    public static BufferedImage Wrong_image;
    public static BufferedImage Mine_image;
    public static BufferedImage Explode_image;
    public static BufferedImage Happy_image;
    public static BufferedImage HappyClick_image;
    public static BufferedImage Sad_image;
    public static BufferedImage Click_image;
    public static BufferedImage Settings_image;
    public static BufferedImage SettingsClick_image;
    public static BufferedImage Menu_image;
    public static BufferedImage B1_image;
    public static BufferedImage B1click_image;
    public static BufferedImage B2_image;
    public static BufferedImage B3_image;
    public static BufferedImage I1_image;
    public static BufferedImage I2_image;
    public static BufferedImage E_image;
    public static BufferedImage Won_image;
    public static BufferedImage One_image;
    public static BufferedImage Two_image;
    public static BufferedImage Three_image;
    public static BufferedImage Four_image;
    public static BufferedImage Five_image;
    public static BufferedImage Six_image;
    public static BufferedImage Seven_image;
    public static BufferedImage Eight_image;

    // -------------------------------------------------------------------------
    // Global game state (Python globals)
    // -------------------------------------------------------------------------
    public static volatile Grid gameGrid;
    public static volatile Screen gameScreen;
    public static volatile SettingsMenu gameSettings;
    public static volatile boolean playing;
    public static volatile boolean generated;
    public static volatile Square exploded;
    public static volatile boolean win;

    // -------------------------------------------------------------------------
    // Event queue — replaces pygame.event.get()
    // -------------------------------------------------------------------------
    public static final LinkedBlockingQueue<GameEvent> eventQueue = new LinkedBlockingQueue<>();

    private static JFrame frame;

    // =========================================================================
    // main()
    // =========================================================================

    public static void main(String[] args) {
        File baseDir = resolveBaseDir();
        loadImages(baseDir);

        // Python: GameGrid = Grid(3); GameScreen = Screen(GameGrid); GameSettings = ...
        gameGrid = new Grid(3);

        SwingUtilities.invokeLater(() -> {
            gameScreen = new Screen(gameGrid);
            gameSettings = new SettingsMenu(gameGrid);

            frame = new JFrame("Minesweeper");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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
    // Game loop — direct port of Python "while run:" block
    // =========================================================================

    private static void gameLoop() {
        // Python calls getCloseMines on all squares right after grid creation
        // (before mines exist, so all values will be 0 — matches Python behavior)
        initCloseMines();

        playing = true;
        generated = false;
        exploded = null;
        win = false;

        boolean run = true;

        while (run) {
            GameEvent event;
            try {
                event = eventQueue.take(); // blocks — mirrors pygame.event.get()
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            if (event.type == GameEvent.EventType.QUIT) {
                run = false;

            } else if (event.type == GameEvent.EventType.KEY_PRESS
                    && event.keyCode == KeyEvent.VK_F2) {
                // Python: keys[pygame.K_F2] → reset same difficulty
                rebuildGame(gameGrid.difficulty);

            } else if (event.type == GameEvent.EventType.MOUSE_DOWN) {
                int mx = event.x;
                int my = event.y;
                int btn = event.button;
                int[] square = selectSquare(mx, my);

                if (btn == 1) {
                    if (gameScreen.resetButton.contains(mx, my)) {
                        if (holdButton(gameScreen.resetButton)) {
                            rebuildGame(gameGrid.difficulty);
                        }

                    } else if (gameScreen.settingsButton.contains(mx, my)) {
                        if (holdButton(gameScreen.settingsButton)) {
                            int setting = openSettings();
                            if (setting != gameGrid.difficulty && setting != -1) {
                                rebuildGame(setting);
                            }
                        }

                    } else if (square != null && playing) {
                        if (!generated) {
                            gameGrid.setFirstSquare(square[0], square[1]);
                            gameGrid.generateMines();
                            initCloseMines();
                            generated = true;
                        }
                        List<int[]> adjacent = holdSquare(square);
                        if (adjacent != null) {
                            int[] sq0 = adjacent.get(0);
                            boolean removed = removeSquare(sq0);
                            if (removed) {
                                if (gameGrid.matrix[sq0[0]][sq0[1]].mine) {
                                    exploded = gameGrid.matrix[sq0[0]][sq0[1]];
                                    playing = false;
                                    win = false;
                                    generated = false;
                                } else if (gameGrid.numRemoved == gameGrid.numSquares - gameGrid.numMines) {
                                    playing = false;
                                    win = true;
                                }
                            }
                        }

                    }

                } else if (btn == 2 && playing) {
                    if (square != null) {
                        List<int[]> adjacent = holdAdjacent(square);
                        if (adjacent != null && gameGrid.matrix[square[0]][square[1]].removed) {
                            int close = getCloseFlags(square);
                            if (close == gameGrid.matrix[square[0]][square[1]].closeMines && close > 0) {
                                for (int[] adj : adjacent) {
                                    boolean removed = removeSquare(adj);
                                    if (removed) {
                                        if (gameGrid.matrix[adj[0]][adj[1]].mine) {
                                            exploded = gameGrid.matrix[adj[0]][adj[1]];
                                            playing = false;
                                            win = false;
                                        } else if (gameGrid.numRemoved == gameGrid.numSquares - gameGrid.numMines) {
                                            playing = false;
                                            win = true;
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else if (btn == 3 && playing) {
                    if (square != null) {
                        placeFlag(square);
                    }
                }
            }

            // Python: GameScreen.draw(playing, exploded, event.type, win) + flip()
            gameScreen.draw(playing, exploded, win);
        }

        SwingUtilities.invokeLater(() -> System.exit(0));
    }

    // =========================================================================
    // rebuildGame — replaces Python "del GameGrid; GameGrid = Grid(n)" pattern
    // =========================================================================

    private static void rebuildGame(int difficulty) {
        gameGrid = new Grid(difficulty);
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame.remove(gameScreen);
                gameScreen = new Screen(gameGrid);
                gameSettings = new SettingsMenu(gameGrid);
                frame.add(gameScreen);
                frame.pack();
                frame.setLocationRelativeTo(null);
                attachListeners(gameScreen);
                gameScreen.setFocusable(true);
                gameScreen.requestFocusInWindow();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Clear stale events from previous game
        eventQueue.clear();

        playing = true;
        generated = false;
        exploded = null;
        win = false;
        gameScreen.draw(playing, exploded, win);
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
    // Hold functions — blocking loops that work naturally with queue.take()
    // =========================================================================

    /**
     * Port of Python holdButton().
     * Shows click image while held; returns true if released over the button.
     */
    static boolean holdButton(Button button) {
        // Show click image (Python: blit clickImage + flip)
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
                    eventQueue.add(e); // propagate so game loop can exit
                    done = true;
                    break;
                default:
                    break; // consume other events (same as Python)
            }
        }

        button.image = origImage;
        gameScreen.clearOverlays();
        return result;
    }

    /**
     * Port of Python holdSquare().
     * Left-click hold: shows Zero at the square under the cursor.
     * Returns the adjacent list at the release position, or null if off-grid.
     */
    static List<int[]> holdSquare(int[] square) {
        List<int[]> adjacent = getAdjacentCoords(square[0], square[1]);

        // Initial display: show scared face + blank the hovered square
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
                    int[] newSquare = selectSquare(e.x, e.y);
                    if (newSquare != null) {
                        square = newSquare;
                        adjacent = getAdjacentCoords(square[0], square[1]);
                        gameScreen.showClickOverlay();
                        showZeroForSquare(adjacent.get(0));
                    } else {
                        adjacent = null;
                        gameScreen.clearOverlays();
                        gameScreen.draw(playing, exploded, win);
                    }
                    break;
                }
                case MOUSE_UP: {
                    // Update square/adjacent at release position
                    int[] newSquare = selectSquare(e.x, e.y);
                    if (newSquare != null) {
                        square = newSquare;
                        adjacent = getAdjacentCoords(square[0], square[1]);
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

    /**
     * Port of Python holdAdjacent().
     * Middle-click hold: shows Zero at all adjacent squares.
     * Returns the adjacent list at the release position, or null if off-grid.
     */
    static List<int[]> holdAdjacent(int[] square) {
        List<int[]> adjacent = getAdjacentCoords(square[0], square[1]);

        // Show scared face + blank all adjacent squares
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
                    int[] newSquare = selectSquare(e.x, e.y);
                    if (newSquare != null) {
                        square = newSquare;
                        adjacent = getAdjacentCoords(square[0], square[1]);
                        gameScreen.showClickOverlay();
                        showZeroForAdjacent(adjacent);
                    } else {
                        adjacent = null;
                        gameScreen.clearOverlays();
                        gameScreen.draw(playing, exploded, win);
                    }
                    break;
                }
                case MOUSE_UP: {
                    int[] newSquare = selectSquare(e.x, e.y);
                    if (newSquare != null) {
                        square = newSquare;
                        adjacent = getAdjacentCoords(square[0], square[1]);
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

    /** Show Zero_image at a single grid square (if not removed/flagged). */
    private static void showZeroForSquare(int[] gridCoord) {
        Square sq = gameGrid.matrix[gridCoord[0]][gridCoord[1]];
        if (!sq.removed && !sq.flagged) {
            List<int[]> positions = new ArrayList<>(1);
            positions.add(new int[]{sq.x, sq.y});
            gameScreen.showZeroAtPositions(positions);
        }
    }

    /** Show Zero_image at all non-removed, non-flagged adjacent squares. */
    private static void showZeroForAdjacent(List<int[]> adjacent) {
        List<int[]> positions = new ArrayList<>();
        for (int[] a : adjacent) {
            Square sq = gameGrid.matrix[a[0]][a[1]];
            if (!sq.removed && !sq.flagged) {
                positions.add(new int[]{sq.x, sq.y});
            }
        }
        gameScreen.showZeroAtPositions(positions);
    }

    // =========================================================================
    // openSettings — port of Python openSettings()
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
                return gameGrid.difficulty;
            }

            if (e.type == GameEvent.EventType.QUIT) {
                eventQueue.add(e);
                gameScreen.clearSettings();
                return -1;
            }

            if (e.type == GameEvent.EventType.MOUSE_DOWN && e.button == 1) {
                int mx = e.x;
                int my = e.y;

                // Check difficulty buttons
                for (Button btn : gameSettings.buttons) {
                    if (btn.contains(mx, my)) {
                        gameScreen.clearSettings();
                        return btn.code;
                    }
                }

                // Python: if selectButton(GameScreen.settingsButton, mouse_pos): holdButton(...)
                if (gameScreen.settingsButton.contains(mx, my)) {
                    if (holdButton(gameScreen.settingsButton)) {
                        gameScreen.clearSettings();
                        return gameGrid.difficulty; // same difficulty → no change
                    }
                    // Button was clicked but not released over it → stay in settings
                    gameScreen.drawSettings(gameSettings);
                } else {
                    // Clicked outside menu → close settings
                    gameScreen.clearSettings();
                    return gameGrid.difficulty;
                }
            }
            // Other events (mouse move, key): redraw settings
            gameScreen.drawSettings(gameSettings);
        }
    }

    // =========================================================================
    // Static game logic — port of Python global functions
    // =========================================================================

    public static void initCloseMines() {
        for (int i = 0; i < gameGrid.columns; i++) {
            for (int j = 0; j < gameGrid.rows; j++) {
                getCloseMines(new int[]{i, j});
            }
        }
    }

    /** Port of Python getCloseMines(). */
    public static void getCloseMines(int[] square) {
        List<int[]> adjacent = getAdjacentCoords(square[0], square[1]);
        // Start from index 1 to skip self (index 0)
        for (int x = 1; x < adjacent.size(); x++) {
            int[] a = adjacent.get(x);
            if (gameGrid.matrix[a[0]][a[1]].mine) {
                gameGrid.matrix[square[0]][square[1]].closeMines++;
            }
        }
    }

    /** Port of Python getCloseFlags(). */
    public static int getCloseFlags(int[] square) {
        List<int[]> adjacent = getAdjacentCoords(square[0], square[1]);
        int close = 0;
        for (int x = 1; x < adjacent.size(); x++) {
            int[] a = adjacent.get(x);
            if (gameGrid.matrix[a[0]][a[1]].flagged) {
                close++;
            }
        }
        return close;
    }

    /**
     * Port of Python selectSquare() — binary search through coordMatrix.
     * Coordinates are sorted as tuples: (x, y) — compare x first, then y.
     */
    public static int[] selectSquare(int mx, int my) {
        for (int i = 0; i < gameGrid.columns; i++) {
            for (int j = 0; j < gameGrid.rows; j++) {
                List<int[]> coords = gameGrid.coordMatrix.get(i).get(j);
                int start = 0, end = coords.size() - 1;
                while (start <= end) {
                    int mid = (start + end) / 2;
                    int[] c = coords.get(mid);
                    int cmp = compareCoord(mx, my, c[0], c[1]);
                    if (cmp == 0) return new int[]{i, j};
                    if (cmp < 0) end = mid - 1;
                    else start = mid + 1;
                }
            }
        }
        return null;
    }

    /** Lexicographic comparison of pixel coords (x first, then y) — matches Python tuple compare. */
    private static int compareCoord(int ax, int ay, int bx, int by) {
        if (ax != bx) return Integer.compare(ax, bx);
        return Integer.compare(ay, by);
    }

    /**
     * Port of Python getSurrounding() — up to 3-ring of neighbors.
     * The loop conditions are equivalent to:
     *   outer: skip only i==0 && j==0 (handled separately as self)
     *   inner: only add diagonal variants when i!=0 && j!=0
     */
    public static List<int[]> getSurroundingCoords(int column, int row) {
        List<int[]> surrounding = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i == 0 && j == 0) {
                    surrounding.add(new int[]{column, row});
                } else {
                    if (column + i < gameGrid.columns && row - j >= 0)
                        surrounding.add(new int[]{column + i, row - j});
                    if (column - i >= 0 && row + j < gameGrid.rows)
                        surrounding.add(new int[]{column - i, row + j});
                    if (i != 0 && j != 0) {
                        if (column + i < gameGrid.columns && row + j < gameGrid.rows)
                            surrounding.add(new int[]{column + i, row + j});
                        if (column - i >= 0 && row - j >= 0)
                            surrounding.add(new int[]{column - i, row - j});
                    }
                }
            }
        }
        return surrounding;
    }

    /**
     * Port of Python getAdjacent() — self + up to 8 immediate neighbors.
     * adjacent[0] is always the square itself (column, row).
     */
    public static List<int[]> getAdjacentCoords(int column, int row) {
        List<int[]> adjacent = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                if (i == 0 && j == 0) {
                    adjacent.add(new int[]{column, row});
                } else {
                    // "axis" neighbors: (col+i, row-j) and (col-i, row+j)
                    if (column + i < gameGrid.columns && row - j >= 0)
                        adjacent.add(new int[]{column + i, row - j});
                    if (column - i >= 0 && row + j < gameGrid.rows)
                        adjacent.add(new int[]{column - i, row + j});
                    // "diagonal" neighbors: only when both i!=0 and j!=0
                    if (i != 0 && j != 0) {
                        if (column + i < gameGrid.columns && row + j < gameGrid.rows)
                            adjacent.add(new int[]{column + i, row + j});
                        if (column - i >= 0 && row - j >= 0)
                            adjacent.add(new int[]{column - i, row - j});
                    }
                }
            }
        }
        return adjacent;
    }

    /** Port of Python removeSquare(). */
    public static boolean removeSquare(int[] square) {
        Square sq = gameGrid.matrix[square[0]][square[1]];
        if (sq.removed || sq.flagged) return false;
        sq.removed = true;
        gameGrid.numRemoved++;
        clearField(square);
        return true;
    }

    /** Port of Python clearField() — recursive flood fill for zero squares. */
    public static void clearField(int[] square) {
        if (gameGrid.matrix[square[0]][square[1]].closeMines != 0) return;
        List<int[]> adjacent = getAdjacentCoords(square[0], square[1]);
        for (int[] a : adjacent) {
            if (!gameGrid.matrix[a[0]][a[1]].flagged) {
                removeSquare(a);
            }
        }
    }

    /** Port of Python placeFlag(). */
    public static boolean placeFlag(int[] square) {
        Square sq = gameGrid.matrix[square[0]][square[1]];
        if (!sq.removed) {
            if (!sq.flagged) {
                if (gameGrid.numFlags > 0) {
                    gameGrid.numFlags--;
                    sq.flagged = true;
                    return true;
                }
            } else {
                gameGrid.numFlags++;
                sq.flagged = false;
            }
        }
        return false;
    }

    // =========================================================================
    // Image loading
    // =========================================================================

    private static File resolveBaseDir() {
        try {
            File loc = new File(Minesweeper.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File dir = loc.isFile() ? loc.getParentFile() : loc;
            for (int i = 0; i < 8; i++) {
                if (new File(dir, "assets").isDirectory()) return dir;
                if (dir.getParentFile() == null) break;
                dir = dir.getParentFile();
            }
        } catch (Exception ignored) {}
        return new File(System.getProperty("user.dir"));
    }

    private static void loadImages(File baseDir) {
        Corner_image        = load(baseDir, "corner.png");
        CornerFlip_image    = load(baseDir, "cornerFlip.png");
        Square_image        = load(baseDir, "square.png");
        Zero_image          = load(baseDir, "NOsquare.png");
        Flag_image          = load(baseDir, "flag.png");
        Wrong_image         = load(baseDir, "wrong.png");
        Mine_image          = load(baseDir, "mine.png");
        Explode_image       = load(baseDir, "explode.png");
        Happy_image         = load(baseDir, "happy.png");
        HappyClick_image    = load(baseDir, "happyClick.png");
        Sad_image           = load(baseDir, "sad.png");
        Click_image         = load(baseDir, "clicking.png");
        Settings_image      = load(baseDir, "settings.png");
        SettingsClick_image = load(baseDir, "settingsClick.png");
        Menu_image          = load(baseDir, "menu.png");
        B1_image            = load(baseDir, "b1.png");
        B1click_image       = load(baseDir, "b1click.png");
        B2_image            = load(baseDir, "b2.png");
        B3_image            = load(baseDir, "b3.png");
        I1_image            = load(baseDir, "i1.png");
        I2_image            = load(baseDir, "i2.png");
        E_image             = load(baseDir, "expert.png");
        Won_image           = load(baseDir, "won.png");
        One_image           = load(baseDir, "one.png");
        Two_image           = load(baseDir, "two.png");
        Three_image         = load(baseDir, "three.png");
        Four_image          = load(baseDir, "four.png");
        Five_image          = load(baseDir, "five.png");
        Six_image           = load(baseDir, "six.png");
        Seven_image         = load(baseDir, "seven.png");
        Eight_image         = load(baseDir, "eight.png");
    }

    private static BufferedImage load(File baseDir, String name) {
        try {
            return ImageIO.read(new File(baseDir, "assets/" + name));
        } catch (IOException e) {
            System.err.println("Failed to load image: " + name + " — " + e.getMessage());
            // Return magenta placeholder so game still runs
            BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics g = img.getGraphics();
            g.setColor(java.awt.Color.MAGENTA);
            g.fillRect(0, 0, 30, 30);
            g.dispose();
            return img;
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /** Convert Swing mouse button to pygame-style (1=left, 2=middle, 3=right). */
    private static int swingToButton(int swingButton) {
        switch (swingButton) {
            case MouseEvent.BUTTON1: return 1;
            case MouseEvent.BUTTON2: return 2;
            case MouseEvent.BUTTON3: return 3;
            default: return swingButton;
        }
    }
}

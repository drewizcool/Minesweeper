package minesweeper;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * Extends JPanel. Replaces the Python Screen class.
 * paintComponent() performs all blitting (replaces pygame blit + display.flip()).
 * All state mutations happen on the game thread; repaint() posts to EDT.
 */
public class Screen extends JPanel {

    private static final Color GREY       = new Color(185, 185, 185);
    private static final Color LIGHT_GREY = new Color(195, 195, 195);
    private static final Color DARK_GREY  = new Color(127, 127, 127);
    private static final Color OFF_WHITE  = new Color(243, 243, 243);

    public final Grid grid;
    public Button resetButton;
    public Button settingsButton;

    // Background drawn once in constructor
    private final BufferedImage background;

    // Paint state — written by game thread, read by EDT in paintComponent
    private volatile boolean paintPlaying = true;
    private volatile Square paintExploded = null;
    private volatile boolean paintWin = false;

    // Overlays for hold functions
    private volatile boolean overlayClickActive = false;
    // List of pixel {x,y} positions to draw Zero_image during hold
    private volatile List<int[]> overlayZeroPositions = Collections.emptyList();

    // Settings menu overlay
    private volatile boolean settingsActive = false;
    private volatile SettingsMenu settingsMenu = null;

    public Screen(Grid grid) {
        this.grid = grid;
        int totalW = grid.W + Minesweeper.ORIGIN_X * 2;
        int totalH = grid.H + Minesweeper.ORIGIN_Y + 20;
        setPreferredSize(new java.awt.Dimension(totalW, totalH));

        resetButton = new Button(
            new int[]{grid.W / 2, 10}, 40, 40,
            Minesweeper.Happy_image, Minesweeper.HappyClick_image);
        settingsButton = new Button(
            new int[]{grid.W - 16, 10}, 40, 40,
            Minesweeper.Settings_image, Minesweeper.SettingsClick_image);

        // Draw static 3D border background once
        background = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_RGB);
        Graphics2D bg = background.createGraphics();

        bg.setColor(GREY);
        bg.fillRect(0, 0, totalW, totalH);

        bg.setColor(LIGHT_GREY);
        bg.fillRect(Minesweeper.ORIGIN_X, Minesweeper.ORIGIN_Y, grid.W, grid.H);

        bg.setColor(DARK_GREY);
        bg.fillRect(Minesweeper.ORIGIN_X - 4, Minesweeper.ORIGIN_Y - 4, 4, grid.H + 4);

        bg.setColor(OFF_WHITE);
        bg.fillRect(Minesweeper.ORIGIN_X, Minesweeper.ORIGIN_Y + grid.H, grid.W, 4);

        bg.setColor(DARK_GREY);
        bg.fillRect(Minesweeper.ORIGIN_X, Minesweeper.ORIGIN_Y - 4, grid.W, 4);

        bg.setColor(OFF_WHITE);
        bg.fillRect(grid.W + Minesweeper.ORIGIN_X, Minesweeper.ORIGIN_Y, 4, grid.H + 4);

        bg.drawImage(Minesweeper.Corner_image,     grid.W + Minesweeper.ORIGIN_X,          Minesweeper.ORIGIN_Y - 4, null);
        bg.drawImage(Minesweeper.Corner_image,     Minesweeper.ORIGIN_X - 4,               Minesweeper.ORIGIN_Y + grid.H, null);
        bg.drawImage(Minesweeper.CornerFlip_image, 0,                                      grid.H + Minesweeper.ORIGIN_Y + 16, null);
        bg.drawImage(Minesweeper.CornerFlip_image, grid.W + Minesweeper.ORIGIN_X * 2 - 4, 0, null);

        bg.setColor(OFF_WHITE);
        bg.fillRect(0, 0, 4, grid.H + Minesweeper.ORIGIN_Y + 16);
        bg.fillRect(0, 0, grid.W + Minesweeper.ORIGIN_X + 16, 4);

        bg.setColor(DARK_GREY);
        bg.fillRect(4, grid.H + Minesweeper.ORIGIN_Y + 16, totalW, 4);
        bg.fillRect(grid.W + Minesweeper.ORIGIN_X * 2 - 4, 4, 4, grid.H + Minesweeper.ORIGIN_Y + 16);

        bg.dispose();
    }

    /**
     * Update render state and trigger repaint.
     * Replaces: GameScreen.draw(playing, exploded, win) + pygame.display.flip()
     */
    public void draw(boolean playing, Square exploded, boolean win) {
        // Update reset button image for win state
        if (win) {
            resetButton.image = Minesweeper.Won_image;
        } else if (!playing && exploded != null) {
            resetButton.image = Minesweeper.Sad_image;
        } else {
            resetButton.image = Minesweeper.Happy_image;
        }
        this.paintPlaying = playing;
        this.paintExploded = exploded;
        this.paintWin = win;
        this.overlayClickActive = false;
        this.overlayZeroPositions = Collections.emptyList();
        repaint();
    }

    /** Show Click_image at reset button (scared face during hold). */
    public void showClickOverlay() {
        this.overlayClickActive = true;
        repaint();
    }

    /** Show Zero_image at these pixel positions (used during hold). */
    public void showZeroAtPositions(List<int[]> positions) {
        this.overlayZeroPositions = positions;
        repaint();
    }

    public void clearOverlays() {
        this.overlayClickActive = false;
        this.overlayZeroPositions = Collections.emptyList();
        repaint();
    }

    /** Show the settings menu overlay. Port of Python GameSettings.draw() */
    public void drawSettings(SettingsMenu settings) {
        this.settingsMenu = settings;
        this.settingsActive = true;
        repaint();
    }

    public void clearSettings() {
        this.settingsActive = false;
        this.settingsMenu = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Snapshot volatile state for consistent paint
        boolean playing = paintPlaying;
        Square exploded = paintExploded;
        boolean win = paintWin;
        boolean clickActive = overlayClickActive;
        List<int[]> zeroPositions = overlayZeroPositions;
        boolean showSettings = settingsActive;
        SettingsMenu menu = settingsMenu;

        // Draw static background
        g2.drawImage(background, 0, 0, null);

        // Draw reset and settings buttons
        BufferedImage resetImg = clickActive ? Minesweeper.Click_image : resetButton.image;
        g2.drawImage(resetImg, resetButton.xy[0], resetButton.xy[1], null);
        g2.drawImage(settingsButton.image, settingsButton.xy[0], settingsButton.xy[1], null);

        // Draw grid squares
        for (int col = 0; col < grid.columns; col++) {
            for (int row = 0; row < grid.rows; row++) {
                Square sq = grid.matrix[col][row];

                if (playing) {
                    if (sq.removed) {
                        if (!sq.mine) {
                            g2.drawImage(numberImage(sq.closeMines), sq.x, sq.y, null);
                        }
                    } else if (!sq.flagged) {
                        g2.drawImage(Minesweeper.Square_image, sq.x, sq.y, null);
                    } else {
                        g2.drawImage(Minesweeper.Flag_image, sq.x, sq.y, null);
                    }
                } else {
                    // Game over
                    if (sq == exploded) {
                        g2.drawImage(Minesweeper.Explode_image, sq.x, sq.y, null);
                    } else if (!sq.removed && sq.mine && !sq.flagged) {
                        g2.drawImage(Minesweeper.Mine_image, sq.x, sq.y, null);
                    } else if (!sq.removed && !sq.mine && sq.flagged) {
                        g2.drawImage(Minesweeper.Wrong_image, sq.x, sq.y, null);
                    }

                    if (win) {
                        if (sq.removed && !sq.mine) {
                            g2.drawImage(numberImage(sq.closeMines), sq.x, sq.y, null);
                        } else if (!sq.removed && sq.mine) {
                            g2.drawImage(Minesweeper.Flag_image, sq.x, sq.y, null);
                        }
                    }
                }
            }
        }

        // Overlays from hold functions (drawn on top of grid)
        for (int[] pos : zeroPositions) {
            g2.drawImage(Minesweeper.Zero_image, pos[0], pos[1], null);
        }

        // Settings menu overlay
        if (showSettings && menu != null) {
            int menuX = grid.W - 160;
            int menuY = Minesweeper.ORIGIN_Y;
            g2.drawImage(Minesweeper.Menu_image, menuX, menuY, null);
            for (Button btn : menu.buttons) {
                g2.drawImage(btn.image, btn.xy[0], btn.xy[1], null);
            }
        }
    }

    private BufferedImage numberImage(int n) {
        switch (n) {
            case 1: return Minesweeper.One_image;
            case 2: return Minesweeper.Two_image;
            case 3: return Minesweeper.Three_image;
            case 4: return Minesweeper.Four_image;
            case 5: return Minesweeper.Five_image;
            case 6: return Minesweeper.Six_image;
            case 7: return Minesweeper.Seven_image;
            case 8: return Minesweeper.Eight_image;
            default: return Minesweeper.Zero_image;
        }
    }
}

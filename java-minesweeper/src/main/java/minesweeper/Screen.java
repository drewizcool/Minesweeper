package minesweeper;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

public class Screen extends JPanel {

    private static final Color GREY       = new Color(185, 185, 185);
    private static final Color LIGHT_GREY = new Color(195, 195, 195);
    private static final Color DARK_GREY  = new Color(127, 127, 127);
    private static final Color OFF_WHITE  = new Color(243, 243, 243);

    public final Grid grid;
    public final ImageAssets images;
    public final boolean dungeonMode;
    public Button resetButton;
    public Button settingsButton;
    public Button bagButton; // null when not in dungeon mode

    // Background drawn once in constructor
    private final BufferedImage background;

    // Paint state — written by game thread, read by EDT in paintComponent
    private volatile boolean paintPlaying = true;
    private volatile Square paintExploded = null;
    private volatile boolean paintWin = false;

    // Overlays for hold functions
    private volatile boolean overlayClickActive = false;
    private volatile List<int[]> overlayZeroPositions = Collections.emptyList();

    // Player sprite (dungeon mode)
    private volatile Player paintPlayer = null;
    private volatile boolean paintFlagMode = false;

    // Settings menu overlay
    private volatile boolean settingsActive = false;
    private volatile SettingsMenu settingsMenu = null;

    public Screen(Grid grid, ImageAssets images, boolean dungeonMode) {
        this.grid = grid;
        this.images = images;
        this.dungeonMode = dungeonMode;
        int totalW = grid.W + grid.originX * 2;
        int totalH = grid.H + grid.originY + 20;
        setPreferredSize(new java.awt.Dimension(totalW, totalH));

        resetButton = new Button(
            new int[]{grid.originX + (grid.columns / 2) * Grid.SQUARE_SIZE,
                      grid.originY - 2 * Grid.SQUARE_SIZE}, 30, 30,
            images.happy, images.happyClick);
        settingsButton = new Button(
            new int[]{grid.originX + (grid.columns - 1) * Grid.SQUARE_SIZE,
                      grid.originY - 2 * Grid.SQUARE_SIZE}, 30, 30,
            images.settings, images.settingsClick);

        if (dungeonMode) {
            bagButton = new Button(
                new int[]{grid.originX + (grid.columns - 2) * Grid.SQUARE_SIZE,
                          grid.originY - 2 * Grid.SQUARE_SIZE}, 30, 30,
                images.bag, images.bagClick);
        }

        // Draw static 3D border background once
        background = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_RGB);
        Graphics2D bg = background.createGraphics();

        bg.setColor(GREY);
        bg.fillRect(0, 0, totalW, totalH);

        bg.setColor(LIGHT_GREY);
        bg.fillRect(grid.originX, grid.originY, grid.W, grid.H);

        bg.setColor(DARK_GREY);
        bg.fillRect(grid.originX - 4, grid.originY - 4, 4, grid.H + 4);

        bg.setColor(OFF_WHITE);
        bg.fillRect(grid.originX, grid.originY + grid.H, grid.W, 4);

        bg.setColor(DARK_GREY);
        bg.fillRect(grid.originX, grid.originY - 4, grid.W, 4);

        bg.setColor(OFF_WHITE);
        bg.fillRect(grid.W + grid.originX, grid.originY, 4, grid.H + 4);

        bg.drawImage(images.corner,     grid.W + grid.originX,          grid.originY - 4, null);
        bg.drawImage(images.corner,     grid.originX - 4,               grid.originY + grid.H, null);
        bg.drawImage(images.cornerFlip, 0,                              grid.H + grid.originY + 16, null);
        bg.drawImage(images.cornerFlip, grid.W + grid.originX * 2 - 4, 0, null);

        bg.setColor(OFF_WHITE);
        bg.fillRect(0, 0, 4, grid.H + grid.originY + 16);
        bg.fillRect(0, 0, grid.W + grid.originX + 16, 4);

        bg.setColor(DARK_GREY);
        bg.fillRect(4, grid.H + grid.originY + 16, totalW, 4);
        bg.fillRect(grid.W + grid.originX * 2 - 4, 4, 4, grid.H + grid.originY + 16);

        bg.dispose();
    }

    public void draw(boolean playing, Square exploded, boolean win, Player player, boolean flagMode) {
        if (win) {
            resetButton.image = images.won;
        } else if (!playing && exploded != null) {
            resetButton.image = images.sad;
        } else {
            resetButton.image = images.happy;
        }
        this.paintPlaying = playing;
        this.paintExploded = exploded;
        this.paintWin = win;
        this.paintPlayer = player;
        this.paintFlagMode = flagMode;
        this.overlayClickActive = false;
        this.overlayZeroPositions = Collections.emptyList();
        repaint();
    }

    public void showClickOverlay() {
        this.overlayClickActive = true;
        repaint();
    }

    public void showZeroAtPositions(List<int[]> positions) {
        this.overlayZeroPositions = positions;
        repaint();
    }

    public void clearOverlays() {
        this.overlayClickActive = false;
        this.overlayZeroPositions = Collections.emptyList();
        repaint();
    }

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

        boolean playing = paintPlaying;
        Square exploded = paintExploded;
        boolean win = paintWin;
        boolean clickActive = overlayClickActive;
        List<int[]> zeroPositions = overlayZeroPositions;
        boolean showSettings = settingsActive;
        SettingsMenu menu = settingsMenu;

        g2.drawImage(background, 0, 0, null);

        BufferedImage resetImg = clickActive ? images.click : resetButton.image;
        g2.drawImage(resetImg, resetButton.xy[0], resetButton.xy[1], null);
        g2.drawImage(settingsButton.image, settingsButton.xy[0], settingsButton.xy[1], null);
        if (bagButton != null) {
            g2.drawImage(bagButton.image, bagButton.xy[0], bagButton.xy[1], null);
        }

        for (int col = 0; col < grid.columns; col++) {
            for (int row = 0; row < grid.rows; row++) {
                Square sq = grid.matrix[col][row];

                if (playing) {
                    if (sq.removed) {
                        if (!sq.mine) {
                            if (sq.isStairs) {
                                BufferedImage stairImg = sq.stairMarked ? images.greenFlag
                                        : (sq.stairUnlocked ? images.stairs : images.lockedStair);
                                g2.drawImage(stairImg, sq.x, sq.y, null);
                            } else if (sq.isKey && !sq.keyCollected) {
                                g2.drawImage(images.key, sq.x, sq.y, null);
                            } else {
                                g2.drawImage(images.numberImage(sq.closeMines), sq.x, sq.y, null);
                            }
                        }
                    } else if (!sq.flagged) {
                        g2.drawImage(images.square, sq.x, sq.y, null);
                    } else {
                        g2.drawImage(images.flag, sq.x, sq.y, null);
                    }
                } else {
                    if (sq == exploded) {
                        g2.drawImage(images.explode, sq.x, sq.y, null);
                    } else if (!sq.removed && sq.mine && !sq.flagged) {
                        g2.drawImage(images.mine, sq.x, sq.y, null);
                    } else if (!sq.removed && !sq.mine && sq.flagged) {
                        g2.drawImage(images.wrong, sq.x, sq.y, null);
                    }

                    if (win) {
                        if (sq.removed && !sq.mine) {
                            g2.drawImage(images.numberImage(sq.closeMines), sq.x, sq.y, null);
                        } else if (!sq.removed && sq.mine) {
                            g2.drawImage(images.flag, sq.x, sq.y, null);
                        }
                    }
                }
            }
        }

        // Gridlines
        g2.setColor(DARK_GREY);
        g2.setStroke(new java.awt.BasicStroke(2));
        for (int x = grid.originX; x <= grid.originX + grid.W; x += Grid.SQUARE_SIZE) {
            g2.drawLine(x, grid.originY, x, grid.originY + grid.H);
        }
        for (int y = grid.originY; y <= grid.originY + grid.H; y += Grid.SQUARE_SIZE) {
            g2.drawLine(grid.originX, y, grid.originX + grid.W, y);
        }
        g2.setStroke(new java.awt.BasicStroke(1));

        // Draw player sprite in dungeon mode
        Player playerSnap = paintPlayer;
        if (playerSnap != null && playing) {
            boolean fm = paintFlagMode;
            BufferedImage pImg;
            if (playerSnap.facingDx > 0) pImg = fm ? images.playerRightFlag : images.playerRight;
            else if (playerSnap.facingDx < 0) pImg = fm ? images.playerLeftFlag : images.playerLeft;
            else if (playerSnap.facingDy < 0) pImg = fm ? images.playerUpFlag : images.playerUp;
            else pImg = fm ? images.playerDownFlag : images.playerDown;
            g2.drawImage(pImg, (int) playerSnap.x, (int) playerSnap.y, null);
        }

        for (int[] pos : zeroPositions) {
            g2.drawImage(images.zero, pos[0], pos[1], null);
        }

        if (showSettings && menu != null) {
            int menuX = grid.W - 160;
            int menuY = grid.originY;
            g2.drawImage(images.menu, menuX, menuY, null);
            for (Button btn : menu.buttons) {
                g2.drawImage(btn.image, btn.xy[0], btn.xy[1], null);
            }
        }
    }
}

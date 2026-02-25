package minesweeper;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class HudPanel extends JPanel {

    private static final Color GREY       = new Color(185, 185, 185);
    private static final Color LIGHT_GREY = new Color(195, 195, 195);
    private static final Color DARK_GREY  = new Color(127, 127, 127);
    private static final Color OFF_WHITE  = new Color(243, 243, 243);

    private static final int GRID_W = Grid.DIFFICULTIES[1][0]; // 240
    private static final int GRID_H = Grid.DIFFICULTIES[1][1]; // 240

    private static final int BORDER_X = Minesweeper.ORIGIN_X;
    private static final int BORDER_TOP = 20;
    private static final int BORDER_BOTTOM = 20;

    public static final int PANEL_W = GRID_W + BORDER_X * 2;
    public static final int PANEL_H = GRID_H + BORDER_TOP + BORDER_BOTTOM;

    private final ImageAssets images;
    private final BufferedImage background;
    private volatile int keysFound = 0;

    public HudPanel(ImageAssets images) {
        this.images = images;
        setPreferredSize(new Dimension(PANEL_W, PANEL_H));
        background = buildBackground();
    }

    public void setKeysFound(int keysFound) {
        this.keysFound = keysFound;
    }

    private BufferedImage buildBackground() {
        BufferedImage bg = new BufferedImage(PANEL_W, PANEL_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bg.createGraphics();

        g.setColor(GREY);
        g.fillRect(0, 0, PANEL_W, PANEL_H);

        g.setColor(LIGHT_GREY);
        g.fillRect(BORDER_X, BORDER_TOP, GRID_W, GRID_H);

        g.setColor(DARK_GREY);
        g.fillRect(BORDER_X - 4, BORDER_TOP - 4, 4, GRID_H + 4);
        g.fillRect(BORDER_X, BORDER_TOP - 4, GRID_W, 4);

        g.setColor(OFF_WHITE);
        g.fillRect(BORDER_X, BORDER_TOP + GRID_H, GRID_W, 4);
        g.fillRect(GRID_W + BORDER_X, BORDER_TOP, 4, GRID_H + 4);

        g.drawImage(images.corner, GRID_W + BORDER_X, BORDER_TOP - 4, null);
        g.drawImage(images.corner, BORDER_X - 4, BORDER_TOP + GRID_H, null);
        g.drawImage(images.cornerFlip, 0, GRID_H + BORDER_TOP + 16, null);
        g.drawImage(images.cornerFlip, GRID_W + BORDER_X * 2 - 4, 0, null);

        g.setColor(OFF_WHITE);
        g.fillRect(0, 0, 4, GRID_H + BORDER_TOP + 16);
        g.fillRect(0, 0, GRID_W + BORDER_X + 16, 4);

        g.setColor(DARK_GREY);
        g.fillRect(4, GRID_H + BORDER_TOP + 16, PANEL_W, 4);
        g.fillRect(GRID_W + BORDER_X * 2 - 4, 4, 4, GRID_H + BORDER_TOP + 16);

        g.dispose();
        return bg;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.drawImage(background, 0, 0, null);

        int found = keysFound;
        int cols = GRID_W / Grid.SQUARE_SIZE;
        int rows = GRID_H / Grid.SQUARE_SIZE;
        for (int i = 0; i < found && i < cols * rows; i++) {
            int col = i % cols;
            int row = i / cols;
            int px = BORDER_X + col * Grid.SQUARE_SIZE;
            int py = BORDER_TOP + row * Grid.SQUARE_SIZE;
            g2.drawImage(images.key, px, py, null);
        }

        g2.setColor(DARK_GREY);
        g2.setStroke(new java.awt.BasicStroke(2));
        for (int x = BORDER_X; x <= BORDER_X + GRID_W; x += Grid.SQUARE_SIZE) {
            g2.drawLine(x, BORDER_TOP, x, BORDER_TOP + GRID_H);
        }
        for (int y = BORDER_TOP; y <= BORDER_TOP + GRID_H; y += Grid.SQUARE_SIZE) {
            g2.drawLine(BORDER_X, y, BORDER_X + GRID_W, y);
        }
        g2.setStroke(new java.awt.BasicStroke(1));
    }
}

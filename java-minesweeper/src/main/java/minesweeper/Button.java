package minesweeper;

import java.awt.image.BufferedImage;

/**
 * Direct port of the Python Button class.
 * The Python 'range' list (all pixel coords) is replaced by a bounds check.
 */
public class Button {
    public int[] xy;       // top-left pixel position {x, y}
    public int width;
    public int height;
    public BufferedImage image;
    public BufferedImage clickImage;
    public int code;       // used by SettingsMenu buttons to identify difficulty

    public Button(int[] xy, int width, int height, BufferedImage image, BufferedImage clickImage, int code) {
        this.xy = xy;
        this.width = width;
        this.height = height;
        this.image = image;
        this.clickImage = clickImage;
        this.code = code;
    }

    public Button(int[] xy, int width, int height, BufferedImage image, BufferedImage clickImage) {
        this(xy, width, height, image, clickImage, 0);
    }

    /**
     * Replaces: mouse_pos in button.range
     */
    public boolean contains(int x, int y) {
        return x >= xy[0] && x < xy[0] + width
            && y >= xy[1] && y < xy[1] + height;
    }
}

package minesweeper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageAssets {

    public final BufferedImage corner;
    public final BufferedImage cornerFlip;
    public final BufferedImage square;
    public final BufferedImage zero;
    public final BufferedImage flag;
    public final BufferedImage wrong;
    public final BufferedImage mine;
    public final BufferedImage explode;
    public final BufferedImage happy;
    public final BufferedImage happyClick;
    public final BufferedImage sad;
    public final BufferedImage click;
    public final BufferedImage settings;
    public final BufferedImage settingsClick;
    public final BufferedImage menu;
    public final BufferedImage b1;
    public final BufferedImage b1click;
    public final BufferedImage b2;
    public final BufferedImage b3;
    public final BufferedImage i1;
    public final BufferedImage i2;
    public final BufferedImage expert;
    public final BufferedImage dungeon;
    public final BufferedImage won;
    public final BufferedImage stairs;
    public final BufferedImage lockedStair;
    public final BufferedImage greenFlag;
    public final BufferedImage key;
    public final BufferedImage bag;
    public final BufferedImage bagClick;
    public final BufferedImage one;
    public final BufferedImage two;
    public final BufferedImage three;
    public final BufferedImage four;
    public final BufferedImage five;
    public final BufferedImage six;
    public final BufferedImage seven;
    public final BufferedImage eight;
    public final BufferedImage player;
    public final BufferedImage playerUp;
    public final BufferedImage playerDown;
    public final BufferedImage playerLeft;
    public final BufferedImage playerRight;
    public final BufferedImage playerUpFlag;
    public final BufferedImage playerDownFlag;
    public final BufferedImage playerLeftFlag;
    public final BufferedImage playerRightFlag;

    public ImageAssets(File baseDir) {
        corner        = load(baseDir, "corner.png");
        cornerFlip    = load(baseDir, "cornerFlip.png");
        square        = load(baseDir, "square.png");
        zero          = load(baseDir, "NOsquare.png");
        flag          = load(baseDir, "flag.png");
        wrong         = load(baseDir, "wrong.png");
        mine          = load(baseDir, "mine.png");
        explode       = load(baseDir, "explode.png");
        happy         = load(baseDir, "happy.png");
        happyClick    = load(baseDir, "happyClick.png");
        sad           = load(baseDir, "sad.png");
        click         = load(baseDir, "clicking.png");
        settings      = load(baseDir, "settings.png");
        settingsClick = load(baseDir, "settingsClick.png");
        menu          = load(baseDir, "menu.png");
        b1            = load(baseDir, "b1.png");
        b1click       = load(baseDir, "b1click.png");
        b2            = load(baseDir, "b2.png");
        b3            = load(baseDir, "b3.png");
        i1            = load(baseDir, "i1.png");
        i2            = load(baseDir, "i2.png");
        expert        = load(baseDir, "expert.png");
        dungeon       = load(baseDir, "dungeon.png");
        won           = load(baseDir, "won.png");
        stairs        = load(baseDir, "stairs.png");
        lockedStair   = load(baseDir, "lockedStair.png");
        greenFlag     = load(baseDir, "greenFlag.png");
        key           = load(baseDir, "key.png");
        bag           = load(baseDir, "bag.png");
        bagClick      = load(baseDir, "bagClick.png");
        one           = load(baseDir, "one.png");
        two           = load(baseDir, "two.png");
        three         = load(baseDir, "three.png");
        four          = load(baseDir, "four.png");
        five          = load(baseDir, "five.png");
        six           = load(baseDir, "six.png");
        seven         = load(baseDir, "seven.png");
        eight         = load(baseDir, "eight.png");
        player        = load(baseDir, "player.png");
        playerUp      = load(baseDir, "playerUp.png");
        playerDown    = load(baseDir, "playerDown.png");
        playerLeft    = load(baseDir, "playerLeft.png");
        playerRight   = load(baseDir, "playerRight.png");
        playerUpFlag    = load(baseDir, "playerUpFlag.png");
        playerDownFlag  = load(baseDir, "playerDownFlag.png");
        playerLeftFlag  = load(baseDir, "playerLeftFlag.png");
        playerRightFlag = load(baseDir, "playerRightFlag.png");
    }

    public BufferedImage numberImage(int n) {
        switch (n) {
            case 1: return one;
            case 2: return two;
            case 3: return three;
            case 4: return four;
            case 5: return five;
            case 6: return six;
            case 7: return seven;
            case 8: return eight;
            default: return zero;
        }
    }

    static File resolveBaseDir() {
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

    private static BufferedImage load(File baseDir, String name) {
        try {
            return ImageIO.read(new File(baseDir, "assets/" + name));
        } catch (IOException e) {
            System.err.println("Failed to load image: " + name + " — " + e.getMessage());
            BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics g = img.getGraphics();
            g.setColor(java.awt.Color.MAGENTA);
            g.fillRect(0, 0, 30, 30);
            g.dispose();
            return img;
        }
    }
}

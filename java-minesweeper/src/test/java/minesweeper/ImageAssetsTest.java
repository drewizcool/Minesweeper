package minesweeper;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ImageAssetsTest {

    @Test
    void resolveBaseDir_findsAssetsDirectory() {
        File baseDir = ImageAssets.resolveBaseDir();
        assertTrue(new File(baseDir, "assets").isDirectory(),
            "resolveBaseDir() should find a directory containing assets/");
    }

    @Test
    void numberImage_returnsZeroForDefault() {
        ImageAssets images = new ImageAssets(ImageAssets.resolveBaseDir());
        BufferedImage result = images.numberImage(0);
        assertSame(images.zero, result);
    }

    @Test
    void numberImage_returnsCorrectForEachDigit() {
        ImageAssets images = new ImageAssets(ImageAssets.resolveBaseDir());
        assertSame(images.one,   images.numberImage(1));
        assertSame(images.two,   images.numberImage(2));
        assertSame(images.three, images.numberImage(3));
        assertSame(images.four,  images.numberImage(4));
        assertSame(images.five,  images.numberImage(5));
        assertSame(images.six,   images.numberImage(6));
        assertSame(images.seven, images.numberImage(7));
        assertSame(images.eight, images.numberImage(8));
    }

    @Test
    void numberImage_negativeReturnsZero() {
        ImageAssets images = new ImageAssets(ImageAssets.resolveBaseDir());
        assertSame(images.zero, images.numberImage(-1));
    }

    @Test
    void allImagesAreNonNull() {
        ImageAssets images = new ImageAssets(ImageAssets.resolveBaseDir());
        assertNotNull(images.corner);
        assertNotNull(images.happy);
        assertNotNull(images.mine);
        assertNotNull(images.key);
        assertNotNull(images.stairs);
    }
}

package fr.turgot.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PosterStorageTest {
    private static final String STORAGE_PROPERTY = "CYNEMMA_IMAGE_DIR";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void storesValidatedPngOutsideTheApplicationAndReturnsStableRelativePath()
            throws Exception {
        Path storage = temporaryFolder.newFolder("posters").toPath();
        String previousValue = System.getProperty(STORAGE_PROPERTY);
        try {
            System.setProperty(STORAGE_PROPERTY, storage.toString());

            String relativePath = PosterStorage.save(pngBytes());

            assertTrue(relativePath.matches("image/[0-9a-f-]{36}\\.png"));
            assertTrue(Files.isRegularFile(storage.resolve(relativePath.substring("image/".length()))));
            assertEquals(
                    storage.resolve(relativePath.substring("image/".length())).toRealPath(),
                    PosterStorage.resolve(relativePath, null));
            assertEquals("image/png", PosterStorage.contentType(relativePath));
        } finally {
            restoreProperty(previousValue);
        }
    }

    @Test
    public void rejectsPathsOutsideTheManagedImageDirectory() {
        assertTrue(PosterStorage.isManagedPath("image/cover.png"));
        assertFalse(PosterStorage.isManagedPath("../cover.png"));
        assertFalse(PosterStorage.isManagedPath("image/../../WEB-INF/web.xml"));
        assertFalse(PosterStorage.isManagedPath("image/folder/cover.png"));
        assertFalse(PosterStorage.isManagedPath("image/cover.svg"));
    }

    @Test(expected = PosterStorage.InvalidImageException.class)
    public void rejectsNonImageBytes() throws Exception {
        PosterStorage.save(new byte[] {1, 2, 3});
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private void restoreProperty(String previousValue) {
        if (previousValue == null) {
            System.clearProperty(STORAGE_PROPERTY);
        } else {
            System.setProperty(STORAGE_PROPERTY, previousValue);
        }
    }
}

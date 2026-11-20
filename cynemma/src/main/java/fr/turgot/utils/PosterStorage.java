package fr.turgot.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import fr.turgot.service.LocalConfiguration;
import jakarta.servlet.ServletContext;

public final class PosterStorage {
    private static final String STORAGE_DIRECTORY = "CYNEMMA_IMAGE_DIR";
    private static final String RELATIVE_DIRECTORY = "image";
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;

    private PosterStorage() {
    }

    public static String save(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new InvalidImageException("L'image est vide ou dépasse la taille maximale autorisée.");
        }

        BufferedImage image = decode(bytes);
        Path directory = storageDirectory();
        Files.createDirectories(directory);
        Path realDirectory = directory.toRealPath();
        String fileName = UUID.randomUUID() + ".png";
        Path destination = realDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(realDirectory)) {
            throw new IOException("Chemin de stockage d'affiche invalide.");
        }

        Path temporary = Files.createTempFile(realDirectory, ".poster-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                if (!ImageIO.write(image, "png", output)) {
                    throw new IOException("Impossible d'encoder l'affiche en PNG.");
                }
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, destination);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return RELATIVE_DIRECTORY + "/" + fileName;
    }

    public static String save(InputStream input) throws IOException {
        if (input == null) {
            throw new InvalidImageException("Aucune image n'a été fournie.");
        }
        byte[] bytes = input.readNBytes((int) MAX_IMAGE_BYTES + 1);
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new InvalidImageException("L'image dépasse la taille maximale autorisée.");
        }
        return save(bytes);
    }

    public static Path resolve(String relativePath, ServletContext servletContext) throws IOException {
        if (!isManagedPath(relativePath)) {
            return null;
        }

        String fileName = relativePath.substring(RELATIVE_DIRECTORY.length() + 1);
        Path directory = storageDirectory();
        Path externalFile = directory.resolve(fileName).normalize();
        if (!externalFile.startsWith(directory)) {
            return null;
        }
        Path storedFile = resolveExistingFile(externalFile, directory);
        if (storedFile != null) {
            return storedFile;
        }
        return resolveLegacyFile(relativePath, servletContext);
    }

    public static void delete(String relativePath, ServletContext servletContext) throws IOException {
        if (!isManagedPath(relativePath)) {
            return;
        }

        String fileName = relativePath.substring(RELATIVE_DIRECTORY.length() + 1);
        Path directory = storageDirectory();
        Path externalFile = resolveExistingFile(directory.resolve(fileName).normalize(), directory);
        if (externalFile != null) {
            Files.deleteIfExists(externalFile);
            return;
        }

        Path legacyFile = resolveLegacyFile(relativePath, servletContext);
        if (legacyFile != null) {
            Files.deleteIfExists(legacyFile);
        }
    }

    public static boolean isManagedPath(String relativePath) {
        if (relativePath == null || !relativePath.startsWith(RELATIVE_DIRECTORY + "/")) {
            return false;
        }
        String fileName = relativePath.substring(RELATIVE_DIRECTORY.length() + 1);
        if (!fileName.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,199}")) {
            return false;
        }
        String lowerCaseName = fileName.toLowerCase(Locale.ROOT);
        return lowerCaseName.endsWith(".png")
                || lowerCaseName.endsWith(".jpg")
                || lowerCaseName.endsWith(".jpeg");
    }

    public static String contentType(String relativePath) {
        String lowerCasePath = relativePath.toLowerCase(Locale.ROOT);
        return lowerCasePath.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        try (ImageInputStream imageInput =
                     ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (imageInput == null) {
                throw new InvalidImageException("Le fichier ne peut pas être lu comme une image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new InvalidImageException("Format d'image non pris en charge.");
            }

            ImageReader reader = readers.next();
            try {
                try {
                    reader.setInput(imageInput, true, true);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) {
                        throw new InvalidImageException("Seules les images PNG et JPEG sont acceptées.");
                    }
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                        throw new InvalidImageException(
                                "Les dimensions de l'image dépassent la limite autorisée.");
                    }
                    BufferedImage image = reader.read(0);
                    if (image == null) {
                        throw new InvalidImageException("L'image fournie est invalide.");
                    }
                    return image;
                } catch (InvalidImageException e) {
                    throw e;
                } catch (IOException e) {
                    throw new InvalidImageException("L'image fournie est invalide.", e);
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private static Path storageDirectory() {
        String configuredDirectory = LocalConfiguration.get(STORAGE_DIRECTORY);
        Path directory;
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            try {
                directory = Path.of(configuredDirectory);
            } catch (InvalidPathException e) {
                throw new StorageConfigurationException(
                        STORAGE_DIRECTORY + " doit être un chemin absolu valide.", e);
            }
            if (!directory.isAbsolute()) {
                throw new StorageConfigurationException(
                        STORAGE_DIRECTORY + " doit désigner un chemin absolu.");
            }
        } else {
            String catalinaBase = System.getProperty("catalina.base");
            if (catalinaBase == null || catalinaBase.isBlank()) {
                throw new StorageConfigurationException(
                        STORAGE_DIRECTORY + " ou la propriété catalina.base doit être configurée.");
            }
            directory = Path.of(catalinaBase).resolve("cynemma-images");
        }
        return directory.normalize();
    }

    private static Path resolveExistingFile(Path file, Path directory) throws IOException {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        Path realDirectory = directory.toRealPath();
        Path realFile = file.toRealPath();
        return realFile.startsWith(realDirectory) ? realFile : null;
    }

    private static Path resolveLegacyFile(String relativePath, ServletContext servletContext)
            throws IOException {
        if (servletContext == null) {
            return null;
        }
        String webRootPath = servletContext.getRealPath("/");
        if (webRootPath == null) {
            return null;
        }

        Path webRoot = Path.of(webRootPath).toRealPath();
        Path legacyDirectory = webRoot.resolve(RELATIVE_DIRECTORY);
        if (!Files.isDirectory(legacyDirectory)) {
            return null;
        }
        Path realLegacyDirectory = legacyDirectory.toRealPath();
        if (!realLegacyDirectory.startsWith(webRoot)) {
            return null;
        }
        Path legacyFile = webRoot.resolve(relativePath).normalize();
        if (!legacyFile.startsWith(realLegacyDirectory)) {
            return null;
        }
        Path realLegacyFile = resolveExistingFile(legacyFile, realLegacyDirectory);
        return realLegacyFile != null && realLegacyFile.startsWith(realLegacyDirectory)
                ? realLegacyFile
                : null;
    }

    public static final class InvalidImageException extends IOException {
        private static final long serialVersionUID = 1L;

        private InvalidImageException(String message) {
            super(message);
        }

        private InvalidImageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class StorageConfigurationException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        private StorageConfigurationException(String message) {
            super(message);
        }

        private StorageConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

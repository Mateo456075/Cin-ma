package fr.turgot.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class LocalConfiguration {
    private static final String CONFIG_FILE_PROPERTY = "cynemma.config.file";
    private static final String CONFIG_FILE_NAME = "cynemma.env";

    private LocalConfiguration() {
    }

    public static String get(String name) {
        Objects.requireNonNull(name, "name");

        String value = configuredValue(System.getenv(name));
        if (value != null) {
            return value;
        }

        value = configuredValue(System.getProperty(name));
        if (value != null) {
            return value;
        }

        return readLocalConfiguration().get(name);
    }

    private static Map<String, String> readLocalConfiguration() {
        Path configFile = findConfigFile();
        if (configFile == null) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        try {
            int lineNumber = 0;
            for (String line : Files.readAllLines(configFile, StandardCharsets.UTF_8)) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("export ")) {
                    trimmed = trimmed.substring("export ".length()).trim();
                }

                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    throw new IllegalStateException(
                            "Format invalide dans " + configFile + " à la ligne " + lineNumber + ".");
                }
                String key = trimmed.substring(0, separator).trim();
                String value = unquote(trimmed.substring(separator + 1).trim());
                if (key.isEmpty()) {
                    throw new IllegalStateException(
                            "Nom de variable vide dans " + configFile + " à la ligne " + lineNumber + ".");
                }
                values.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossible de lire le fichier de configuration local " + configFile + ".", e);
        }
        return values;
    }

    private static Path findConfigFile() {
        String configuredPath = configuredValue(System.getProperty(CONFIG_FILE_PROPERTY));
        if (configuredPath != null) {
            Path path = Path.of(configuredPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException(
                        "Le fichier indiqué par -D" + CONFIG_FILE_PROPERTY + " n'existe pas : " + path);
            }
            return path;
        }

        Set<Path> candidates = new LinkedHashSet<>();
        addTomcatConfigFile(candidates, System.getProperty("catalina.base"));
        addTomcatConfigFile(candidates, System.getProperty("catalina.home"));
        addProjectConfigFiles(candidates, System.getProperty("user.dir"));
        addProjectConfigFiles(candidates, System.getProperty("catalina.base"));
        addProjectConfigFiles(candidates, System.getProperty("catalina.home"));
        addProjectConfigFiles(candidates, System.getProperty("user.home"));

        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
    }

    private static void addTomcatConfigFile(Set<Path> candidates, String directory) {
        if (directory != null && !directory.isBlank()) {
            candidates.add(Path.of(directory).resolve("conf").resolve(CONFIG_FILE_NAME)
                    .toAbsolutePath().normalize());
        }
    }

    private static void addProjectConfigFiles(Set<Path> candidates, String directory) {
        if (directory == null || directory.isBlank()) {
            return;
        }
        Path current = Path.of(directory).toAbsolutePath().normalize();
        while (current != null) {
            candidates.add(current.resolve(".local-secrets").resolve(CONFIG_FILE_NAME));
            current = current.getParent();
        }
    }

    private static String configuredValue(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}

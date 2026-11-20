package fr.turgot.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalConfigurationTest {
    private static final String CONFIG_PROPERTY = "cynemma.config.file";
    private static final String TEST_KEY = "CYNEMMA_LOCAL_CONFIGURATION_TEST";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void readsValueFromLocalEnvFileWhenEnvironmentAndPropertyAreAbsent() throws Exception {
        File configFile = temporaryFolder.newFile("cynemma.env");
        Files.writeString(configFile.toPath(), TEST_KEY + "='from-file'\n", StandardCharsets.UTF_8);
        String previousConfigPath = System.getProperty(CONFIG_PROPERTY);
        String previousValue = System.getProperty(TEST_KEY);

        try {
            System.clearProperty(TEST_KEY);
            System.setProperty(CONFIG_PROPERTY, configFile.getAbsolutePath());

            assertEquals("from-file", LocalConfiguration.get(TEST_KEY));
        } finally {
            restoreProperty(CONFIG_PROPERTY, previousConfigPath);
            restoreProperty(TEST_KEY, previousValue);
        }
    }

    @Test
    public void systemPropertyOverridesLocalEnvFile() throws Exception {
        File configFile = temporaryFolder.newFile("cynemma.env");
        Files.writeString(configFile.toPath(), TEST_KEY + "=from-file\n", StandardCharsets.UTF_8);
        String previousConfigPath = System.getProperty(CONFIG_PROPERTY);
        String previousValue = System.getProperty(TEST_KEY);

        try {
            System.setProperty(CONFIG_PROPERTY, configFile.getAbsolutePath());
            System.setProperty(TEST_KEY, "from-property");

            assertEquals("from-property", LocalConfiguration.get(TEST_KEY));
        } finally {
            restoreProperty(CONFIG_PROPERTY, previousConfigPath);
            restoreProperty(TEST_KEY, previousValue);
        }
    }

    @Test
    public void findsConfigurationInTomcatBaseConfDirectory() throws Exception {
        File catalinaBase = temporaryFolder.newFolder("tomcat");
        File confDirectory = new File(catalinaBase, "conf");
        Files.createDirectories(confDirectory.toPath());
        File configFile = new File(confDirectory, "cynemma.env");
        Files.writeString(configFile.toPath(), TEST_KEY + "=from-tomcat-conf\n", StandardCharsets.UTF_8);
        String previousConfigPath = System.getProperty(CONFIG_PROPERTY);
        String previousCatalinaBase = System.getProperty("catalina.base");
        String previousValue = System.getProperty(TEST_KEY);

        try {
            System.clearProperty(CONFIG_PROPERTY);
            System.clearProperty(TEST_KEY);
            System.setProperty("catalina.base", catalinaBase.getAbsolutePath());

            assertEquals("from-tomcat-conf", LocalConfiguration.get(TEST_KEY));
        } finally {
            restoreProperty(CONFIG_PROPERTY, previousConfigPath);
            restoreProperty("catalina.base", previousCatalinaBase);
            restoreProperty(TEST_KEY, previousValue);
        }
    }

    @Test
    public void findsConfigurationInTomcatHomeConfDirectory() throws Exception {
        File catalinaHome = temporaryFolder.newFolder("tomcat-home");
        File confDirectory = new File(catalinaHome, "conf");
        Files.createDirectories(confDirectory.toPath());
        File configFile = new File(confDirectory, "cynemma.env");
        Files.writeString(configFile.toPath(), TEST_KEY + "=from-tomcat-home\n", StandardCharsets.UTF_8);
        File catalinaBase = temporaryFolder.newFolder("tomcat-base");
        String previousConfigPath = System.getProperty(CONFIG_PROPERTY);
        String previousCatalinaBase = System.getProperty("catalina.base");
        String previousCatalinaHome = System.getProperty("catalina.home");
        String previousValue = System.getProperty(TEST_KEY);

        try {
            System.clearProperty(CONFIG_PROPERTY);
            System.clearProperty(TEST_KEY);
            System.setProperty("catalina.base", catalinaBase.getAbsolutePath());
            System.setProperty("catalina.home", catalinaHome.getAbsolutePath());

            assertEquals("from-tomcat-home", LocalConfiguration.get(TEST_KEY));
        } finally {
            restoreProperty(CONFIG_PROPERTY, previousConfigPath);
            restoreProperty("catalina.base", previousCatalinaBase);
            restoreProperty("catalina.home", previousCatalinaHome);
            restoreProperty(TEST_KEY, previousValue);
        }
    }

    private static void restoreProperty(String name, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previousValue);
        }
    }
}

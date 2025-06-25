package com.musimizer.util;

import com.musimizer.exception.MusicDirectoryException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsManager {
    private static final Logger LOGGER = Logger.getLogger(SettingsManager.class.getName());

    static final String MUSIC_DIR_KEY = "musicDir";
    static final String NUM_PICKS_KEY = "numberOfPicks";
    static final String NUM_SEARCH_RESULTS_KEY = "numberOfSearchResults";

    private static final int DEFAULT_NUM_PICKS = 25;
    private static final int DEFAULT_NUM_SEARCH_RESULTS = 50;

    private static final String SETTINGS_FILE_NAME = "musimizer_settings.properties";
    private static final Path SETTINGS_FILE_PATH;
    private static final Properties properties = new Properties();

    static {
        // Determine the settings file path based on OS
        String appDataDir;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            appDataDir = System.getenv("APPDATA");
        } else {
            // For Linux/macOS, use user.home/.config or similar
            appDataDir = System.getProperty("user.home") + "/.config";
        }
        SETTINGS_FILE_PATH = Paths.get(appDataDir, "Musimizer", SETTINGS_FILE_NAME);
        loadSettings();
    }

    private SettingsManager() {
        // Private constructor to prevent instantiation
    }

    private static void loadSettings() {
        if (Files.exists(SETTINGS_FILE_PATH)) {
            try (InputStream input = Files.newInputStream(SETTINGS_FILE_PATH)) {
                properties.load(input);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load settings from " + SETTINGS_FILE_PATH, e);
            }
        }
        // Set default values if not present
        properties.putIfAbsent(MUSIC_DIR_KEY, "");
        properties.putIfAbsent(NUM_PICKS_KEY, String.valueOf(DEFAULT_NUM_PICKS));
        properties.putIfAbsent(NUM_SEARCH_RESULTS_KEY, String.valueOf(DEFAULT_NUM_SEARCH_RESULTS));
    }

    private static void saveSettings() {
        try {
            Files.createDirectories(SETTINGS_FILE_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(SETTINGS_FILE_PATH)) {
                properties.store(output, null);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save settings to " + SETTINGS_FILE_PATH, e);
        }
    }

    public static String getMusicDir() {
        return properties.getProperty(MUSIC_DIR_KEY, "");
    }

    public static void setMusicDir(String musicDir) {
        properties.setProperty(MUSIC_DIR_KEY, musicDir != null ? musicDir : "");
        saveSettings();
    }

    public static int getNumberOfPicks() {
        return Integer.parseInt(properties.getProperty(NUM_PICKS_KEY, String.valueOf(DEFAULT_NUM_PICKS)));
    }

    public static void setNumberOfPicks(int number) {
        int validNumber = Math.max(1, number); // Ensure at least 1
        properties.setProperty(NUM_PICKS_KEY, String.valueOf(validNumber));
        saveSettings();
    }

    public static int getNumberOfSearchResults() {
        return Integer.parseInt(properties.getProperty(NUM_SEARCH_RESULTS_KEY, String.valueOf(DEFAULT_NUM_SEARCH_RESULTS)));
    }

    public static void setNumberOfSearchResults(int number) {
        int validNumber = Math.max(1, number); // Ensure at least 1
        properties.setProperty(NUM_SEARCH_RESULTS_KEY, String.valueOf(validNumber));
        saveSettings();
    }

    public static Path getExclusionFilePath() {
        return getAppDataPath().resolve("excluded_albums.txt");
    }

    public static void ensureExclusionFileExists() throws IOException {
        Path exclusionFile = getExclusionFilePath();
        if (exclusionFile != null && !Files.exists(exclusionFile)) {
            Files.createDirectories(exclusionFile.getParent());
            Files.createFile(exclusionFile);
        }
    }

    public static Path getAppDataPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        
        try {
            Path appDataPath;
            if (os.contains("win")) {
                String appData = System.getenv("APPDATA");
                appDataPath = Paths.get(appData != null ? appData : userHome, "musimizer");
            } else if (os.contains("mac")) {
                appDataPath = Paths.get(userHome, "Library", "Application Support", "musimizer");
            } else {
                appDataPath = Paths.get(userHome, ".config", "musimizer");
            }
            
            // Ensure the directory exists
            Files.createDirectories(appDataPath);
            return appDataPath;
            
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to get application data directory", e);
        }
    }
}

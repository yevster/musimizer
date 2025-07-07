package com.musimizer.util;

import com.musimizer.exception.MusicDirectoryException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SettingsManager {
    private static final Logger LOGGER = Logger.getLogger(SettingsManager.class.getName());

    static final String MUSIC_DIR_KEY = "musicDir";
    static final String NUM_PICKS_KEY = "numberOfPicks";
    static final String NUM_SEARCH_RESULTS_KEY = "numberOfSearchResults";
    static final String APPLY_EXCLUSIONS_TO_SEARCH_KEY = "applyExclusionsToSearch";

    private static final int DEFAULT_NUM_PICKS = 25;
    private static final int DEFAULT_NUM_SEARCH_RESULTS = 50;
    private static final boolean DEFAULT_APPLY_EXCLUSIONS_TO_SEARCH = true;

    private static final String SETTINGS_FILE_NAME = "musimizer_settings.properties";
    private static final String BOOKMARKS_FILE_NAME = "bookmarks.txt";
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
        properties.putIfAbsent(APPLY_EXCLUSIONS_TO_SEARCH_KEY, String.valueOf(DEFAULT_APPLY_EXCLUSIONS_TO_SEARCH));
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
    
    public static boolean isApplyExclusionsToSearch() {
        return Boolean.parseBoolean(properties.getProperty(APPLY_EXCLUSIONS_TO_SEARCH_KEY, 
                String.valueOf(DEFAULT_APPLY_EXCLUSIONS_TO_SEARCH)));
    }
    
    public static void setApplyExclusionsToSearch(boolean apply) {
        properties.setProperty(APPLY_EXCLUSIONS_TO_SEARCH_KEY, String.valueOf(apply));
        saveSettings();
    }

    public static Path getExclusionFilePath() {
        return getAppDataPath().resolve("excluded_albums.txt");
    }
    
    public static Path getBookmarksFilePath() {
        return getAppDataPath().resolve(BOOKMARKS_FILE_NAME);
    }
    
    /**
     * Toggles the bookmark status of an album path.
     * @param albumPath the path to toggle bookmark for
     * @return true if the album was bookmarked, false if it was unbookmarked
     * @throws IOException if there's an error accessing the bookmarks file
     */
    public static boolean toggleBookmark(Path albumPath) throws IOException {
        Path bookmarksFile = getBookmarksFilePath();
        Files.createDirectories(bookmarksFile.getParent());
        
        // Read existing bookmarks
        List<String> bookmarks = new ArrayList<>();
        if (Files.exists(bookmarksFile)) {
            bookmarks = new ArrayList<>(Files.readAllLines(bookmarksFile, StandardCharsets.UTF_8));
        }
        
        String path = albumPath.toString();
        boolean wasRemoved = bookmarks.remove(path);
        
        if (!wasRemoved) {
            // If it wasn't in the list, add it
            bookmarks.add(path);
        }
        
        // Write the updated list back to the file
        Files.write(bookmarksFile, bookmarks, StandardCharsets.UTF_8);
        return !wasRemoved; // Return true if added, false if removed
    }
    
    /**
     * Checks if an album path is bookmarked.
     * @param albumPath the path to check
     * @return true if the path is bookmarked, false otherwise
     * @throws IOException if there's an error reading the bookmarks file
     */
    public static boolean isBookmarked(Path albumPath) throws IOException {
        Path bookmarksFile = getBookmarksFilePath();
        if (!Files.exists(bookmarksFile)) {
            return false;
        }
        String path = albumPath.toString();
        return Files.lines(bookmarksFile).anyMatch(path::equals);
    }
    
    public static List<Path> getBookmarks() throws IOException {
        Path bookmarksFile = getBookmarksFilePath();
        if (!Files.exists(bookmarksFile)) {
            return Collections.emptyList();
        }
        
        return Files.readAllLines(bookmarksFile, StandardCharsets.UTF_8).stream()
                .map(Paths::get)
                .collect(Collectors.toList());
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

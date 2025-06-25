package com.musimizer.util;

import com.musimizer.exception.MusicDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class SettingsManager {
    static final String MUSIC_DIR_KEY = "musicDir";
    static final String NUM_PICKS_KEY = "numberOfPicks";
    static final String NUM_SEARCH_RESULTS_KEY = "numberOfSearchResults";
    private static final int DEFAULT_NUM_PICKS = 25;
    private static final int DEFAULT_NUM_SEARCH_RESULTS = 50;
    private static final Preferences prefs = Preferences.userNodeForPackage(SettingsManager.class);

    private SettingsManager() {
        // Private constructor to prevent instantiation
    }

    public static String getMusicDir() {
        return prefs.get(MUSIC_DIR_KEY, "");
    }

    public static void setMusicDir(String musicDir) {
        prefs.put(MUSIC_DIR_KEY, musicDir != null ? musicDir : "");
    }

    public static int getNumberOfPicks() {
        return prefs.getInt(NUM_PICKS_KEY, DEFAULT_NUM_PICKS);
    }

    public static void setNumberOfPicks(int number) {
        int validNumber = Math.max(1, number); // Ensure at least 1
        prefs.putInt(NUM_PICKS_KEY, validNumber);
    }

    public static int getNumberOfSearchResults() {
        return prefs.getInt(NUM_SEARCH_RESULTS_KEY, DEFAULT_NUM_SEARCH_RESULTS);
    }

    public static void setNumberOfSearchResults(int number) {
        int validNumber = Math.max(1, number); // Ensure at least 1
        prefs.putInt(NUM_SEARCH_RESULTS_KEY, validNumber);
    }

    public static Path getExclusionFilePath() {
        return getAppDataPath().resolve("excluded_albums.txt");
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
            FileSystemManager.createDirectories(appDataPath);
            return appDataPath;
            
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to get application data directory", e);
        }
    }
}

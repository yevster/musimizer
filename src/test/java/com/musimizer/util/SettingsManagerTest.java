package com.musimizer.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingsManagerTest {
    
    private static final String TEST_MUSIC_DIR = "/test/music/dir";
    private static final int TEST_NUM_PICKS = 10;
    private static final int TEST_NUM_SEARCH_RESULTS = 20;
    
    private String originalMusicDir;
    private int originalNumPicks;
    private int originalNumSearchResults;
    
    @BeforeEach
    void setUp() {
        // Save original values
        originalMusicDir = SettingsManager.getMusicDir();
        originalNumPicks = SettingsManager.getNumberOfPicks();
        originalNumSearchResults = SettingsManager.getNumberOfSearchResults();
        
        // Clear test preferences
        clearTestPreferences();
    }
    
    @AfterEach
    void tearDown() {
        // Restore original values
        if (originalMusicDir != null) {
            SettingsManager.setMusicDir(originalMusicDir);
        }
        SettingsManager.setNumberOfPicks(originalNumPicks);
        SettingsManager.setNumberOfSearchResults(originalNumSearchResults);
    }
    
    private void clearTestPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(SettingsManager.class);
        prefs.remove(SettingsManager.MUSIC_DIR_KEY);
        prefs.remove(SettingsManager.NUM_PICKS_KEY);
        prefs.remove(SettingsManager.NUM_SEARCH_RESULTS_KEY);
    }
    
    @Test
    void testSetAndGetMusicDir() {
        // Test setting and getting music directory
        SettingsManager.setMusicDir(TEST_MUSIC_DIR);
        assertEquals(TEST_MUSIC_DIR, SettingsManager.getMusicDir());
        
        // Test with null
        SettingsManager.setMusicDir(null);
        assertEquals("", SettingsManager.getMusicDir());
    }
    
    @Test
    void testSetAndGetNumberOfPicks() {
        // Test setting and getting number of picks
        SettingsManager.setNumberOfPicks(TEST_NUM_PICKS);
        assertEquals(TEST_NUM_PICKS, SettingsManager.getNumberOfPicks());
        
        // Test with invalid values
        SettingsManager.setNumberOfPicks(-1);
        assertEquals(1, SettingsManager.getNumberOfPicks(), "Number of picks should be corrected to 1");
    }
    
    @Test
    void testSetAndGetNumberOfSearchResults() {
        // Test setting and getting number of search results
        SettingsManager.setNumberOfSearchResults(TEST_NUM_SEARCH_RESULTS);
        assertEquals(TEST_NUM_SEARCH_RESULTS, SettingsManager.getNumberOfSearchResults());
        
        // Test with invalid values
        SettingsManager.setNumberOfSearchResults(0);
        assertEquals(1, SettingsManager.getNumberOfSearchResults(), "Number of search results should be corrected to 1");
    }
    
    @Test
    void testGetExclusionFilePath() {
        // Test getting exclusion file path
        Path exclusionPath = SettingsManager.getExclusionFilePath();
        assertNotNull(exclusionPath);
        assertTrue(exclusionPath.toString().endsWith("excluded_albums.txt"));
    }
    
    @Test
    void testGetAppDataPath() {
        // Test getting app data path
        Path appDataPath = SettingsManager.getAppDataPath();
        assertNotNull(appDataPath);
        assertTrue(Files.isDirectory(appDataPath));
    }
}

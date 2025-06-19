package com.musimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlbumPickerTest {
    @TempDir
    Path tempDir;
    
    private Path musicDir;
    private Path exclusionFile;
    private AlbumPicker albumPicker;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create a test music directory structure
        musicDir = tempDir.resolve("music");
        Files.createDirectories(musicDir);
        
        // Create some test artist/album directories
        createAlbum("Artist1", "Album1");
        createAlbum("Artist1", "Album2");
        createAlbum("Artist2", "Album1");
        createAlbum("Artist2", "Album3");
        
        // Set up exclusion file
        exclusionFile = tempDir.resolve("excluded_albums.txt");
        
        // Initialize AlbumPicker
        albumPicker = new AlbumPicker(musicDir, exclusionFile);
    }
    
    private void createAlbum(String artist, String album) throws IOException {
        Path albumPath = musicDir.resolve(artist).resolve(album);
        Files.createDirectories(albumPath);
    }
    
    @Test
    void testInitialPicks() {
        // When
        albumPicker.generateNewPicks(2);
        List<String> picks = albumPicker.getCurrentPicks();
        
        // Then
        assertEquals(2, picks.size());
        assertFalse(picks.get(0).equals(picks.get(1)));
    }
    
    @Test
    void testExcludeAlbum() {
        // Given
        String albumToExclude = "Artist1 - Album1";
        
        // When
        albumPicker.excludeAlbum(albumToExclude);
        albumPicker.generateNewPicks(10); // More than total albums to test exclusion
        List<String> picks = albumPicker.getCurrentPicks();
        
        // Then
        assertFalse(picks.contains(albumToExclude));
        assertTrue(albumPicker.getExcludedAlbums().contains(albumToExclude));
    }
    
    @Test
    void testSaveAndLoadPicks() {
        // Given
        albumPicker.generateNewPicks(2);
        List<String> originalPicks = albumPicker.getCurrentPicks();
        
        // When - create a new AlbumPicker which should load the saved picks
        AlbumPicker newPicker = new AlbumPicker(musicDir, exclusionFile);
        newPicker.loadSavedPicks();
        List<String> loadedPicks = newPicker.getCurrentPicks();
        
        // Then
        assertEquals(originalPicks, loadedPicks);
    }
    
    @Test
    void testPickMoreAlbumsThanAvailable() {
        // When - try to pick more albums than available (only 4 total)
        albumPicker.generateNewPicks(10);
        List<String> picks = albumPicker.getCurrentPicks();
        
        // Then - should return all available albums (4)
        assertEquals(4, picks.size());
    }
    
    @Test
    void testLoadNonExistentPicksFile() {
        // Given - exclusion file doesn't exist yet
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");
        AlbumPicker picker = new AlbumPicker(musicDir, nonExistentFile);
        
        // When
        picker.loadSavedPicks();
        
        // Then - should not throw exception and have empty picks
        assertTrue(picker.getCurrentPicks().isEmpty());
    }
    
    @Test
    void testExclusionPersistence() {
        // Given
        String albumToExclude = "Artist2 - Album3";
        albumPicker.excludeAlbum(albumToExclude);
        
        // When - create a new AlbumPicker which should load the exclusions
        AlbumPicker newPicker = new AlbumPicker(musicDir, exclusionFile);
        newPicker.generateNewPicks(10);
        
        // Then
        assertFalse(newPicker.getCurrentPicks().contains(albumToExclude));
        assertTrue(newPicker.getExcludedAlbums().contains(albumToExclude));
    }
}

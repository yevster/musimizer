package com.musimizer.repository;

import com.musimizer.exception.MusicDirectoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;


import static org.junit.jupiter.api.Assertions.*;

class FileAlbumRepositoryTest {

    @TempDir
    Path tempDir;

    private Path musicDir;
    private FileAlbumRepository repository;
    private Path exclusionFile;
    private Path savedPicksFile;

    private Path album1;
    private Path album2;
    private Path album3;

    @BeforeEach
    void setUp() throws IOException {
        repository = new FileAlbumRepository();
        
        musicDir = tempDir.resolve("music");
        Files.createDirectories(musicDir);
        
        Path artist1 = musicDir.resolve("Artist With Spaces");
        Path artist2 = musicDir.resolve("Special-Chars_ Test");
        
        album1 = artist1.resolve("Album 1 (2020)");
        album2 = artist1.resolve("Album no. 2 [Special Edition]");
        album3 = artist2.resolve("Album_ The Third!");
        
        Files.createDirectories(album1);
        Files.createDirectories(album2);
        Files.createDirectories(album3);

        exclusionFile = tempDir.resolve("exclusions.txt");
        savedPicksFile = tempDir.resolve("picks.txt");
    }

    @Test
    void testFindAllAlbums() throws IOException {
        List<Path> albums = repository.findAllAlbums(musicDir);
        
        assertEquals(3, albums.size());
        assertTrue(albums.contains(album1.toAbsolutePath()));
        assertTrue(albums.contains(album2.toAbsolutePath()));
        assertTrue(albums.contains(album3.toAbsolutePath()));
    }

    @Test
    void testSaveAndLoadExcludedAlbums() throws IOException {
        Set<Path> excludedAlbums = Set.of(album1, album3);
        
        repository.saveExcludedAlbums(musicDir, exclusionFile, excludedAlbums);
        
        Set<Path> loadedExclusions = repository.loadExcludedAlbums(musicDir, exclusionFile);
        
        assertEquals(2, loadedExclusions.size());
        assertTrue(loadedExclusions.contains(album1));
        assertTrue(loadedExclusions.contains(album3));
    }

    @Test
    void testSaveAndLoadAlbumPicks() throws IOException {
        List<Path> albumPicks = List.of(album1, album2);
        
        repository.saveAlbumPicks(savedPicksFile, albumPicks);
        
        List<Path> loadedPicks = repository.loadAlbumPicks(savedPicksFile);
        
        assertEquals(2, loadedPicks.size());
        assertTrue(loadedPicks.contains(album1));
        assertTrue(loadedPicks.contains(album2));
    }

    @Test
    void testLoadExcludedAlbums_WhenFileDoesNotExist() {
        Set<Path> result = repository.loadExcludedAlbums(musicDir, exclusionFile);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadAlbumPicks_WhenFileDoesNotExist() {
        List<Path> result = repository.loadAlbumPicks(savedPicksFile);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAllAlbums_WhenDirectoryDoesNotExist() {
        Path nonExistentDir = tempDir.resolve("nonexistent");
        assertThrows(MusicDirectoryException.class, () -> {
            repository.findAllAlbums(nonExistentDir);
        });
    }

}

package com.musimizer.service;

import com.musimizer.exception.MusicDirectoryException;
import com.musimizer.repository.AlbumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @TempDir
    private Path tempDir;
    
    @Mock
    private AlbumRepository albumRepository;
    
    private AlbumService albumService;
    private Path musicDir;
    private Path exclusionFile;
    private Path savedPicksFile;
    
    private final List<String> sampleAlbums = List.of(
        "Artist1 - Album1",
        "Artist1 - Album2",
        "Artist2 - Album1",
        "Artist2 - Album3"
    );

    @BeforeEach
    void setUp() {
        musicDir = tempDir.resolve("music");
        exclusionFile = tempDir.resolve("excluded_albums.txt");
        savedPicksFile = tempDir.resolve("saved_picks.txt");
        albumService = new AlbumService(albumRepository, musicDir, exclusionFile);
    }

    @Test
    void constructor_shouldInitializeWithNoExcludedAlbums() throws Exception {
        // No need to set up mocks since we're just testing the default state
        assertTrue(albumService.getExcludedAlbums().isEmpty());
    }

    @Test
    void generateNewPicks_shouldReturnRequestedNumberOfPicks() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        
        // When
        albumService.generateNewPicks(2);
        List<String> picks = albumService.getCurrentPicks();
        
        // Then
        assertEquals(2, picks.size());
        assertTrue(sampleAlbums.containsAll(picks));
        verify(albumRepository).saveAlbumPicks(savedPicksFile, picks);
    }

    @Test
    void generateNewPicks_shouldNotReturnExcludedAlbums() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        
        String excludedAlbum = sampleAlbums.get(0);
        albumService.excludeAlbum(excludedAlbum);
        
        // When
        albumService.generateNewPicks(sampleAlbums.size());
        List<String> picks = albumService.getCurrentPicks();
        
        // Then
        assertFalse(picks.contains(excludedAlbum));
        assertEquals(sampleAlbums.size() - 1, picks.size());
    }

    @Test
    void generateNewPicks_shouldThrowWhenNoAlbumsAvailable() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(Collections.emptyList());
        
        // When/Then
        assertThrows(IllegalStateException.class, () -> albumService.generateNewPicks(1));
    }

    @Test
    void excludeAlbum_shouldAddToExcludedAlbums() throws Exception {
        // Given
        String albumToExclude = sampleAlbums.get(0);
        
        // When
        albumService.excludeAlbum(albumToExclude);
        
        // Then
        assertTrue(albumService.getExcludedAlbums().contains(albumToExclude));
        verify(albumRepository).saveExcludedAlbums(exclusionFile, Set.of(albumToExclude));
    }

    @Test
    void excludeAlbum_shouldRemoveFromCurrentPicks() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        albumService.generateNewPicks(sampleAlbums.size());
        String albumToExclude = sampleAlbums.get(0);
        
        // When
        albumService.excludeAlbum(albumToExclude);
        
        // Then
        assertFalse(albumService.getCurrentPicks().contains(albumToExclude));
    }

    @Test
    void searchAlbums_shouldReturnMatchingAlbums() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        
        // When
        List<String> results = albumService.searchAlbums(List.of("Artist1"), 10);
        
        // Then
        assertEquals(2, results.size());
        assertTrue(results.contains("Artist1 - Album1"));
        assertTrue(results.contains("Artist1 - Album2"));
    }

    @Test
    void searchAlbums_shouldRespectMaxResults() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        
        // When
        List<String> results = albumService.searchAlbums(List.of("Album"), 2);
        
        // Then
        assertEquals(2, results.size());
    }

    @Test
    void searchAlbums_shouldReturnEmptyListForEmptySearchTerms() throws Exception {
        // When
        List<String> results = albumService.searchAlbums(Collections.emptyList(), 10);
        
        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    void loadSavedPicks_shouldLoadPicksFromRepository() throws Exception {
        // Given
        List<String> savedPicks = List.of("Artist1 - Album1", "Artist2 - Album3");
        when(albumRepository.loadAlbumPicks(savedPicksFile, exclusionFile)).thenReturn(savedPicks);
        
        // When
        albumService.loadSavedPicks();
        
        // Then
        assertEquals(savedPicks, albumService.getCurrentPicks());
    }

    @Test
    void loadExcludedAlbums_shouldReloadExcludedAlbums() throws Exception {
        // Given
        Set<String> excluded = Set.of("Artist1 - Album1");
        when(albumRepository.loadExcludedAlbums(exclusionFile)).thenReturn(excluded);
        
        // When
        albumService.loadExcludedAlbums();
        
        // Then
        assertEquals(excluded, albumService.getExcludedAlbums());
    }

    @Test
    void findAllAlbums_shouldRethrowAsMusicDirectoryException() throws Exception {
        // Given
        Exception cause = new Exception("Test exception");
        when(albumRepository.findAllAlbums(musicDir)).thenThrow(cause);
        
        // When/Then
        MusicDirectoryException exception = assertThrows(MusicDirectoryException.class, 
            () -> albumService.searchAlbums(List.of("test"), 10));
        assertEquals(cause, exception.getCause());
    }
}

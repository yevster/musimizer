package com.musimizer.service;

import com.musimizer.exception.MusicDirectoryException;
import com.musimizer.repository.AlbumRepository;
import com.musimizer.settings.ApplicationSettings;
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
    private ApplicationSettings settings;
    private Path musicDir;
    private Path exclusionFile;
    private Path savedPicksFile;
    
    private List<Path> sampleAlbums;

    @BeforeEach
    void setUp() {
        musicDir = tempDir.resolve("music");
        exclusionFile = tempDir.resolve("excluded_albums.txt");
        savedPicksFile = tempDir.resolve("saved_picks.txt");
        settings = mock(ApplicationSettings.class);
        albumService = new AlbumService(albumRepository, musicDir, exclusionFile, settings);
        sampleAlbums = List.of(
            Path.of(musicDir.toAbsolutePath().toString(), "Artist1", "Album1"),
            Path.of(musicDir.toAbsolutePath().toString(), "Artist1", "Album2"),
            Path.of(musicDir.toAbsolutePath().toString(), "Ar-tist2", "Album2"),
            Path.of(musicDir.toAbsolutePath().toString(), "Artist3", "Al_ bum3"),
            Path.of(musicDir.toAbsolutePath().toString(), "A rti st4", "Al bum4")
        );
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
        List<Path> picks = albumService.getCurrentPicks();
        
        // Then
        assertEquals(2, picks.size());
        assertTrue(sampleAlbums.containsAll(picks));
        verify(albumRepository).saveAlbumPicks(savedPicksFile, picks);
    }

    @Test
    void generateNewPicks_shouldNotReturnExcludedAlbums() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        
        Path excludedAlbum = sampleAlbums.get(0);
        albumService.excludeAlbum(excludedAlbum);
        
        // When
        albumService.generateNewPicks(sampleAlbums.size());
        List<Path> picks = albumService.getCurrentPicks();
        
        // Then
        assertFalse(picks.contains(excludedAlbum));
        assertEquals(sampleAlbums.size() - 1, picks.size());
    }

    @Test
    void generateNewPicks_shouldNotReturnExcludedAlbumsAfterReloading() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));

        Path excludedAlbum = sampleAlbums.get(0);
        Set<Path> capturedExclusions = new HashSet<>();

        // Mock the behavior of saving and loading excluded albums to simulate file I/O
        doAnswer(invocation -> {
            // Capture the collection of paths passed to saveExcludedAlbums
            Collection<Path> albumsToExclude = invocation.getArgument(2);
            capturedExclusions.addAll(albumsToExclude);
            return null;
        }).when(albumRepository).saveExcludedAlbums(any(), any(), any());

        when(albumRepository.loadExcludedAlbums(any(), any())).thenReturn(capturedExclusions);

        // When
        albumService.excludeAlbum(excludedAlbum); // This will now "save" the exclusion to our captured set
        albumService.loadExcludedAlbums(); // This will "load" from our captured set

        albumService.generateNewPicks(sampleAlbums.size());
        List<Path> picks = albumService.getCurrentPicks();

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
        Path albumToExclude = sampleAlbums.get(0);
        
        // When
        albumService.excludeAlbum(albumToExclude);
        
        // Then
        assertTrue(albumService.getExcludedAlbums().contains(albumToExclude));
        verify(albumRepository).saveExcludedAlbums(musicDir, exclusionFile, Set.of(albumToExclude));
    }

    @Test
    void excludeAlbum_shouldRemoveFromCurrentPicks() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        albumService.generateNewPicks(sampleAlbums.size());
        Path albumToExclude = sampleAlbums.get(0);
        
        // When
        albumService.excludeAlbum(albumToExclude);
        
        // Then
        assertFalse(albumService.getCurrentPicks().contains(albumToExclude));
    }

    @Test
    void searchAlbums_shouldReturnMatchingAlbums() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        when(settings.isApplyExclusionsToSearch()).thenReturn(true);
        
        // When - Test with exclusions applied (default behavior)
        List<Path> results = albumService.searchAlbums(List.of("Artist1"), 10);
        
        // Then
        assertEquals(2, results.size());
        assertTrue(results.contains(sampleAlbums.get(0)));
        assertTrue(results.contains(sampleAlbums.get(1)));

        // Some special cases in album and search names
        assertEquals(1, albumService.searchAlbums(List.of("tist2", "bum2"), 10).size());
        assertEquals(1, albumService.searchAlbums(List.of("Ar-tist2"), 10).size());
    }

    @Test
    void searchAlbums_shouldRespectMaxResults() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        when(settings.isApplyExclusionsToSearch()).thenReturn(true);
        
        // When
        List<Path> results = albumService.searchAlbums(List.of("Album"), 2);
        
        // Then
        assertEquals(2, results.size());
    }

    @Test
    void searchAlbums_shouldReturnEmptyListForEmptySearchTerms() {
        // When/Then - Should return empty list for empty search terms
        assertTrue(albumService.searchAlbums(Collections.emptyList(), 10).isEmpty());
    }

    @Test
    void searchAlbums_shouldExcludeAlbumsWhenSettingIsEnabled() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        when(settings.isApplyExclusionsToSearch()).thenReturn(true);
        
        Path excludedAlbum = sampleAlbums.get(0);
        albumService.excludeAlbum(excludedAlbum);
        
        // When - With exclusions enabled (default)
        List<Path> results = albumService.searchAlbums(List.of("Artist1"), 10);
        
        // Then - Should not contain excluded album
        assertEquals(1, results.size());
        assertFalse(results.contains(excludedAlbum));
        assertTrue(results.contains(sampleAlbums.get(1))); // The other album from Artist1
    }
    
    @Test
    void searchAlbums_shouldIncludeExcludedAlbumsWhenSettingIsDisabled() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        when(settings.isApplyExclusionsToSearch()).thenReturn(false);
        
        Path excludedAlbum = sampleAlbums.get(0);
        albumService.excludeAlbum(excludedAlbum);
        
        // When - With exclusions disabled
        List<Path> results = albumService.searchAlbums(List.of("Artist1"), 10);
        
        // Then - Should include excluded album in results
        assertEquals(2, results.size());
        assertTrue(results.contains(excludedAlbum));
        assertTrue(results.contains(sampleAlbums.get(1)));
    }
    
    @Test
    void searchAlbums_shouldHandleMixedCaseSearchTerms() throws Exception {
        // Given
        when(albumRepository.findAllAlbums(musicDir)).thenReturn(new ArrayList<>(sampleAlbums));
        when(settings.isApplyExclusionsToSearch()).thenReturn(true);
        
        // When - Search with mixed case
        List<Path> results = albumService.searchAlbums(List.of("aRtIsT1"), 10);
        
        // Then - Should match case-insensitively
        assertEquals(2, results.size());
        assertTrue(results.contains(sampleAlbums.get(0)));
        assertTrue(results.contains(sampleAlbums.get(1)));
    }
    
    @Test
    void loadSavedPicks_shouldLoadPicksFromRepository() throws Exception {
        // Given
        List<Path> savedPicks = List.of(sampleAlbums.get(0), sampleAlbums.get(1));
        when(albumRepository.loadAlbumPicks(savedPicksFile)).thenReturn(savedPicks);
        
        // When
        albumService.loadSavedPicks();
        
        // Then
        assertEquals(savedPicks, albumService.getCurrentPicks());
    }

    @Test
    void loadExcludedAlbums_shouldReloadExcludedAlbums() throws Exception {
        // Given
        Set<Path> excluded = Set.of(sampleAlbums.get(0));
        when(albumRepository.loadExcludedAlbums(musicDir, exclusionFile)).thenReturn(excluded);
        
        // When
        albumService.loadExcludedAlbums();
        
        // Then
        assertEquals(excluded, albumService.getExcludedAlbums());
    }

}

package com.musimizer.service;

import com.musimizer.exception.MusicDirectoryException;
import com.musimizer.repository.AlbumRepository;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class AlbumService {
    private final AlbumRepository albumRepository;
    private final Path musicDir;
    private final Path exclusionFile;
    private final Path savedPicksFile;
    private final Set<Path> excludedAlbums;
    private List<Path> currentPicks;

    public AlbumService(AlbumRepository albumRepository, Path musicDir, Path exclusionFile) {
        this.albumRepository = albumRepository;
        this.musicDir = musicDir;
        this.exclusionFile = exclusionFile;
        this.savedPicksFile = exclusionFile.getParent().resolve("saved_picks.txt");
        this.excludedAlbums = new HashSet<>();
        this.currentPicks = new ArrayList<>();
        
        loadExcludedAlbums();
    }

    public void loadExcludedAlbums() {
        try {
            excludedAlbums.clear();
            excludedAlbums.addAll(albumRepository.loadExcludedAlbums(musicDir, exclusionFile));
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to load excluded albums", e);
        }
    }

    public void loadSavedPicks() {
        try {
            currentPicks = albumRepository.loadAlbumPicks(savedPicksFile);
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to load saved picks", e);
        }
    }

    public void generateNewPicks(int numberOfPicks) {
        List<Path> allAlbums = findAllAlbums();
        List<Path> eligibleAlbums = allAlbums.stream()
                .filter(album -> !excludedAlbums.contains(album))
                .collect(Collectors.toList());

        if (eligibleAlbums.isEmpty()) {
            throw new IllegalStateException("No albums available after applying exclusions");
        }

        Collections.shuffle(eligibleAlbums);
        currentPicks = eligibleAlbums.stream()
                .limit(Math.min(numberOfPicks, eligibleAlbums.size()))
                .collect(Collectors.toList());

        saveCurrentPicks();
    }

    public void excludeAlbum(Path albumPath) {
        excludedAlbums.add(albumPath);
        currentPicks.remove(albumPath);
        saveExcludedAlbums();
        saveCurrentPicks();
    }

    public List<Path> searchAlbums(List<String> searchTerms, int maxResults) {
        if (searchTerms == null || searchTerms.isEmpty() || maxResults <= 0) {
            return Collections.emptyList();
        }

        List<Path> allAlbums = findAllAlbums();
        List<Path> searchResults = allAlbums.stream()
                .filter(album -> !excludedAlbums.contains(album))
                .filter(album -> {
                    String searchableAlbum = albumPathToDisplayString(album).toLowerCase();
                    return searchTerms.stream()
                            .map(String::toLowerCase)
                            .allMatch(searchableAlbum::contains);
                })
                .collect(Collectors.toList());

        Collections.shuffle(searchResults);
        return searchResults.subList(0, Math.min(maxResults, searchResults.size()));
    }

    private List<Path> findAllAlbums() {
        try {
            return albumRepository.findAllAlbums(musicDir);
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to find albums in directory: " + musicDir, e);
        }
    }

    private void saveExcludedAlbums() {
        try {
            albumRepository.saveExcludedAlbums(musicDir, exclusionFile, excludedAlbums);
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to save excluded albums", e);
        }
    }

    private void saveCurrentPicks() {
        try {
            albumRepository.saveAlbumPicks(savedPicksFile, currentPicks);
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to save current picks", e);
        }
    }

    public List<Path> getCurrentPicks() {
        return Collections.unmodifiableList(currentPicks);
    }

    public String albumPathToDisplayString(Path albumPath) {
        if (albumPath == null) {
            return "";
        }
        Path artistDir = albumPath.getParent();
        if (artistDir == null) {
            return albumPath.getFileName().toString();
        }
        return artistDir.getFileName().toString() + " - " + albumPath.getFileName().toString();
    }

    public Set<Path> getExcludedAlbums() {
        return Collections.unmodifiableSet(excludedAlbums);
    }
}

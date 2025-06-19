package com.musimizer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class AlbumPicker {
    private final Path musicDir;
    private final Path exclusionFile;
    private final Path savedPicksFile;
    private final Set<String> excludedAlbums;
    private List<String> currentPicks;

    public AlbumPicker(Path musicDir, Path exclusionFile) {
        this.musicDir = musicDir;
        this.exclusionFile = exclusionFile;
        this.savedPicksFile = exclusionFile.getParent().resolve("saved_picks.txt");
        this.excludedAlbums = loadExclusions();
        this.currentPicks = new ArrayList<>();
    }

    public List<String> getCurrentPicks() {
        return Collections.unmodifiableList(currentPicks);
    }
        
    private boolean isMusicDirectoryValid() {
        if (musicDir == null || !Files.exists(musicDir) || !Files.isDirectory(musicDir)) {
            return false;
        }
        return true;
    }
    
    public void generateNewPicks(int n) {
        // Check if music directory is valid
        if (!isMusicDirectoryValid()) {
            throw new IllegalStateException("Music directory does not exist or is not accessible: " + 
                (musicDir != null ? musicDir.toAbsolutePath() : "[not set]"));
        }
        
        // Find all albums and filter out excluded ones
        List<String> allAlbums = findAlbums();
        List<String> eligible = allAlbums.stream()
                .filter(album -> !excludedAlbums.contains(album))
                .collect(Collectors.toList());
                
        if (eligible.isEmpty()) {
            throw new IllegalStateException("No albums found in the music directory or all albums are excluded: " + 
                musicDir.toAbsolutePath());
        }
        
        // Shuffle and select the requested number of albums
        Collections.shuffle(eligible);
        currentPicks = eligible.stream()
            .limit(Math.min(n, eligible.size()))
            .collect(Collectors.toList());
            
        saveCurrentPicks();
    }

    public void excludeAlbum(String albumPath) {
        excludedAlbums.add(albumPath);
        // Remove from current picks if present
        currentPicks.remove(albumPath);
        saveCurrentPicks();
        saveExclusions();
    }

    private Set<String> loadExclusions() {
        if (!Files.exists(exclusionFile))
            return new HashSet<>();
        try {
            return new HashSet<>(Files.readAllLines(exclusionFile));
        } catch (IOException e) {
            AppUI.showError("Error Loading Exclusions", "Failed to load exclusion list", 
                "Could not read from exclusion file: " + exclusionFile + "\n\n" + e.getMessage());
            return new HashSet<>();
        }
    }

    private void saveExclusions() {
        try {
            Files.createDirectories(exclusionFile.getParent());
            Files.write(exclusionFile, excludedAlbums);
        } catch (IOException e) {
            AppUI.showError("Error Saving Exclusions", "Failed to save exclusion list", 
                "Could not write to exclusion file: " + exclusionFile + "\n\n" + e.getMessage());
        }
    }

    private List<String> findAlbums() {
        List<String> albums = new ArrayList<>();
        if (musicDir != null && Files.isDirectory(musicDir)) {
            try {
                Files.list(musicDir).filter(Files::isDirectory).forEach(artistDir -> {
                    try {
                        Files.list(artistDir).filter(Files::isDirectory).forEach(albumDir -> {
                            albums.add(artistDir.getFileName().toString() + " - " + albumDir.getFileName().toString());
                        });
                    } catch (IOException e) {
                        AppUI.showError("Error Reading Directory", "Failed to read artist directory", 
                            "Could not read directory: " + artistDir + "\n\n" + e.getMessage());
                    }
                });
            } catch (IOException e) {
                AppUI.showError("Error Reading Music Directory", "Failed to read music directory", 
                    "Could not read music directory: " + musicDir + "\n\n" + e.getMessage());
            }
        }
        return albums;
    }

    public Set<String> getExcludedAlbums() {
        return Collections.unmodifiableSet(excludedAlbums);
    }
    
    public void refreshPicks(int n) {
        generateNewPicks(n);
    }
    
    public void loadSavedPicks() {
        if (!Files.exists(savedPicksFile)) {
            return;
        }
        try {
            currentPicks = Files.readAllLines(savedPicksFile);
        } catch (IOException e) {
            AppUI.showError("Error loading saved picks", "Error loading saved picks", e.getMessage());
        }
    }
    
    private void saveCurrentPicks() {
        if (currentPicks == null || currentPicks.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(savedPicksFile.getParent());
            Files.write(savedPicksFile, currentPicks);
        } catch (IOException e) {
            AppUI.showError("Error Saving Picks", "Failed to save current picks", 
                "Could not write to saved picks file: " + savedPicksFile + "\n\n" + e.getMessage());
        }
    }
}

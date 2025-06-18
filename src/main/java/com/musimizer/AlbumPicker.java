package com.musimizer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class AlbumPicker {
    private final Path musicDir;
    private final Path exclusionFile;
    private Set<String> excludedAlbums;
    public AlbumPicker(Path musicDir, Path exclusionFile) {
        this.musicDir = musicDir;
        this.exclusionFile = exclusionFile;
        this.excludedAlbums = loadExclusions();
    }
    public List<String> pickRandomAlbums(int n) {
        List<String> allAlbums = findAlbums();
        List<String> eligible = allAlbums.stream()
            .filter(album -> !excludedAlbums.contains(album))
            .collect(Collectors.toList());
        Collections.shuffle(eligible);
        return eligible.stream().limit(n).collect(Collectors.toList());
    }
    public void excludeAlbum(String albumPath) {
        excludedAlbums.add(albumPath);
        saveExclusions();
    }
    private Set<String> loadExclusions() {
        if (!Files.exists(exclusionFile)) return new HashSet<>();
        try {
            return new HashSet<>(Files.readAllLines(exclusionFile));
        } catch (IOException e) {
            return new HashSet<>();
        }
    }
    private void saveExclusions() {
        try {
            Files.createDirectories(exclusionFile.getParent());
            Files.write(exclusionFile, excludedAlbums);
        } catch (IOException ignored) {}
    }
    private List<String> findAlbums() {
        List<String> albums = new ArrayList<>();
        if (Files.isDirectory(musicDir)) {
            try {
                Files.list(musicDir).filter(Files::isDirectory).forEach(artistDir -> {
                    try {
                        Files.list(artistDir).filter(Files::isDirectory).forEach(albumDir -> {
                            albums.add(albumDir.toAbsolutePath().toString());
                        });
                    } catch (IOException ignored) {}
                });
            } catch (IOException ignored) {}
        }
        return albums;
    }
    public Set<String> getExcludedAlbums() {
        return Collections.unmodifiableSet(excludedAlbums);
    }
}

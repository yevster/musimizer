package com.musimizer.repository;

import com.musimizer.exception.MusicDirectoryException;
import com.musimizer.util.FileSystemManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File-based implementation of AlbumRepository that stores album data on the filesystem.
 * Handles reading and writing album data to/from files.
 */

public class FileAlbumRepository implements AlbumRepository {
    @Override
    public List<String> loadAlbumPicks(Path savedPicksFile, Path exclusionFile) {
        try {
            if (FileSystemManager.exists(savedPicksFile)) {
                return FileSystemManager.readAllLines(savedPicksFile);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to load album picks", e);
        }
    }

    @Override
    public void saveAlbumPicks(Path file, List<String> albumPicks) {
        try {
            FileSystemManager.writeAllLines(file, albumPicks);
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to save album picks", e);
        }
    }

    @Override
    public Set<String> loadExcludedAlbums(Path exclusionFile) {
        try {
            if (FileSystemManager.exists(exclusionFile)) {
                return new HashSet<>(FileSystemManager.readAllLines(exclusionFile));
            }
            return new HashSet<>();
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to load excluded albums", e);
        }
    }

    @Override
    public void saveExcludedAlbums(Path exclusionFile, Set<String> excludedAlbums) {
        try {
            FileSystemManager.writeAllLines(exclusionFile, new ArrayList<>(excludedAlbums));
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to save excluded albums", e);
        }
    }

    @Override
    public List<String> findAllAlbums(Path musicDir) {
        if (!FileSystemManager.exists(musicDir) || !FileSystemManager.isDirectory(musicDir)) {
            throw new MusicDirectoryException("Music directory does not exist or is not accessible: " + musicDir);
        }

        try (Stream<Path> artistDirs = Files.list(musicDir)) {
            // Collect results into a list immediately to avoid stream reuse issues
            return artistDirs
                .filter(Files::isDirectory)
                .flatMap(this::getAlbumsForArtist)
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to list albums in directory: " + musicDir, e);
        }
    }

    private Stream<String> getAlbumsForArtist(Path artistDir) {
        try (Stream<Path> albumDirs = Files.list(artistDir)) {
            // Collect results into a list first to avoid stream reuse issues
            List<String> albums = albumDirs
                .filter(Files::isDirectory)
                .map(albumDir -> artistDir.getFileName() + " - " + albumDir.getFileName())
                .collect(Collectors.toList());
            return albums.stream();
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to read artist directory: " + artistDir, e);
        }
    }
}

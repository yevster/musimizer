package com.musimizer.repository;

import com.musimizer.exception.MusicDirectoryException;

import java.io.File;
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
    private List<Path> allAlbums = null;
    public FileAlbumRepository() {
    }

    @Override
    public List<Path> loadAlbumPicks(Path savedPicksFile) {
        try {
            if (Files.exists(savedPicksFile)) {
                return Files.readAllLines(savedPicksFile).stream()
                        .map(Path::of)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to load album picks", e);
        }
    }

    @Override
    public void saveAlbumPicks(Path file, List<Path> albumPicks) {
        try {
            List<String> pathsAsStrings = albumPicks.stream()
                    .map(Path::toString)
                    .collect(Collectors.toList());
            Files.write(file, pathsAsStrings);
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to save album picks", e);
        }
    }

    @Override
    public Set<Path> loadExcludedAlbums(Path musicDirectory, Path exclusionFile) {
        try {
            if (!Files.exists(exclusionFile))
                return Set.of();
            return Files.readAllLines(exclusionFile).stream()
                    .map(musicDirectory::resolve)
                    .collect(Collectors.toCollection(()->new LinkedHashSet<Path>()));
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to load excluded albums", e);
        }
    }

    @Override
    public void saveExcludedAlbums(Path musicDirectory, Path exclusionFile, Collection<Path> excludedAlbums) {
        try {
            // Convert excludedAlbums to String paths relative to musicDir
            var pathsAsStrings = excludedAlbums.stream()
                    .map(musicDirectory::relativize)
                    .map(Path::toString)
                    .map(pathString -> pathString.replace(File.separator, "/"))
                    .collect(Collectors.toList());
            Files.write(exclusionFile, pathsAsStrings);
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to save excluded albums", e);
        }
    }

    @Override
    public SequencedSet<Path> loadBookmarks(Path bookmarksFile) {
        try {
            if (Files.exists(bookmarksFile)) {
                return Files.readAllLines(bookmarksFile).stream()
                        .map(Path::of)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
            return new LinkedHashSet<>();
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to load bookmarks", e);
        }
    }

    @Override
    public void saveBookmarks(Path bookmarksFile, Collection<Path> bookmarks) {
        try {
            Files.createDirectories(bookmarksFile.getParent());
            var pathsAsStrings = bookmarks.stream()
                    .map(Path::toString)
                    .collect(Collectors.toList());
            Files.write(bookmarksFile, pathsAsStrings);
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to save bookmarks", e);
        }
    }

    @Override
    public List<Path> findAllAlbums(Path musicDir) {
        if (!Files.exists(musicDir) || !Files.isDirectory(musicDir)) {
            throw new MusicDirectoryException("Music directory does not exist or is not accessible: " + musicDir);
        }

        if (allAlbums != null) 
            return allAlbums;

        try (Stream<Path> artists = Files.list(musicDir)) {
            allAlbums = artists
                    .filter(Files::isDirectory)
                    .flatMap(artistDir -> {
                        try (Stream<Path> albums = Files.list(artistDir)) {
                            return albums
                                    .filter(Files::isDirectory)
                                    .map(Path::toAbsolutePath)
                                    .collect(Collectors.toList()).stream();
                        } catch (IOException e) {
                            throw new MusicDirectoryException("Failed to read artist directory: " + artistDir, e);
                        }
                    })
                    .collect(Collectors.toList());
            return allAlbums;
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to list albums in directory: " + musicDir, e);
        }
    }
}

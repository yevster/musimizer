package com.musimizer.repository;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AlbumRepository {
    List<Path> loadAlbumPicks(Path savedPicksFile);
    void saveAlbumPicks(Path file, List<Path> albumPicks);
    Set<Path> loadExcludedAlbums(Path musicDir, Path exclusionFile);
    void saveExcludedAlbums(Path musicDir, Path exclusionFile, Collection<Path> excludedAlbums);
    List<Path> findAllAlbums(Path musicDir);
}

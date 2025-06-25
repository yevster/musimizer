package com.musimizer.repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface AlbumRepository {
    List<String> loadAlbumPicks(Path musicDir, Path exclusionFile) throws Exception;
    void saveAlbumPicks(Path file, List<String> albumPicks) throws Exception;
    Set<String> loadExcludedAlbums(Path exclusionFile) throws Exception;
    void saveExcludedAlbums(Path exclusionFile, Set<String> excludedAlbums) throws Exception;
    List<String> findAllAlbums(Path musicDir) throws Exception;
}

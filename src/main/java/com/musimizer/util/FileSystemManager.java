package com.musimizer.util;

import com.musimizer.exception.MusicDirectoryException;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSystemManager {
    private FileSystemManager() {
        // Private constructor to prevent instantiation
    }
    
    public static List<Path> listDirectories(Path directory) throws MusicDirectoryException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                .filter(Files::isDirectory)
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to list directories in: " + directory, e);
        }
    }
    
    public static void createDirectories(Path path) throws MusicDirectoryException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to create directory: " + path, e);
        }
    }
    
    public static List<String> readAllLines(Path file) throws MusicDirectoryException {
        try {
            if (Files.exists(file)) {
                return Files.readAllLines(file);
            }
            return List.of();
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to read file: " + file, e);
        }
    }
    
    public static void writeAllLines(Path file, Iterable<? extends CharSequence> lines) throws MusicDirectoryException {
        try {
            createDirectories(file.getParent());
            Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to write to file: " + file, e);
        }
    }
    
    public static boolean exists(Path path) {
        return Files.exists(path);
    }
    
    public static boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }
}

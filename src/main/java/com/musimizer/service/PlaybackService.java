package com.musimizer.service;

import java.awt.Desktop;
import java.io.BufferedWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class PlaybackService {

    private static final String TEMP_DIR_PREFIX = "musimizer-playlist-";
    private Path tempDir;

    public PlaybackService() {
        try {
            this.tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
            // Register a shutdown hook to delete the temp directory on JVM exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.delete(tempDir);
                } catch (IOException e) {
                    // Log this exception properly in a real application
                    System.err.println("Error deleting temporary directory: " + tempDir);
                    e.printStackTrace();
                }
            }));
        } catch (IOException e) {
            // Handle exception properly
            e.printStackTrace();
            // Maybe throw a custom exception to be handled by the controller
        }
    }

    public void playAlbum(Path albumPath) {
        try {
            List<Path> trackPaths = Files.list(albumPath)
                    .filter(p -> p.toString().toLowerCase().endsWith(".mp3") ||
                            p.toString().toLowerCase().endsWith(".flac") ||
                            p.toString().toLowerCase().endsWith(".wav") ||
                            p.toString().toLowerCase().endsWith(".m4a") ||
                            p.toString().toLowerCase().endsWith(".aac") ||
                            p.toString().toLowerCase().endsWith(".ogg") ||
                            p.toString().toLowerCase().endsWith(".m4a"))
                    .sorted()
                    .collect(Collectors.toList());

            if (trackPaths.isEmpty()) {
                // Handle case with no playable tracks
                System.out.println("No playable tracks found in: " + albumPath);
                return;
            }

            Path playlistFile = createM3u8Playlist(albumPath.getFileName().toString(), trackPaths);
            Desktop.getDesktop().open(playlistFile.toFile());

        } catch (IOException e) {
            e.printStackTrace();
            // Handle this exception in the UI, e.g., show an error dialog
        }
    }

    private Path createM3u8Playlist(String albumName, List<Path> trackPaths) throws IOException {
        Path playlistPath = tempDir.resolve(albumName + ".m3u8");

        try (BufferedWriter writer = Files.newBufferedWriter(playlistPath)) {
            writer.write("#EXTM3U");
            writer.newLine();

            for (Path trackPath : trackPaths) {
                writer.write(trackPath.toUri().toString());
                writer.newLine();
            }
        }

        return playlistPath;
    }
}

package com.musimizer.service;

import com.musimizer.exception.MusicDirectoryException;
import com.musimizer.util.FileSystemManager;
import com.musimizer.util.SettingsManager;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for handling audio playback of albums.
 * Creates temporary M3U8 playlists for albums and launches them in the default media player.
 */
public class PlaybackService {
    private static final String TEMP_DIR_PREFIX = "musimizer_playback_";
    private static final String M3U8_HEADER = "#EXTM3U\n";
    private Path tempDir;

    public PlaybackService() {
        try {
            // Create a temporary directory for playlists that will be deleted on exit
            tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
            tempDir.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new MusicDirectoryException("Failed to create temporary directory for playlists", e);
        }
    }

    /**
     * Plays an album by creating an M3U8 playlist and launching it in the default media player.
     * @param albumPath The path of the album to play, in the format "Artist - Album"
     */
    public void playAlbum(String albumPath) {
        try {
            // Parse artist and album from the path
            String[] parts = albumPath.split(" - ", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid album path format. Expected 'Artist - Album'");
            }
            
            String artist = parts[0].trim();
            String album = parts[1].trim();
            
            // Get the album directory
            Path albumDir = Paths.get(SettingsManager.getMusicDir(), artist, album);
            if (!Files.isDirectory(albumDir)) {
                throw new MusicDirectoryException("Album directory not found: " + albumDir);
            }
            
            // Create playlist content
            String playlistContent = createPlaylistContent(albumDir);
            
            // Create a temporary playlist file
            String safeAlbumName = album.replaceAll("[^a-zA-Z0-9.-]", "_");
            Path playlistFile = tempDir.resolve(safeAlbumName + ".m3u8");
            
            // Write the playlist file
            Files.writeString(playlistFile, playlistContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // Launch the default media player with the playlist
            launchMediaPlayer(playlistFile);
            
        } catch (Exception e) {
            throw new MusicDirectoryException("Failed to play album: " + albumPath, e);
        }
    }
    
    /**
     * Creates the content of an M3U8 playlist for the given album directory.
     */
    private String createPlaylistContent(Path albumDir) throws IOException {
        StringBuilder sb = new StringBuilder(M3U8_HEADER);
        
        // Find all audio files in the album directory, sorted by filename
        try (Stream<Path> walk = Files.walk(albumDir, 1)) {
            List<Path> audioFiles = walk
                .filter(Files::isRegularFile)
                .filter(this::isAudioFile)
                .sorted(Comparator.comparing(Path::getFileName))
                .collect(Collectors.toList());
                
            if (audioFiles.isEmpty()) {
                throw new MusicDirectoryException("No audio files found in album: " + albumDir);
            }
            
            // Add each track to the playlist
            for (Path audioFile : audioFiles) {
                sb.append("#EXTINF:0,").append(getFileNameWithoutExtension(audioFile)).append("\n");
                sb.append(audioFile.toAbsolutePath().toString()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Checks if a file is an audio file based on its extension.
     */
    private boolean isAudioFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".mp3") || 
               fileName.endsWith(".m4a") || 
               fileName.endsWith(".flac") || 
               fileName.endsWith(".wav") || 
               fileName.endsWith(".ogg");
    }
    
    /**
     * Gets the filename without its extension.
     */
    private String getFileNameWithoutExtension(Path file) {
        String fileName = file.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    /**
     * Launches the default media player with the given playlist file.
     */
    private void launchMediaPlayer(Path playlistFile) throws IOException {
        try {
            java.awt.Desktop.getDesktop().open(playlistFile.toFile());
        } catch (java.awt.HeadlessException e) {
            throw new IOException("Desktop operations not supported in headless environment", e);
        }
    }
}

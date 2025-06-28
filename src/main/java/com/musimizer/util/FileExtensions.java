package com.musimizer.util;

import java.nio.file.Path;
import java.util.Set;

/**
 * Utility class for file extensions used in the application.
 */
public final class FileExtensions {
    // Prevent instantiation
    private FileExtensions() {}
    
    /**
     * Set of supported audio file extensions (lowercase, with leading dot)
     */
    public static final Set<String> AUDIO_EXTENSIONS = Set.of(
        ".mp3", 
        ".m4a", 
        ".flac", 
        ".wav", 
        ".ogg", 
        ".wma",
        ".aac",
        ".alac",
        ".aiff"
    );
    
    /**
     * Checks if a file path has an audio file extension.
     * 
     * @param filePath the file path to check
     * @return true if the file has an audio extension (case-insensitive check)
     */
    public static boolean isAudioFile(String filePath) {
        if (filePath == null || filePath.lastIndexOf('.') == -1)
            return false;
            
        String extension = filePath.substring(filePath.lastIndexOf('.')).toLowerCase();
        return AUDIO_EXTENSIONS.contains(extension);
    }
    
    /**
     * Checks if a file path has an audio file extension.
     * 
     * @param filePath the file path to check
     * @return true if the file has an audio extension (case-insensitive check)
     */
    public static boolean isAudioFile(Path filePath) {
        return filePath != null && isAudioFile(filePath.getFileName().toString());
    }
}

package com.musimizer.util;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;

import javafx.scene.image.Image;

/**
 * Utility class for extracting album art from music files.
 * Supports multiple audio formats as defined in FileExtensions.
 */
public class AlbumArtExtractor {
    private static final java.util.logging.Logger LOGGER = 
        java.util.logging.Logger.getLogger(AlbumArtExtractor.class.getName());
    
    /**
     * Finds the first music file in the given directory.
     * 
     * @param directory The directory to search in
     * @return Optional containing the path to the first music file, or empty if none found
     */
    public static Optional<Path> findFirstMusicFile(Path directory) {
        try (var files = Files.list(directory)) {
            return files
                .filter(Files::isRegularFile)
                .filter(FileExtensions::isAudioFile)
                .sorted()
                .findFirst();
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Error finding music files in " + directory, e);
            return Optional.empty();
        }
    }
    
    /**
     * Extracts album art from the given music file.
     * Supports multiple audio formats as defined in FileExtensions.
     * 
     * @param musicFile The music file to extract art from
     * @return Optional containing the album art as an Image, or empty if no art found
     */
    public static Optional<Image> extractAlbumArt(Path musicFile) {
        if (!FileExtensions.isAudioFile(musicFile)) {
            return Optional.empty();
        }
        
        String fileName = musicFile.getFileName().toString().toLowerCase(Locale.ROOT);
        
        try {
            // Handle MP3 files
            if (fileName.endsWith(".mp3")) {
                return extractMp3AlbumArt(musicFile);
            }
            // Add support for other formats here as needed
            // Example: 
            // else if (fileName.endsWith(".m4a") || fileName.endsWith(".aac")) {
            //     return extractAacAlbumArt(musicFile);
            // }
            
            LOGGER.log(Level.FINE, "Album art extraction not yet implemented for file: " + musicFile);
            return Optional.empty();
            
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error extracting album art from " + musicFile, e);
            return Optional.empty();
        }
    }
    
    /**
     * Extracts album art from an MP3 file using ID3v2 tags.
     * 
     * @param mp3File The MP3 file to extract art from
     * @return Optional containing the album art as an Image, or empty if no art found
     */
    private static Optional<Image> extractMp3AlbumArt(Path mp3File) {
        try {
            Mp3File mp3file = new Mp3File(mp3File);
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2tag = mp3file.getId3v2Tag();
                byte[] imageData = id3v2tag.getAlbumImage();
                
                if (imageData != null && imageData.length > 0) {
                    return Optional.of(new Image(new ByteArrayInputStream(imageData)));
                }
            }
            return Optional.empty();
            
        } catch (UnsupportedTagException | InvalidDataException e) {
            LOGGER.log(Level.FINE, "Invalid MP3 file or tags: " + mp3File, e);
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading MP3 file: " + mp3File, e);
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Unexpected error processing MP3 file: " + mp3File, e);
            return Optional.empty();
        }
    }
    
    /**
     * Gets album art for the first music file in the given directory.
     * 
     * @param directory The directory containing the album
     * @return Optional containing the album art as an Image, or empty if no art found
     */
    public static Optional<Image> getAlbumArt(Path directory) {
        return findFirstMusicFile(directory)
            .flatMap(AlbumArtExtractor::extractAlbumArt);
    }
    
}

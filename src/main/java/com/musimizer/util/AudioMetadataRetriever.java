package com.musimizer.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Utility class for retrieving metadata from audio files.
 * Currently supports extracting album art from MP3 and AAC (M4A) files.
 */
public class AudioMetadataRetriever {
    private static final byte[] ID3_HEADER = "ID3".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] MP4_HEADER = "ftyp".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] COVER_ART_MP3 = "APIC".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] COVER_ART_AAC = "covr".getBytes(StandardCharsets.ISO_8859_1);

    private static final Logger LOGGER = Logger.getLogger(AudioMetadataRetriever.class.getName());

    /**
     * Extracts the album cover image from the specified audio file.
     *
     * @param audioFilePath path to the audio file
     * @return byte array containing the cover image data, or null if no cover art is found
     * @throws IOException if an I/O error occurs
     */
    public static byte[] getCoverImage(Path audioFilePath) throws IOException {
        if (audioFilePath == null) {
            LOGGER.warning("Audio file path is null");
            return null;
        }
        
        LOGGER.fine("Extracting cover art from: " + audioFilePath);
        
        String fileName = audioFilePath.getFileName().toString().toLowerCase();
        try (RandomAccessFile file = new RandomAccessFile(audioFilePath.toFile(), "r");
             FileChannel channel = file.getChannel()) {
            
            byte[] imageData = null;
            
            if (fileName.endsWith(".mp3")) {
                imageData = extractMp3CoverArt(file, channel);
            } else if (fileName.endsWith(".m4a") || fileName.endsWith(".aac")) {
                imageData = extractAacCoverArt(file, channel);
            } else {
                LOGGER.warning("Unsupported audio format: " + fileName);
                return null;
            }
            
            if (imageData == null || imageData.length == 0) {
                LOGGER.fine("No cover art found in: " + audioFilePath);
                return null;
            }
            
            // Ensure the image data has a proper header
            if (!hasValidImageHeader(imageData)) {
                LOGGER.fine("Image data has invalid header, attempting to fix...");
                imageData = fixImageHeader(imageData);
                
                if (imageData == null || !hasValidImageHeader(imageData)) {
                    LOGGER.warning("Failed to fix image header for: " + audioFilePath);
                    return null;
                }
            }
            
            LOGGER.fine("Successfully extracted cover art, size: " + imageData.length + " bytes");
            return imageData;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting cover art from: " + audioFilePath, e);
            return null;
        }
    }
    
    private static boolean hasValidImageHeader(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        
        // Check for JPEG
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
            return true;
        }
        
        // Check for PNG
        if (data[0] == (byte) 0x89 && data[1] == 0x50 && 
            data[2] == 0x4E && data[3] == 0x47) {
            return true;
        }
        
        return false;
    }
    
    private static byte[] fixImageHeader(byte[] imageData) {
        if (imageData == null || imageData.length < 4) {
            return imageData;
        }
        
        // Try to find JPEG start marker (0xFF 0xD8)
        for (int i = 0; i < imageData.length - 1; i++) {
            if (imageData[i] == (byte) 0xFF && imageData[i + 1] == (byte) 0xD8) {
                // Found JPEG start, extract from here
                byte[] fixedData = new byte[imageData.length - i];
                System.arraycopy(imageData, i, fixedData, 0, fixedData.length);
                
                // Ensure it has a proper end marker
                if (fixedData.length >= 2 && 
                    fixedData[fixedData.length - 2] == (byte) 0xFF && 
                    fixedData[fixedData.length - 1] == (byte) 0xD9) {
                    return fixedData;
                } else {
                    // Add JPEG end marker if missing
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write(fixedData, 0, fixedData.length);
                    baos.write(0xFF);
                    baos.write(0xD9);
                    return baos.toByteArray();
                }
            }
        }
        
        // If we get here, we couldn't find a valid header
        return null;
    }

    private static byte[] extractMp3CoverArt(RandomAccessFile file, FileChannel channel) throws IOException {
        // Skip ID3 header (10 bytes: 'ID3' + version + flags + size)
        file.seek(10);
        
        // Read ID3 tags
        while (file.getFilePointer() < file.length() - 10) {
            byte[] frameHeader = new byte[10];
            if (file.read(frameHeader) != 10) break;
            
            String frameId = new String(frameHeader, 0, 4, StandardCharsets.ISO_8859_1);
            int frameSize = ((frameHeader[4] & 0xFF) << 24) | 
                          ((frameHeader[5] & 0xFF) << 16) | 
                          ((frameHeader[6] & 0xFF) << 8) | 
                          (frameHeader[7] & 0xFF);
            
            if (frameId.startsWith("APIC")) {
                // Found APIC frame (picture)
                file.skipBytes(1); // Skip text encoding
                
                // Read mime type (null-terminated string)
                StringBuilder mimeType = new StringBuilder();
                byte b;
                while ((b = file.readByte()) != 0) {
                    mimeType.append((char) b);
                }
                
                // Skip picture type
                file.skipBytes(1);
                
                // Read description (null-terminated string)
                while (file.readByte() != 0) {
                    // Skip until null terminator
                }
                
                // The rest is the image data
                int imageSize = frameSize - mimeType.length() - 2; // -2 for the two null terminators we skipped
                byte[] imageData = new byte[imageSize];
                file.readFully(imageData);
                return imageData;
            } else {
                // Skip to next frame
                file.skipBytes(frameSize);
            }
        }
        return null;
    }

    private static byte[] extractAacCoverArt(RandomAccessFile file, FileChannel channel) throws IOException {
        // MP4/M4A files use a box structure
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        
        // Skip to the first box
        file.seek(0);
        
        while (file.getFilePointer() < file.length() - 8) {
            // Read box header (size + type)
            buffer.clear();
            if (file.read(buffer.array(), 0, 8) != 8) break;
            
            long boxSize = buffer.getInt(0) & 0xFFFFFFFFL;
            if (boxSize == 1) {
                // Extended size (64-bit)
                buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
                file.read(buffer.array(), 8, 8);
                boxSize = buffer.getLong(8);
            }
            
            String boxType = new String(buffer.array(), 4, 4, StandardCharsets.ISO_8859_1);
            
            if (boxType.equals("moov")) {
                // Found moov box, now look for 'udta' -> 'meta' -> 'ilst' -> 'covr'
                return findCoverArtInMoov(file, boxSize - 8);
            }
            
            // Move to next box
            if (boxSize > 0) {
                file.seek(file.getFilePointer() + boxSize - 8);
            } else {
                break; // Invalid box size
            }
        }
        return null;
    }
    
    private static byte[] findCoverArtInMoov(RandomAccessFile file, long moovSize) throws IOException {
        long moovEnd = file.getFilePointer() + moovSize;
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        
        while (file.getFilePointer() < moovEnd - 8) {
            buffer.clear();
            if (file.read(buffer.array(), 0, 8) != 8) break;
            
            long boxSize = buffer.getInt(0) & 0xFFFFFFFFL;
            String boxType = new String(buffer.array(), 4, 4, StandardCharsets.ISO_8859_1);
            
            if (boxType.equals("udta")) {
                // Found user data box, look for 'meta' -> 'ilst' -> 'covr'
                return findCoverArtInUdta(file, boxSize - 8);
            }
            
            // Skip to next box
            if (boxSize > 0) {
                file.seek(file.getFilePointer() + boxSize - 8);
            } else {
                break;
            }
        }
        return null;
    }
    
    private static byte[] findCoverArtInUdta(RandomAccessFile file, long udtaSize) throws IOException {
        long udtaEnd = file.getFilePointer() + udtaSize;
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        
        while (file.getFilePointer() < udtaEnd - 8) {
            buffer.clear();
            if (file.read(buffer.array(), 0, 8) != 8) break;
            
            long boxSize = buffer.getInt(0) & 0xFFFFFFFFL;
            String boxType = new String(buffer.array(), 4, 4, StandardCharsets.ISO_8859_1);
            
            if (boxType.equals("meta")) {
                // Skip version and flags (4 bytes)
                file.skipBytes(4);
                return findCoverArtInMeta(file, boxSize - 12);
            }
            
            // Skip to next box
            if (boxSize > 0) {
                file.seek(file.getFilePointer() + boxSize - 8);
            } else {
                break;
            }
        }
        return null;
    }
    
    private static byte[] findCoverArtInMeta(RandomAccessFile file, long metaSize) throws IOException {
        long metaEnd = file.getFilePointer() + metaSize;
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        
        while (file.getFilePointer() < metaEnd - 8) {
            buffer.clear();
            if (file.read(buffer.array(), 0, 8) != 8) break;
            
            long boxSize = buffer.getInt(0) & 0xFFFFFFFFL;
            String boxType = new String(buffer.array(), 4, 4, StandardCharsets.ISO_8859_1);
            
            if (boxType.equals("ilst")) {
                // Found item list box, look for 'covr' box
                return findCoverArtInIlst(file, boxSize - 8);
            }
            
            // Skip to next box
            if (boxSize > 0) {
                file.seek(file.getFilePointer() + boxSize - 8);
            } else {
                break;
            }
        }
        return null;
    }
    
    private static byte[] findCoverArtInIlst(RandomAccessFile file, long ilstSize) throws IOException {
        long ilstEnd = file.getFilePointer() + ilstSize;
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        
        while (file.getFilePointer() < ilstEnd - 8) {
            buffer.clear();
            if (file.read(buffer.array(), 0, 8) != 8) break;
            
            long boxSize = buffer.getInt(0) & 0xFFFFFFFFL;
            String boxType = new String(buffer.array(), 4, 4, StandardCharsets.ISO_8859_1);
            
            if (boxType.equals("covr")) {
                // Skip version and flags (4 bytes) and the 'data' box header (8 bytes)
                file.skipBytes(12);
                
                // Read the image data (remaining size - 12 bytes for the headers we skipped)
                int imageSize = (int) (boxSize - 20); // 8 (header) + 12 (skipped) = 20
                byte[] imageData = new byte[imageSize];
                file.readFully(imageData);
                return imageData;
            }
            
            // Skip to next box
            if (boxSize > 0) {
                file.seek(file.getFilePointer() + boxSize - 8);
            } else {
                break;
            }
        }
        return null;
    }
    
    private static boolean startsWith(byte[] array, byte[] prefix) {
        if (array.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean containsAt(byte[] array, byte[] pattern, int offset) {
        if (array.length < offset + pattern.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (array[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }
}

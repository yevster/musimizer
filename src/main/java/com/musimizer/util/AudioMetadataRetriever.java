package com.musimizer.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        
        LOGGER.fine("Searching for cover art in ilst box, size: " + ilstSize);
        
        while (file.getFilePointer() < ilstEnd - 8) {
            long boxStart = file.getFilePointer();
            buffer.clear();
            if (file.read(buffer.array(), 0, 8) != 8) {
                LOGGER.fine("Failed to read box header at position: " + boxStart);
                break;
            }
            
            long boxSize = buffer.getInt(0) & 0xFFFFFFFFL;
            String boxType = new String(buffer.array(), 4, 4, StandardCharsets.ISO_8859_1);
            
            LOGGER.fine(String.format("Found box: %s, size: %d at position: %d", boxType, boxSize, boxStart));
            
            if (boxType.equals("covr")) {
                LOGGER.fine("Found 'covr' box, processing...");
                
                // Read the full box content
                byte[] boxData = new byte[(int)(boxSize - 8)]; // -8 for the header we already read
                if (file.read(boxData) != boxData.length) {
                    LOGGER.warning("Failed to read full covr box data");
                    return null;
                }
                
                // The covr box contains a data atom
                // First 4 bytes: size, next 4 bytes: 'data' tag, next 4 bytes: flags
                if (boxData.length < 12) {
                    LOGGER.warning("covr box too small to contain valid data");
                    return null;
                }
                
                // Check for 'data' tag (bytes 4-8)
                if (!(boxData[4] == 'd' && boxData[5] == 'a' && boxData[6] == 't' && boxData[7] == 'a')) {
                    LOGGER.warning("covr box doesn't contain 'data' atom");
                    // Try to find image data anyway by looking for image headers
                    return findImageInData(boxData, 0);
                }
                
                // Skip the data atom header (8 bytes size + 4 bytes 'data' + 4 bytes flags + 8 bytes reserved = 24 bytes)
                // But some files might have different offsets, so we'll look for image headers
                return findImageInData(boxData, 16); // Start after 'data' + flags (12 bytes) + 4 bytes reserved
            }
            
            // Skip to next box
            if (boxSize > 0) {
                file.seek(boxStart + boxSize);
            } else {
                LOGGER.warning("Invalid box size: " + boxSize);
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
    
    /**
     * Tries to find image data in a byte array by looking for image headers.
     * @param data The data to search in
     * @param startOffset The offset to start searching from
     * @return The image data if found, or null if no valid image header is found
     */
    private static byte[] findImageInData(byte[] data, int startOffset) {
        if (data == null || startOffset >= data.length) {
            return null;
        }
        
        // Look for JPEG start marker (0xFF 0xD8)
        for (int i = startOffset; i < data.length - 1; i++) {
            // Check for JPEG start marker
            if (data[i] == (byte) 0xFF && data[i + 1] == (byte) 0xD8) {
                LOGGER.fine("Found JPEG start marker at offset: " + i);
                // Look for JPEG end marker (0xFF 0xD9)
                for (int j = i + 2; j < data.length - 1; j++) {
                    if (data[j] == (byte) 0xFF && data[j + 1] == (byte) 0xD9) {
                        // Found complete JPEG
                        byte[] imageData = new byte[j - i + 2];
                        System.arraycopy(data, i, imageData, 0, imageData.length);
                        return imageData;
                    }
                }
                // If we get here, we found a start but no end - return what we have
                byte[] imageData = new byte[data.length - i];
                System.arraycopy(data, i, imageData, 0, imageData.length);
                return imageData;
            }
            // Check for PNG header
            else if (i < data.length - 7 && 
                    data[i] == (byte) 0x89 && data[i + 1] == 0x50 && 
                    data[i + 2] == 0x4E && data[i + 3] == 0x47 &&
                    data[i + 4] == 0x0D && data[i + 5] == 0x0A &&
                    data[i + 6] == 0x1A && data[i + 7] == 0x0A) {
                LOGGER.fine("Found PNG header at offset: " + i);
                // For PNG, we'll return everything from the header to the end
                // since PNG has a well-defined end marker (IEND chunk)
                byte[] imageData = new byte[data.length - i];
                System.arraycopy(data, i, imageData, 0, imageData.length);
                return imageData;
            }
        }
        
        LOGGER.warning("Could not find any image data in the provided data");
        return null;
    }
}

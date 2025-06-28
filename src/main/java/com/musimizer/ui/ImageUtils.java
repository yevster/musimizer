package com.musimizer.ui;

import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for image-related operations specific to the UI.
 */
public class ImageUtils {
    private static final Logger LOGGER = Logger.getLogger(ImageUtils.class.getName());
    
    /**
     * Creates a JavaFX Image from a byte array containing image data.
     * The image will be scaled to fit within the specified dimensions while preserving aspect ratio.
     * 
     * @param imageData The raw image data
     * @param targetWidth The target width of the image (use 0 to use image's original width)
     * @param targetHeight The target height of the image (use 0 to use image's original height)
     * @return A JavaFX Image, or null if the image could not be created
     */
    public static Image createImageFromBytes(byte[] imageData, double targetWidth, double targetHeight) {
        if (imageData == null || imageData.length == 0) {
            LOGGER.warning("Cannot create image: image data is null or empty");
            return null;
        }
        
        try (InputStream is = new ByteArrayInputStream(imageData)) {
            // Create the image directly from the input stream
            Image image = new Image(
                is,
                targetWidth,  // requested width (0 = use image width)
                targetHeight, // requested height (0 = use image height)
                true,         // preserve ratio
                true          // smooth scaling
            );
            
            if (image.isError()) {
                throw image.getException();
            }
            
            LOGGER.fine(String.format(
                "Created image from byte array. Dimensions: %.1fx%.1f",
                image.getWidth(), image.getHeight()
            ));
            
            return image;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create image from byte array: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Determines the image type based on the magic number in the byte array.
     * 
     * @param imageData The image data to check
     * @return The image type as a string (e.g., "png", "jpg"), or null if unknown
     */
    public static String detectImageType(byte[] imageData) {
        if (imageData == null || imageData.length < 4) {
            return null;
        }
        
        // Check for PNG
        if (imageData[0] == (byte) 0x89 && imageData[1] == 0x50 && 
            imageData[2] == 0x4E && imageData[3] == 0x47) {
            return "png";
        }
        
        // Check for JPEG
        if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8) {
            return "jpg";
        }
        
        return null;
    }
}

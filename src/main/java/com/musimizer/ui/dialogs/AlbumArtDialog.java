package com.musimizer.ui.dialogs;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Pos;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dialog for displaying album art in full size.
 */
public class AlbumArtDialog extends Dialog<Void> {
    private static final Logger LOGGER = Logger.getLogger(AlbumArtDialog.class.getName());
    private static final double SCREEN_SIZE_FACTOR = 0.8;
    private static final int DIALOG_PADDING = 40;
    
    private final ImageView fullSizeView = new ImageView();
    private Image originalImage;
    
    /**
     * Creates a new AlbumArtDialog with the specified owner and image data.
     *
     * @param owner The owner window of this dialog.
     * @param imageData The image data to display, or null if no image is available.
     */
    public AlbumArtDialog(Window owner, byte[] imageData) {
        initializeDialog(owner);
        loadImage(imageData);
    }
    
    private void initializeDialog(Window owner) {
        setTitle("Album Art");
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setResizable(false);
        
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        
        fullSizeView.setPreserveRatio(true);
        fullSizeView.setSmooth(true);
        
        StackPane content = new StackPane(fullSizeView);
        content.setPadding(new Insets(10));
        dialogPane.setContent(content);
    }
    
    private void positionCloseButton() {
        DialogPane dialogPane = getDialogPane();
        javafx.scene.Node closeButton = dialogPane.lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            // Get the right edge of the image (content area)
            double imageRightEdge = fullSizeView.getBoundsInParent().getMaxX() + fullSizeView.getLayoutX();
            
            // Get the width of the close button
            double buttonWidth = closeButton.getBoundsInLocal().getWidth();
            
            // Position the button so its right edge aligns with the image's right edge
            double buttonX = imageRightEdge - buttonWidth;
            
            // Apply the position
            closeButton.setLayoutX(buttonX);
            closeButton.setLayoutY(10); // 10px from top
        }
    }
    
    private void loadImage(byte[] imageData) {
        if (imageData == null) {
            LOGGER.fine("No image data provided to load");
            return;
        }
        
        try {
            loadAndScaleImage(imageData);
            resizeDialogToFitImage();
        } catch (Exception e) {
            handleImageLoadError(e);
        }
    }
    
    private void loadAndScaleImage(byte[] imageData) {
        originalImage = new Image(new ByteArrayInputStream(imageData));
        fullSizeView.setImage(originalImage);
        
        Screen screen = Screen.getPrimary();
        Rectangle2D screenBounds = screen.getVisualBounds();
        double maxHeight = screenBounds.getHeight() * SCREEN_SIZE_FACTOR;
        double maxWidth = Math.min(maxHeight, screenBounds.getWidth() * SCREEN_SIZE_FACTOR);
        
        if (needsScaling(maxWidth, maxHeight)) {
            scaleImageToFit(maxWidth, maxHeight);
        } else {
            setOriginalImageSize();
        }
    }
    
    private boolean needsScaling(double maxWidth, double maxHeight) {
        return originalImage.getWidth() > maxWidth || originalImage.getHeight() > maxHeight;
    }
    
    private void scaleImageToFit(double maxWidth, double maxHeight) {
        double scale = calculateOptimalScale(maxWidth, maxHeight);
        fullSizeView.setFitWidth(originalImage.getWidth() * scale);
        fullSizeView.setFitHeight(originalImage.getHeight() * scale);
    }
    
    private double calculateOptimalScale(double maxWidth, double maxHeight) {
        return Math.min(
            maxWidth / originalImage.getWidth(), 
            maxHeight / originalImage.getHeight()
        );
    }
    
    private void setOriginalImageSize() {
        fullSizeView.setFitWidth(originalImage.getWidth());
        fullSizeView.setFitHeight(originalImage.getHeight());
    }
    
    private void resizeDialogToFitImage() {
        DialogPane dialogPane = getDialogPane();
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double maxWidth = screenBounds.getWidth() * SCREEN_SIZE_FACTOR + DIALOG_PADDING;
        double maxHeight = screenBounds.getHeight() * SCREEN_SIZE_FACTOR + DIALOG_PADDING;
        
        // Use the scaled dimensions from the ImageView instead of the original image
        double targetWidth = Math.min(fullSizeView.getFitWidth() + DIALOG_PADDING, maxWidth);
        double targetHeight = Math.min(fullSizeView.getFitHeight() + DIALOG_PADDING, maxHeight);
        
        dialogPane.setPrefSize(targetWidth, targetHeight);

        // Position the dialog in the center of the screen
        javafx.stage.Window window = dialogPane.getScene().getWindow();
        window.setX((screenBounds.getWidth() - targetWidth) / 2);
        window.setY((screenBounds.getHeight() - targetHeight) / 2);

        // Position the close button after the dialog is shown
        positionCloseButton();
    }
    
    private void handleImageLoadError(Exception e) {
        String errorMsg = "Failed to load album art";
        LOGGER.log(Level.SEVERE, errorMsg, e);
        
        // Optionally show an error to the user
        DialogPane dialogPane = getDialogPane();
        dialogPane.setContentText("Could not load album art: " + e.getMessage());
    }
}

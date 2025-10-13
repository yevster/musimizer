package com.musimizer.ui.dialogs;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import javafx.stage.Window;

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
        
        // Add keyboard shortcut for copying (Ctrl+C or Cmd+C)
        getDialogPane().getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Check for Ctrl+C (Windows/Linux) or Cmd+C (Mac)
            KeyCombination copyCombination = new KeyCodeCombination(
                KeyCode.C, 
                KeyCombination.CONTROL_DOWN,
                KeyCombination.SHORTCUT_ANY
            );
            
            if (copyCombination.match(event)) {
                copyImageToClipboard();
                event.consume();
            }
        });
        
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        
        // Add Copy button
        ButtonType copyButtonType = new ButtonType("Copy");
        dialogPane.getButtonTypes().add(copyButtonType);
        
        fullSizeView.setPreserveRatio(true);
        fullSizeView.setSmooth(true);
        
        StackPane content = new StackPane(fullSizeView);
        content.setPadding(new Insets(10));
        dialogPane.setContent(content);
        
        // Set up the copy button action
        Button copyButton = (Button) dialogPane.lookupButton(copyButtonType);
        copyButton.setTooltip(new Tooltip("Copy image to clipboard"));
        
        // Prevent the dialog from closing when the Copy button is clicked
        Button closeButton = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            closeButton.setDefaultButton(true);
        }
        
        // Handle copy action without closing the dialog
        copyButton.setDefaultButton(false);
        copyButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            copyImageToClipboard();
            event.consume(); // Prevent the dialog from closing
        });
    }
    
    private void positionCloseButton() {
        DialogPane dialogPane = getDialogPane();
        javafx.scene.Node closeButton = dialogPane.lookupButton(ButtonType.CLOSE);
        javafx.scene.Node copyButton = dialogPane.getButtonTypes().stream()
            .filter(bt -> "Copy".equals(bt.getText()))
            .map(dialogPane::lookupButton)
            .findFirst()
            .orElse(null);
        
        if (closeButton != null && copyButton != null) {
            // Get the right edge of the image (content area)
            double imageRightEdge = fullSizeView.getBoundsInParent().getMaxX() + fullSizeView.getLayoutX();
            
            // Get the width of the buttons
            double closeButtonWidth = closeButton.getBoundsInLocal().getWidth();
            double copyButtonWidth = copyButton.getBoundsInLocal().getWidth();
            
            // Position the close button
            double closeButtonX = imageRightEdge - closeButtonWidth;
            closeButton.setLayoutX(closeButtonX);
            closeButton.setLayoutY(10); // 10px from top
            
            // Position the copy button to the left of the close button with some padding
            double copyButtonX = closeButtonX - copyButtonWidth - 10; // 10px padding between buttons
            copyButton.setLayoutX(copyButtonX);
            copyButton.setLayoutY(10);
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
    
    private void copyImageToClipboard() {
        if (originalImage != null) {
            try {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putImage(originalImage);
                clipboard.setContent(content);
                
                // Show feedback to user
                DialogPane dialogPane = getDialogPane();
                Button copyButton = (Button) dialogPane.getButtonTypes().stream()
                    .filter(bt -> "Copy".equals(bt.getText()))
                    .map(dialogPane::lookupButton)
                    .findFirst()
                    .orElse(null);
                    
                if (copyButton != null) {
                    String originalText = copyButton.getText();
                    copyButton.setText("Copied!");
                    copyButton.setDisable(true);
                    
                    // Reset button text after delay
                    new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                javafx.application.Platform.runLater(() -> {
                                    copyButton.setText(originalText);
                                    copyButton.setDisable(false);
                                });
                            }
                        },
                        1500 // 1.5 seconds
                    );
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to copy image to clipboard", e);
            }
        }
    }
    
    private void handleImageLoadError(Exception e) {
        String errorMsg = "Failed to load album art";
        LOGGER.log(Level.SEVERE, errorMsg, e);
        
        // Optionally show an error to the user
        DialogPane dialogPane = getDialogPane();
        dialogPane.setContentText("Could not load album art: " + e.getMessage());
    }
}

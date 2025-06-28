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

import java.io.ByteArrayInputStream;

public class AlbumArtDialog extends Dialog<Void> {
    
    private final ImageView fullSizeView;
    private Image originalImage;
    
    public AlbumArtDialog(Window owner, byte[] imageData) {
        setTitle("Album Art");
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setResizable(false);
        
        // Create dialog pane
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        
        // Create image view
        fullSizeView = new ImageView();
        fullSizeView.setPreserveRatio(true);
        fullSizeView.setSmooth(true);
        
        // Set up the dialog content
        StackPane content = new StackPane(fullSizeView);
        content.setPadding(new javafx.geometry.Insets(10));
        dialogPane.setContent(content);
        
        // Load the image
        loadImage(imageData);
    }
    
    private void loadImage(byte[] imageData) {
        if (imageData == null) return;
        
        try {
            // Create image from byte array
            originalImage = new Image(new ByteArrayInputStream(imageData));
            fullSizeView.setImage(originalImage);
            
            // Set initial size to fit screen if image is too large
            Screen screen = Screen.getPrimary();
            double maxHeight = screen.getVisualBounds().getHeight() * 0.8; // 80% of screen height
            double maxWidth = Math.min(maxHeight, screen.getVisualBounds().getWidth() * 0.8);
            
            if (originalImage.getWidth() > maxWidth || originalImage.getHeight() > maxHeight) {
                double scale = Math.min(maxWidth / originalImage.getWidth(), 
                                     maxHeight / originalImage.getHeight());
                fullSizeView.setFitWidth(originalImage.getWidth() * scale);
                fullSizeView.setFitHeight(originalImage.getHeight() * scale);
            } else {
                fullSizeView.setFitWidth(originalImage.getWidth());
                fullSizeView.setFitHeight(originalImage.getHeight());
            }
            
            // Set dialog size
            DialogPane dialogPane = getDialogPane();
            dialogPane.setPrefSize(
                Math.min(originalImage.getWidth() + 40, maxWidth + 40),
                Math.min(originalImage.getHeight() + 40, maxHeight + 40)
            );
            
      
            
        } catch (Exception e) {
            // Log error or show error message
            System.err.println("Failed to load album art: " + e.getMessage());
        }
    }
    

        

}

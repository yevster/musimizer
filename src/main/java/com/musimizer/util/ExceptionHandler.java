package com.musimizer.util;

import com.musimizer.exception.MusicDirectoryException;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionHandler {
    private static final Logger logger = Logger.getLogger(ExceptionHandler.class.getName());
    
    private ExceptionHandler() {
        // Private constructor to prevent instantiation
    }
    
    public static void handle(Throwable throwable, String context) {
        logger.log(Level.SEVERE, "Error in " + context, throwable);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("An error occurred");
            
            String contentText = throwable.getMessage();
            if (throwable instanceof MusicDirectoryException) {
                alert.setHeaderText("Music Directory Error");
            } else {
                contentText = "An unexpected error occurred: " + contentText;
            }
            
            alert.setContentText(contentText);
            alert.showAndWait();
        });
    }
    
    public static Optional<ButtonType> showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }
}

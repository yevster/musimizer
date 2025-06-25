package com.musimizer;

import com.musimizer.ui.AppUI;
import com.musimizer.util.ExceptionHandler;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Set up uncaught exception handler
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            ExceptionHandler.handle(throwable, "unhandled exception");
        });

        try {
            // Set application icon
            try {
                Image appIcon = new Image("icon.png");
                primaryStage.getIcons().add(appIcon);
            } catch (Exception e) {
                System.err.println("Could not set application icon: " + e.getMessage());
            }
            
            primaryStage.setTitle("Musimizer - Random Album Picker");
            
            // Create and configure the main UI
            BorderPane root = AppUI.createRootPane(primaryStage);
            
            // Set minimum window size
            primaryStage.setMinWidth(650);
            primaryStage.setMinHeight(400);
            
            Scene scene = new Scene(root, 800, 650);
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            // Handle any startup errors
            ExceptionHandler.handle(e, "starting application");
            // If we can't even show the error dialog, print to stderr
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        try {
            // Launch the JavaFX application
            launch(args);
        } catch (Exception e) {
            // Handle any errors that occur during application startup
            System.err.println("Fatal error during application startup:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

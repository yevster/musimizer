package com.musimizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Set application icon
        try {
            Image appIcon = new Image("icon.png");
            primaryStage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Could not set application icon: " + e.getMessage());
        }
        
        primaryStage.setTitle("Musimizer - Random Album Picker");
        BorderPane root = AppUI.createRootPane(primaryStage);
        
        // Set minimum window size
        primaryStage.setMinWidth(650);
        primaryStage.setMinHeight(400);
        
        Scene scene = new Scene(root, 800, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

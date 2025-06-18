package com.musimizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.util.Base64;

public class MainApp extends Application {
    // Base64-encoded 32x32 music note icon
    private static final String ICON_BASE64 = 
        "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAIqSURBVFiF7Zc9aBNRFMZ/7ya1H4hWq6hBcUjFwUFwU3BwcHARHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwUFBQUB0VQkFqoNk3eOQ7JpnmvSd5L2hQc+sPjvXeuef7zr3n3nthE5v4n0FqEe7du1cHjgHHgQPAXmAbsAVYBd4BL4FZ4LGq/lyvQjUJ7t69u11Vr4jIWeBQjfAF8EBVb4rIY1VdWY9CqQR37tzZq6q3gVPr4QdQVQO4qKr3axG4f/++UdWbwBUgqGvYx7Isi8gFVb1bJc8kCUTkMnC9iX1lqOo1Vb1dJpNIICJXgRtN7CqDql4VkZtFcokEIjIG3GpqUwXGReRW3kQmgYgMAw+BrU3tqoG3wDFV/ZQ2kUkgIheBOw3tqYt54ISqLqQnMh2tqjM0z/9GUNUZ4Hx2PJdAVW8BT+oY1hSq+lRVb2XHc3tAREaBZ8D2ZqZV4pc7dlT1c3qwKIGIyCjwuKFtVfHNHbvpwZIEInIaeNLMplp4DpxR1T/pwUIvqOo08LyJVTUhIs+B6exYGQER2Qa8AnY1sK0KvgAHVfVrdqLMC6jqT2CqhmFNMAX8yg6WEQBAVWeBp3UNq4qqzqrqbN5cJQIXgA91jKuBD8DFoslKBFT1FzAO/K5rXEX8BsZV9VfRhEoEAFR1HphoYFwVTKjqfNmCygQAVPU+8KiqcRXxCLhXZaGIVHwWx2V5CpxoYlwO5oBTqvqnbGHlE4jjQxGZBF43sS+F18CkqpY6H2oQAFDVX8BZ4FsT+wLfgbOq+r1sYW0CAKr6FjgHrDaxD1gFzqnquyoL1kUAQFVfAJeAvzXNBfgLXFLVF1UXrJsAgKrOARPA34rmrQITqjpXZ8OGCACo6hPgMvCvZOk/4LKqPq67R+0qLIOITAFXgZ05U9+Aa6o6XZe3MQIAIrINuA6MAzuAJeAhcF1Vf9Th/Qc4J2Jj2EwqUQAAAABJRU5ErkJggg==";

    @Override
    public void start(Stage primaryStage) {
        // Set application icon
        try {
            byte[] iconBytes = Base64.getDecoder().decode(ICON_BASE64);
            Image appIcon = new Image(new java.io.ByteArrayInputStream(iconBytes));
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

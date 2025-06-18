package com.musimizer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class AppController {
    private final Stage stage;
    private final ListView<String> albumListView;
    private final ObservableList<String> albumList;
    private AlbumPicker albumPicker;
    private String musicDir;
    private Path exclusionFile;

    public AppController(Stage stage, ListView<String> albumListView) {
        this.stage = stage;
        this.albumListView = albumListView;
        this.albumList = FXCollections.observableArrayList();
        albumListView.setItems(albumList);
        initialize();
    }

    private void initialize() {
        // Load settings
        musicDir = SettingsManager.getMusicDir();
        if (musicDir == null || musicDir.isEmpty()) {
            // Show settings dialog if music directory is not set
            showSettingsDialog();
        } else {
            // Initialize album picker
            initializeAlbumPicker();
        }
    }
    
    public void reinitializeWithNewSettings() {
        musicDir = SettingsManager.getMusicDir();
        if (musicDir != null && !musicDir.isEmpty()) {
            initializeAlbumPicker();
        }
    }
    
    private void initializeAlbumPicker() {
        if (musicDir == null || musicDir.isEmpty()) {
            return;
        }
        exclusionFile = AppController.getExclusionFilePath();
        albumPicker = new AlbumPicker(Paths.get(musicDir), exclusionFile);
        pickAlbums();
    }

    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(stage);
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(dir -> {
            if (dir != null && !dir.isEmpty()) {
                SettingsManager.setMusicDir(dir);
                musicDir = dir;
                initializeAlbumPicker();
            } else {
                Platform.exit();
            }
        });
        
        if (musicDir == null || musicDir.isEmpty()) {
            Platform.exit();
        }
    }

    public void pickAlbums() {
        List<String> picked = albumPicker.pickRandomAlbums(25);
        albumList.setAll(picked);
    }

    public void excludeAlbum(String albumPath) {
        albumPicker.excludeAlbum(albumPath);
        pickAlbums();
    }

    // Exclude album without refreshing the entire list
    public void excludeAlbumNoRefresh(String albumPath) {
        albumPicker.excludeAlbum(albumPath);
    }

    public static Path getExclusionFilePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Paths.get(appData != null ? appData : userHome, "musimizer", "excluded_albums.txt");
        } else if (os.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", "musimizer", "excluded_albums.txt");
        } else {
            return Paths.get(userHome, ".config", "musimizer", "excluded_albums.txt");
        }
    }
}

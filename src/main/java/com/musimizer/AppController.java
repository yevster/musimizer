package com.musimizer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

public class AppController {
    private final Stage stage;
    private final ObservableList<String> albumList;
    private final Button pickButton;
    private final Button backButton;
    private final Label titleLabel;
    private AlbumPicker albumPicker;
    private String musicDir;
    private Path exclusionFile;
    private List<String> currentRandomPicks;
    private static final String DEFAULT_TITLE = "Randomly Selected Albums";

    public AppController(Stage primaryStage, ListView<String> albumListView, Button pickButton, Button backButton, Label titleLabel) {
        this.stage = primaryStage;
        this.albumList = FXCollections.observableArrayList();
        this.pickButton = pickButton;
        this.backButton = backButton;
        this.titleLabel = titleLabel;
        this.currentRandomPicks = new ArrayList<>();
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
        
        try {
            exclusionFile = AppController.getExclusionFilePath();
            albumPicker = new AlbumPicker(Paths.get(musicDir), exclusionFile);
            albumPicker.loadSavedPicks();
            
            if (albumPicker.getCurrentPicks().isEmpty()) {
                try {
                    albumPicker.generateNewPicks(25);
                } catch (IllegalStateException e) {
                    // Show error but don't show settings dialog yet to avoid loops
                    AppUI.showError("Error Initializing", "Could not generate initial album picks", e.getMessage());
                }
            }
            
            albumList.setAll(albumPicker.getCurrentPicks());
            titleLabel.setText(DEFAULT_TITLE);
            
        } catch (Exception e) {
            AppUI.showError("Initialization Error", "Failed to initialize album picker", e.getMessage());
        }
    }

    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(stage);
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(settings -> {
            if (settings != null && settings.musicDir != null && !settings.musicDir.isEmpty()) {
                // Save the music directory
                SettingsManager.setMusicDir(settings.musicDir);
                musicDir = settings.musicDir;
                
                // Save the settings
                SettingsManager.setNumberOfPicks(settings.numberOfPicks);
                SettingsManager.setNumberOfSearchResults(settings.numberOfSearchResults);
                
                // Reinitialize with new settings
                initializeAlbumPicker();
            } else {
                // If no valid settings and no existing music directory, exit
                if (musicDir == null || musicDir.isEmpty()) {
                    Platform.exit();
                }
            }
        });
        
        // If we still don't have a music directory after the dialog, exit
        if (musicDir == null || musicDir.isEmpty()) {
            Platform.exit();
        }
    }

    public void excludeAlbum(String albumPath) {
        albumPicker.excludeAlbum(albumPath);
        // Don't call pickAlbums() here as it would load the saved picks
        // Instead, just remove the excluded album from the current view
        albumList.remove(albumPath);
    }

    public void showSearchResults(List<String> searchTerms, String keywords) {
        if (searchTerms == null || searchTerms.isEmpty() || albumPicker == null) {
            return;
        }
        
        try {
            // Save current random picks if not already in search mode
            if (pickButton.isVisible()) {
                currentRandomPicks = new ArrayList<>(albumList);
            }
            
            // Get all albums that match the search terms, limited by search results setting
            List<String> matchingAlbums = albumPicker.searchAlbums(searchTerms, SettingsManager.getNumberOfSearchResults());
            
            // Update the UI with search results
            Platform.runLater(() -> {
                albumList.setAll(matchingAlbums);
                pickButton.setVisible(false);
                backButton.setVisible(true);
                titleLabel.setText("Search Results for: " + keywords);
            });
            
        } catch (Exception e) {
            Platform.runLater(() -> 
                AppUI.showError("Search Error", "Error searching albums", e.getMessage())
            );
        }
    }
    
    public void showRandomPicks() {
        Platform.runLater(() -> {
            if (!currentRandomPicks.isEmpty()) {
                albumList.setAll(currentRandomPicks);
            } else {
                pickAlbums();
            }
            pickButton.setVisible(true);
            backButton.setVisible(false);
            titleLabel.setText(DEFAULT_TITLE);
        });
    }
    
    public void pickAlbums() {
        try {
            int numPicks = SettingsManager.getNumberOfPicks();
            albumPicker.generateNewPicks(numPicks);
            currentRandomPicks = new ArrayList<>(albumPicker.getCurrentPicks());
            albumList.setAll(currentRandomPicks);
            pickButton.setVisible(true);
            backButton.setVisible(false);
            titleLabel.setText(DEFAULT_TITLE);
        } catch (IllegalStateException e) {
            // Show error message and prompt for settings if music directory is invalid
            boolean openSettings = AppUI.showErrorWithOptions("Music Directory Error", 
                "Error generating album picks", 
                e.getMessage() + "\n\nWould you like to open settings to correct this?");
            
            if (openSettings) {
                showSettingsDialog();
            }
        }
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

package com.musimizer.controller;

import com.musimizer.service.AlbumService;
import com.musimizer.service.PlaybackService;
import com.musimizer.repository.AlbumRepository;
import com.musimizer.repository.FileAlbumRepository;
import com.musimizer.ui.dialogs.SettingsDialog;
import com.musimizer.util.ExceptionHandler;
import com.musimizer.util.SettingsManager;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AppController {
    private final Stage stage;
    private final AlbumService albumService;
    private final PlaybackService playbackService;
    private final ListView<Path> albumListView;
    private final Button pickButton;
    private final Button backButton;
    private final Label titleLabel;
    private static final String DEFAULT_TITLE = "Randomly Selected Albums";

    public AppController(Stage primaryStage, ListView<Path> albumListView, Button pickButton, Button backButton, Label titleLabel) {
        this.stage = primaryStage;
        this.albumListView = albumListView;
        this.pickButton = pickButton;
        this.backButton = backButton;
        this.titleLabel = titleLabel;
        
        // Initialize services
        Path musicDir = Paths.get(SettingsManager.getMusicDir());
        Path exclusionFile = SettingsManager.getExclusionFilePath();
        AlbumRepository albumRepository = new FileAlbumRepository();
        this.albumService = new AlbumService(albumRepository, musicDir, exclusionFile);
        this.playbackService = new PlaybackService();
        

    }

    public void initialize() {
        String musicDir = SettingsManager.getMusicDir();
        if (musicDir == null || musicDir.isEmpty()) {
            Platform.runLater(this::showSettingsDialog);
        } else {
            initializeAlbumPicker();
        }
    }
    
    public void reinitializeWithNewSettings() {
        String musicDir = SettingsManager.getMusicDir();
        if (musicDir != null && !musicDir.isEmpty()) {
            initializeAlbumPicker();
        }
    }

    public AlbumService getAlbumService() {
        return albumService;
    }
    
    private void initializeAlbumPicker() {
        try {
            albumService.loadExcludedAlbums();
            albumService.loadSavedPicks();
            
            if (albumService.getCurrentPicks().isEmpty()) {
                albumService.generateNewPicks(SettingsManager.getNumberOfPicks());
            }
            
            updateAlbumList(albumService.getCurrentPicks());
            titleLabel.setText(DEFAULT_TITLE);
            
        } catch (Exception e) {
            ExceptionHandler.handle(e, "initializing album picker");
        }
    }

    /**
     * Shows the settings dialog and updates the application settings if valid settings are provided.
     * This method is public to allow access from UI components.
     */
    public void showSettingsDialog() {
        try {
            SettingsDialog dialog = new SettingsDialog(stage);
            Optional<SettingsDialog.Settings> result = dialog.showAndWait();
            if (result.isPresent()) {
                SettingsDialog.Settings settings = result.get();
                boolean needsReinitialization = false;
                
                // Update music directory if changed
                if (settings.musicDir != null && !settings.musicDir.isEmpty() 
                        && !settings.musicDir.equals(SettingsManager.getMusicDir())) {
                    SettingsManager.setMusicDir(settings.musicDir);
                    needsReinitialization = true;
                }
                
                // Update number of picks if changed
                if (settings.numberOfPicks != SettingsManager.getNumberOfPicks()) {
                    SettingsManager.setNumberOfPicks(settings.numberOfPicks);
                }
                
                // Update number of search results if changed
                if (settings.numberOfSearchResults != SettingsManager.getNumberOfSearchResults()) {
                    SettingsManager.setNumberOfSearchResults(settings.numberOfSearchResults);
                }
                
                // Reinitialize if music directory was changed
                if (needsReinitialization) {
                    initializeAlbumPicker();
                }
            }
            
            // Exit if no music directory is set
            if (SettingsManager.getMusicDir() == null || SettingsManager.getMusicDir().isEmpty()) {
                Platform.exit();
            }
        } catch (Exception e) {
            ExceptionHandler.handle(e, "Error showing settings dialog");
        }
    }

    public void excludeAlbum(Path albumPath) {
        try {
            albumService.excludeAlbum(albumPath);
        } catch (Exception e) {
            ExceptionHandler.handle(e, "excluding album");
        }
    }

    public void showSearchResults(List<String> searchTerms, String keywords) {
        if (searchTerms == null || searchTerms.isEmpty()) {
            return;
        }
        
        try {
            List<Path> searchResults = albumService.searchAlbums(searchTerms, SettingsManager.getNumberOfSearchResults());
            updateAlbumList(searchResults);
            pickButton.setVisible(false);
            backButton.setVisible(true);
            titleLabel.setText((searchResults.isEmpty() ? "No " : "") + "Search Results for: " + keywords);
        } catch (Exception e) {
            ExceptionHandler.handle(e, "searching albums");
        }
    }
    
    public void showRandomPicks() {
        Platform.runLater(() -> {
            updateAlbumList(albumService.getCurrentPicks());
            pickButton.setVisible(true);
            backButton.setVisible(false);
            titleLabel.setText(DEFAULT_TITLE);
        });
    }
    
    public void pickAlbums() {
        try {
            albumService.generateNewPicks(SettingsManager.getNumberOfPicks());
            updateAlbumList(albumService.getCurrentPicks());
            pickButton.setVisible(true);
            backButton.setVisible(false);
            titleLabel.setText(DEFAULT_TITLE);
        } catch (Exception e) {
            boolean openSettings = ExceptionHandler.showConfirmation(
                "Music Directory Error",
                "Error generating album picks",
                e.getMessage() + "\n\nWould you like to open settings to correct this?"
            ).filter(buttonType -> buttonType == ButtonType.OK).isPresent();
            
            if (openSettings) {
                showSettingsDialog();
            }
        }
    }
    
    public void playAlbum(Path albumPath) {
        try {
            playbackService.playAlbum(albumPath);
        } catch (Exception e) {
            ExceptionHandler.handle(e, "playing album");
        }
    }

    public String albumPathToDisplayString(Path albumPath) {
        return albumService.albumPathToDisplayString(albumPath);
    }

    public void updateAlbumList(List<Path> albums) {
        albumListView.getItems().setAll(albums);
    }
}

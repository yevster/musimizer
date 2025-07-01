package com.musimizer.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.musimizer.service.AlbumService;
import com.musimizer.service.PlaybackService;
import com.musimizer.repository.AlbumRepository;
import com.musimizer.ui.dialogs.SettingsDialog;
import com.musimizer.util.ExceptionHandler;
import com.musimizer.util.SettingsManager;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

public class AppController {
    private final Stage stage;
    private AlbumService albumService;
    private final PlaybackService playbackService;
    private final ListView<Path> albumListView;
    private final Button pickButton;
    private final Button backButton;
    private final Label titleLabel;
    private static final String DEFAULT_TITLE = "Randomly Selected Albums";
    private static final String BOOKMARKS_TITLE = "Bookmarked Albums";
    
    private enum ViewMode {
        RANDOM,
        BOOKMARKS,
        SEARCH_RESULTS
    }
    
    private ViewMode currentView = ViewMode.RANDOM;

    public AppController(Stage primaryStage, ListView<Path> albumListView, Button pickButton, Button backButton,
            Label titleLabel) {
        this.stage = primaryStage;
        this.albumListView = albumListView;
        this.pickButton = pickButton;
        this.backButton = backButton;
        this.titleLabel = titleLabel;

        this.playbackService = new PlaybackService();

    }

    public void initialize() {
        String musicDir = SettingsManager.getMusicDir();
        if (musicDir == null || musicDir.isEmpty()) {
            Platform.runLater(this::showSettingsDialog);
        } else {
            initializeWithSettings();
        }
    }

    public void reinitializeWithNewSettings() {
        String musicDir = SettingsManager.getMusicDir();
        if (musicDir != null && !musicDir.isEmpty()) {
            initializeWithSettings();
        }
    }

    public AlbumService getAlbumService() {
        return albumService;
    }

    private void initializeWithSettings() {
        try {
            AlbumRepository albumRepository = new com.musimizer.repository.FileAlbumRepository();
            albumService = new AlbumService(albumRepository, Paths.get(SettingsManager.getMusicDir()),
                    SettingsManager.getExclusionFilePath());
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
     * Shows the settings dialog and updates the application settings if valid
     * settings are provided.
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
                    initializeWithSettings();
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
        if (searchTerms == null || searchTerms.isEmpty())
            return;

        try {
            List<Path> searchResults = albumService.searchAlbums(searchTerms,
                    SettingsManager.getNumberOfSearchResults());
            updateAlbumList(searchResults);
            pickButton.setVisible(false);
            backButton.setVisible(true);
            currentView = ViewMode.SEARCH_RESULTS;
            titleLabel.setText((searchResults.isEmpty() ? "No " : "") + "Search Results for: " + keywords);
        } catch (Exception e) {
            ExceptionHandler.handle(e, "searching albums");
        }
    }

    public void showRandomPicks() {
        Platform.runLater(() -> {
            try {
                updateAlbumList(albumService.getCurrentPicks());
                pickButton.setVisible(true);
                backButton.setVisible(false);
                currentView = ViewMode.RANDOM;
                titleLabel.setText(DEFAULT_TITLE);
            } catch (Exception e) {
                ExceptionHandler.handle(e, "showing random picks");
            }
        });
    }
    
    public void showBookmarks() {
        Platform.runLater(() -> {
            try {
                var bookmarks = albumService.getBookmarkedAlbums();
                updateAlbumList(bookmarks);
                pickButton.setVisible(false);
                backButton.setVisible(true);
                currentView = ViewMode.BOOKMARKS;
                titleLabel.setText(bookmarks.isEmpty() ? "No Bookmarks." : BOOKMARKS_TITLE);
            } catch (Exception e) {
                ExceptionHandler.handle(e, "loading bookmarks");
            }
        });
    }
    
    public void toggleBookmark(Path albumPath) {
        try {
            albumService.toggleBookmark(albumPath);
            if (currentView == ViewMode.BOOKMARKS) {
                showBookmarks();
            }
        } catch (Exception e) {
            ExceptionHandler.handle(e, "toggling bookmark");
        }
    }
    
    public boolean isShowingBookmarks() {
        return currentView == ViewMode.BOOKMARKS;
    }
    
    public boolean isShowingSearchResults() {
        return currentView == ViewMode.SEARCH_RESULTS;
    }
    
    public boolean isShowingRandomPicks() {
        return currentView == ViewMode.RANDOM;
    }
    
    public boolean isBookmarked(Path albumPath) {
        return albumService.isBookmarked(albumPath);
    }
    public void pickAlbums() {
        try {
            albumService.generateNewPicks(SettingsManager.getNumberOfPicks());
            updateAlbumList(albumService.getCurrentPicks());
            currentView = ViewMode.RANDOM;
            titleLabel.setText(DEFAULT_TITLE);
        } catch (Exception e) {
            Optional<ButtonType> result = ExceptionHandler.showConfirmation(
                "Error Generating Picks",
                "Failed to generate new album picks",
                "Would you like to check your settings?");
            if (result.isPresent() && result.get() == ButtonType.OK) {
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
    
    public void updateAlbumList(Collection<Path> albums) {
        albumListView.getItems().setAll(albums);
    }
}

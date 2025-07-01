package com.musimizer.ui;

import com.musimizer.controller.AppController;
import com.musimizer.service.AlbumService;
import com.musimizer.util.AudioMetadataRetriever;
import com.musimizer.util.ExceptionHandler;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.musimizer.ui.dialogs.AlbumArtDialog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppUI {
    private static final Logger LOGGER = Logger.getLogger(AppUI.class.getName());

    public static BorderPane createRootPane(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // Main content
        ListView<Path> albumListView = createAlbumListView();

        // Create menu
        MenuBar menuBar = createMenuBar(albumListView, primaryStage);

        // Buttons
        Button pickButton = createPickButton();
        Button backButton = createBackButton();
        HBox buttonBox = new HBox(10, pickButton, backButton);

        // Main content area with title and album list
        Label titleLabel = new Label("Randomly Selected Albums");
        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));
        vbox.getChildren().addAll(titleLabel, albumListView, buttonBox);
        VBox.setVgrow(albumListView, Priority.ALWAYS);
        root.setTop(menuBar);
        root.setCenter(vbox);

        // Initialize controller
        AppController controller = new AppController(primaryStage, albumListView, pickButton, backButton, titleLabel);
        root.setUserData(controller);

        // Set up button actions
        pickButton.setOnAction(e -> controller.pickAlbums());
        backButton.setOnAction(e -> controller.showRandomPicks());

        // Add key handler to the scene
        setupEscapeKeyHandler(primaryStage, controller);

        return root;
    }

    private static ListView<Path> createAlbumListView() {
        ListView<Path> albumListView = new ListView<>();
        albumListView.setCellFactory(lv -> new AlbumListCell(albumListView));
        return albumListView;
    }

    private static MenuBar createMenuBar(ListView<Path> albumList, Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem settingsItem = new MenuItem("Settings...");
        settingsItem.setOnAction(e -> showSettings(primaryStage));

        MenuItem showExclusionsItem = new MenuItem("Show Exclusion List...");
        showExclusionsItem.setOnAction(e -> showExclusionList(primaryStage));
        fileMenu.getItems().addAll(settingsItem, showExclusionsItem);

        // Edit menu
        Menu editMenu = new Menu("Edit");
        MenuItem findItem = new MenuItem("Find...");
        findItem.setAccelerator(KeyCombination.keyCombination("shortcut+F"));
        findItem.setOnAction(e -> showFindDialog(albumList,
                (AppController) ((BorderPane) primaryStage.getScene().getRoot()).getUserData()));
        editMenu.getItems().add(findItem);
        
        // View menu
        Menu viewMenu = new Menu("View");
        MenuItem bookmarksItem = new MenuItem("Bookmarks");
        bookmarksItem.setAccelerator(KeyCombination.keyCombination("shortcut+B"));
        bookmarksItem.setOnAction(e -> {
            AppController controller = (AppController) ((BorderPane) primaryStage.getScene().getRoot()).getUserData();
            controller.showBookmarks();
        });
        viewMenu.getItems().add(bookmarksItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu);
        return menuBar;
    }

    private static Button createPickButton() {
        Button button = new Button("Pick New Random Selections");
        button.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        return button;
    }

    private static Button createBackButton() {
        Button button = new Button("Back to Random Picks");
        button.setStyle("-fx-base: #FF9800; -fx-text-fill: white;");
        button.setVisible(false);
        return button;
    }

    private static void setupEscapeKeyHandler(Stage primaryStage, AppController controller) {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            primaryStage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    addEscapeHandler(newScene, controller);
                    addKeyboardShortcuts(newScene, controller);
                }
            });
        } else {
            addEscapeHandler(scene, controller);
            addKeyboardShortcuts(scene, controller);
        }
    }
    
    private static void addKeyboardShortcuts(Scene scene, AppController controller) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            // Handle ESC key to go back to random picks from bookmarks or search results
            if (event.getCode() == KeyCode.ESCAPE && 
                (controller.isShowingBookmarks() || controller.isShowingSearchResults())) {
                controller.showRandomPicks();
                event.consume();
            }
            // Handle CTRL+B to show bookmarks
            else if (event.isControlDown() && event.getCode() == KeyCode.B) {
                controller.showBookmarks();
                event.consume();
            }
        });
    }

    private static void addEscapeHandler(Scene scene, AppController controller) {
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && 
                (controller.isShowingBookmarks() || controller.isShowingSearchResults())) {
                controller.showRandomPicks();
                event.consume();
            }
        });
    }

    private static void showSettings(Stage owner) {
        AppController controller = (AppController) ((BorderPane) owner.getScene().getRoot()).getUserData();
        controller.showSettingsDialog();
    }

    private static void showExclusionList(Stage owner) {
        Path exclusionFile = com.musimizer.util.SettingsManager.getExclusionFilePath();
        if (exclusionFile != null) {
            try {
                com.musimizer.util.SettingsManager.ensureExclusionFileExists();
            } catch (IOException e) {
                showError("Error", "Cannot ensure file exists",
                        "Failed to ensure exclusion file exists: " + e.getMessage());
                return; // Exit if file creation fails
            }

            try {
                java.awt.Desktop.getDesktop().open(exclusionFile.toFile());
            } catch (java.awt.HeadlessException e) {
                showError("Error", "Cannot open file", "Desktop operations not supported in this environment.");
                LOGGER.log(Level.WARNING, "Headless environment, cannot open exclusion file", e);
            } catch (IOException e) {
                showError("Error", "Cannot open file", "Failed to open exclusion file: " + e.getMessage());
                LOGGER.log(Level.SEVERE, "Failed to open exclusion file", e);
            }
        } else {
            showError("Error", "File not found", "Exclusion file does not exist or path is invalid.");
        }
    }

    private static void showFindDialog(ListView<Path> albumList, AppController controller) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Find Albums");
        dialog.setHeaderText("Enter search terms:");
        dialog.setContentText("Search:");
        dialog.setGraphic(null);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(terms -> {
            List<String> searchTerms = Arrays.asList(terms.trim().split("\\s+"));
            controller.showSearchResults(searchTerms, terms);
        });
    }

    public static void showError(String title, String header, String content) {
        LOGGER.log(Level.SEVERE, title + ": " + content);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static boolean showErrorWithOptions(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static class AlbumListCell extends ListCell<Path> {
        private static final Logger LOGGER = Logger.getLogger(AlbumListCell.class.getName());
        private static final ExecutorService executorService = Executors.newCachedThreadPool();
        private static final Image DEFAULT_ALBUM_ART = createDefaultAlbumArt();

        private final ImageView albumArtView = new ImageView(DEFAULT_ALBUM_ART);
        private final Label albumLabel = new Label();
        private byte[] currentAlbumArtData;
        private final Button excludeButton = new Button();
        private final Button playButton = new Button();
        private final Button folderButton = new Button();
        private final Button bookmarkButton = new Button();
        private final HBox hbox = new HBox();
        private AppController controller;

        private static Image createDefaultAlbumArt() {
            // Create a simple default album art
            Rectangle rect = new Rectangle(40, 40);
            rect.setFill(Color.DARKGRAY);

            return rect.snapshot(null, null);
        }

        public AlbumListCell(ListView<Path> albumListView) {
            super();
            setupCell(albumListView);
        }

        @Override
        protected void updateItem(Path item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                if (controller == null) {
                    controller = (AppController) getScene().getRoot().getUserData();
                }
                albumLabel.setText(controller.albumPathToDisplayString(item));
                loadAlbumArt(item);
                updateBookmarkButton(item);
                setGraphic(hbox);
                setText(null); // Important: set text to null if using graphic

                // Make sure buttons are visible when an item is present
                folderButton.setVisible(true);
                excludeButton.setVisible(true);
                playButton.setVisible(true);
                bookmarkButton.setVisible(true);
            }
        }

        private void showFullSizeAlbumArt(byte[] imageData) {
            if (imageData == null)
                return;

            try {
                // Create and show the album art dialog
                AlbumArtDialog dialog = new AlbumArtDialog(
                        getScene().getWindow(),
                        imageData);
                dialog.showAndWait();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to display full size album art", e);
            }
        }

        private void handleAlbumArtClick(MouseEvent event) {
            if (currentAlbumArtData != null) {
                showFullSizeAlbumArt(currentAlbumArtData);
            }
        }

        private void loadAlbumArt(Path albumPath) {
            LOGGER.fine("Loading album art for: " + albumPath);
            // Reset to default art while loading
            albumArtView.setImage(DEFAULT_ALBUM_ART);

            // Load in background to avoid UI freezing
            executorService.submit(() -> {
                try {
                    AlbumService albumService = controller.getAlbumService();
                    LOGGER.fine("Finding first audio file in: " + albumPath);
                    Optional<Path> audioFileOpt = albumService.findFirstAudioFile(albumPath);

                    if (!audioFileOpt.isPresent()) {
                        LOGGER.fine("No audio files found in: " + albumPath);
                        return;
                    }

                    Path audioFile = audioFileOpt.get();
                    LOGGER.fine("Found audio file: " + audioFile);
                    
                    try {
                        LOGGER.fine("Attempting to extract cover image from: " + audioFile);
                        byte[] imageData = AudioMetadataRetriever.getCoverImage(audioFile);
                        LOGGER.fine("Cover image extraction " + (imageData != null ? "succeeded" : "failed"));

                        if (imageData == null) 
                            return;
                        
                        currentAlbumArtData = imageData; // Store the image data for full-size view
                        LOGGER.fine("Cover image size: " + imageData.length + " bytes");

                        // Create and update the image on the JavaFX Application Thread
                        javafx.application.Platform.runLater(() -> {
                            try {
                                LOGGER.fine("Creating JavaFX Image from byte array");
                                Image img = ImageUtils.createImageFromBytes(imageData, 40, 40);
                                
                                if (img != null) {
                                    LOGGER.fine("Successfully created image, dimensions: " +
                                            img.getWidth() + "x" + img.getHeight());
                                    albumArtView.setImage(img);
                                } else 
                                    throw new IOException("Failed to create image from byte array");
                                
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Failed to create/display album art: " + e.getMessage(), e);
                                albumArtView.setImage(DEFAULT_ALBUM_ART);
                            }
                        });
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error extracting album art from: " + audioFile, e);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error finding audio file in: " + albumPath, e);
                }
            });
        }

        private void setupCell(ListView<Path> albumListView) {
            setupAlbumArtView();

            // Add some padding and spacing
            hbox.setSpacing(10);
            hbox.setPadding(new javafx.geometry.Insets(2,15,2,2));

            // Add all components to the HBox
            hbox.getChildren().addAll(albumArtView, albumLabel, folderButton, excludeButton, playButton, bookmarkButton);

            // Configure layout
            HBox.setHgrow(albumLabel, Priority.ALWAYS);
            albumLabel.setMaxWidth(Double.MAX_VALUE);
            hbox.setFillHeight(true);
            hbox.setStyle("-fx-alignment: center-left;");

            // Setup buttons
            setupFolderButton();
            setupExcludeButton(albumListView);
            setupPlayButton();
            setupBookmarkButton(albumListView);

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setGraphic(hbox);
        }

        private void setupAlbumArtView() {
            // Configure album art view
            albumArtView.setFitWidth(40);
            albumArtView.setFitHeight(40);
            albumArtView.setPreserveRatio(true);
            albumArtView.getStyleClass().add("clickable");
            albumArtView.setOnMouseClicked(this::handleAlbumArtClick);
            albumArtView.setStyle(" -fx-cursor: hand;");

        }

        private void setupFolderButton() {
            SVGPath folderIcon = new SVGPath();
            folderIcon.setContent(
                    "M3 7V5a2 2 0 0 1 2-2h3.17a2 2 0 0 1 1.41.59l1.83 1.82H19a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7zm2 0h14v8H5V7z");
            folderIcon.setStyle("-fx-fill: #d5bd1f;");
            folderIcon.setScaleX(1.0);
            folderIcon.setScaleY(1.0);
            folderButton.setGraphic(folderIcon);
            folderButton.setPrefSize(20, 20);
            folderButton
                    .setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand; -fx-opacity: 0.7;");
            folderButton.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                folderButton.setStyle(
                        "-fx-background-color: " + (isNowHovered ? "#ffeeee;" : "transparent;") +
                                " -fx-padding: 0;" +
                                " -fx-cursor: hand;" +
                                " -fx-opacity: " + (isNowHovered ? "1.0;" : "0.7;"));
            });
            folderButton.setTooltip(new Tooltip("Open album folder"));

            folderButton.setOnAction(e -> {
                Path albumPath = getItem();
                if (albumPath != null) {
                    try {
                        openAlbumFolder(albumPath);
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "Error opening album folder", ex);
                        showError("Error", "Could not open folder",
                                "Error opening album folder: " + ex.getMessage());
                    }
                }
            });
        }

        private void updateBookmarkButton(Path albumPath) {
            if (controller == null) {
                controller = (AppController) getScene().getRoot().getUserData();
            }
            boolean isBookmarked = controller.isBookmarked(albumPath);
            
            SVGPath bookmarkIcon = new SVGPath();
            if (isBookmarked) {
                // Filled bookmark icon
                bookmarkIcon.setContent("M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z");
                bookmarkIcon.setStyle("-fx-fill: #DC9F30;");
            } else {
                // Outline bookmark icon
                bookmarkIcon.setContent("M17 18l-5-2.18L7 18V5h10v13zm0-15H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z");
                bookmarkIcon.setStyle("-fx-fill: #757575;");
            }
            bookmarkIcon.setScaleX(0.7);
            bookmarkIcon.setScaleY(0.7);
            bookmarkButton.setGraphic(bookmarkIcon);
            bookmarkButton.setTooltip(new Tooltip(isBookmarked ? "Remove bookmark" : "Add bookmark"));
        }

        private void setupBookmarkButton(ListView<Path> albumListView) {
            bookmarkButton.setPrefSize(20, 20);
            bookmarkButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand; -fx-opacity: 0.7;");
            
            bookmarkButton.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                bookmarkButton.setStyle(
                    "-fx-background-color: " + (isNowHovered ? "#e8f5e9;" : "transparent;") +
                    " -fx-padding: 0;" +
                    " -fx-cursor: hand;" +
                    " -fx-opacity: " + (isNowHovered ? "1.0;" : "0.7;"));
            });
            
            bookmarkButton.setTooltip(new Tooltip("Bookmark this album"));
            
            bookmarkButton.setOnAction(e -> {
                Path albumPath = getItem();
                if (albumPath != null) {
                    AppController controller = (AppController) getScene().getRoot().getUserData();
                    controller.toggleBookmark(albumPath);
                    // Update the button appearance after toggling
                    updateBookmarkButton(albumPath);
                }
            });
        }

        private void setupExcludeButton(ListView<Path> albumListView) {
            SVGPath noEntryIcon = new SVGPath();
            noEntryIcon.setContent(
                    "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" +
                            "M5 5l14 14");
            noEntryIcon.setStyle(
                    "-fx-fill: #ffffff;" +
                            "-fx-stroke: #ff0000;" +
                            "-fx-stroke-width: 3.5;" +
                            "-fx-stroke-linecap: round;");
            noEntryIcon.setScaleX(0.7);
            noEntryIcon.setScaleY(0.7);
            excludeButton.setGraphic(noEntryIcon);
            excludeButton.setPrefSize(20, 20);
            excludeButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-opacity: 0.7;");

            excludeButton.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                excludeButton.setStyle(
                        "-fx-background-color: " + (isNowHovered ? "#ffeeee;" : "transparent;") +
                                " -fx-padding: 2;" +
                                " -fx-cursor: hand;" +
                                " -fx-opacity: " + (isNowHovered ? "1.0;" : "0.7;"));
            });
            excludeButton.setTooltip(new Tooltip("Exclude this album"));
            excludeButton.setOnAction(e -> {
                Path albumPath = getItem();
                if (albumPath == null)
                    return;
                AppController controller = (AppController) getScene().getRoot().getUserData();
                controller.excludeAlbum(albumPath);
                // Remove only this album from the list viewAdd commentMore actions
                albumListView.getItems().remove(albumPath);
            });
        }

        private void setupPlayButton() {
            SVGPath playIcon = new SVGPath();
            playIcon.setContent("M8 5v14l11-7z"); // SVG path for a play triangle
            playIcon.setStyle("-fx-fill: green;");
            playIcon.setScaleX(1.0);
            playIcon.setScaleY(1.0);
            playButton.setGraphic(playIcon);
            playButton.setPrefSize(20, 20);
            playButton
                    .setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand; -fx-opacity: 0.7;");
            playButton.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                playButton.setStyle(
                        "-fx-background-color: " + (isNowHovered ? "#deffee;" : "transparent;") +
                                " -fx-padding: 0;" +
                                " -fx-cursor: hand;" +
                                " -fx-opacity: " + (isNowHovered ? "1.0;" : "0.7;"));
            });
            playButton.setTooltip(new Tooltip("Play album"));

            playButton.setOnAction(e -> {
                Path albumPath = getItem();
                if (albumPath != null) {
                    AppController controller = (AppController) getScene().getRoot().getUserData();
                    if (controller != null) {
                        controller.playAlbum(albumPath);
                    }
                }
            });
        }

        private void openAlbumFolder(Path albumPath) throws IOException {
            Path dir = albumPath.toAbsolutePath();
            if (Files.isDirectory(dir)) {
                try {
                    java.awt.Desktop.getDesktop().open(dir.toFile());
                } catch (java.awt.HeadlessException e) {
                    LOGGER.log(Level.WARNING, "Headless environment, cannot open folder", e);
                    throw new IOException("Desktop operations not supported in headless environment");
                }
            } else {
                throw new IOException("Album directory does not exist: " + dir);
            }
        }

    }
}

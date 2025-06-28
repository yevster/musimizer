package com.musimizer.ui;

import com.musimizer.controller.AppController;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Arrays;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.musimizer.util.AlbumArtExtractor;

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
        albumListView.setFixedCellSize(40);
        albumListView.setStyle("-fx-cell-size: 40px;");
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
        findItem.setOnAction(e -> showFindDialog(albumList, (AppController) ((BorderPane) primaryStage.getScene().getRoot()).getUserData()));
        editMenu.getItems().add(findItem);
        
        menuBar.getMenus().addAll(fileMenu, editMenu);
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
    
    private static void setupEscapeKeyHandler(Stage stage, AppController controller) {
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    addEscapeHandler(newScene, controller);
                }
            });
        } else {
            addEscapeHandler(scene, controller);
        }
    }
    
    private static void addEscapeHandler(Scene scene, AppController controller) {
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                controller.showRandomPicks();
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
                showError("Error", "Cannot ensure file exists", "Failed to ensure exclusion file exists: " + e.getMessage());
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
        private static final int IMAGE_SIZE = 36; // Slightly smaller than row height for padding
        private static final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "AlbumArtLoader");
            t.setDaemon(true);
            return t;
        });
        
        private final ImageView albumArtView = new ImageView();
        private final Label albumLabel = new Label();
        private final Button excludeButton = new Button();
        private final Button folderButton = new Button();
        private final Button playButton = new Button();
        private final HBox hbox = new HBox();
        private AppController controller;
        private Path currentItem;
        
        // Default album art to show when no art is found
        private static final Image DEFAULT_ALBUM_ART = createDefaultAlbumArt();

        public AlbumListCell(ListView<Path> albumListView) {
            super();
            setupCell(albumListView);
        }

        @Override
        protected void updateItem(Path item, boolean empty) {
            super.updateItem(item, empty);

            // Clear previous item
            currentItem = null;
            albumArtView.setImage(DEFAULT_ALBUM_ART);
            
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                currentItem = item;
                if (controller == null) {
                    controller = (AppController) getScene().getRoot().getUserData();
                }
                albumLabel.setText(controller.getAlbumService().albumPathToDisplayString(item));
                setGraphic(hbox);
                setText(null); // Important: set text to null if using graphic

                // Make sure buttons are visible when an item is present
                folderButton.setVisible(true);
                excludeButton.setVisible(true);
                playButton.setVisible(true);
                
                // Load album art asynchronously
                loadAlbumArt(item);
            }
        }
        
        private void loadAlbumArt(Path albumPath) {
            // Set default image first
            albumArtView.setImage(DEFAULT_ALBUM_ART);
            
            // Skip if the cell has been reused for a different item
            if (!albumPath.equals(currentItem)) {
                return;
            }
            
            // Load album art in background
            executorService.submit(() -> {
                try {
                    AlbumArtExtractor.getAlbumArt(albumPath).ifPresent(image -> {
                        // Check again if the cell hasn't been reused
                        if (albumPath.equals(currentItem)) {
                            javafx.application.Platform.runLater(() -> {
                                if (albumPath.equals(currentItem)) {
                                    albumArtView.setImage(image);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Error loading album art for " + albumPath, e);
                }
            });
        }
        
        private static Image createDefaultAlbumArt() {
            // Create a simple default album art
            WritableImage image = new WritableImage(IMAGE_SIZE, IMAGE_SIZE);
            PixelWriter writer = image.getPixelWriter();
            for (int x = 0; x < IMAGE_SIZE; x++) {
                for (int y = 0; y < IMAGE_SIZE; y++) {
                    // Create a simple gradient background
                    Color color = Color.rgb(200, 200, 200);
                    writer.setColor(x, y, color);
                }
            }
            return image;
        }
        
        private void setupCell(ListView<Path> albumListView) {
            // Configure album art view
            albumArtView.setFitWidth(IMAGE_SIZE);
            albumArtView.setFitHeight(IMAGE_SIZE);
            albumArtView.setPreserveRatio(true);
            albumArtView.setSmooth(true);
            albumArtView.setImage(DEFAULT_ALBUM_ART);
            
            // Add all components to HBox
            hbox.getChildren().addAll(albumArtView, albumLabel, folderButton, excludeButton, playButton);
            hbox.setSpacing(10);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(2));
            
            // Make sure the album art has a fixed width and the label takes remaining space
            HBox.setHgrow(albumLabel, Priority.ALWAYS);
            albumLabel.setMaxWidth(Double.MAX_VALUE);
            albumLabel.setAlignment(Pos.CENTER_LEFT);
            albumLabel.setContentDisplay(ContentDisplay.LEFT);
            
            // Set fixed height for the cell
            setPrefHeight(40);
            
            // Set up buttons
            setupFolderButton();
            setupExcludeButton(albumListView);
            setupPlayButton();
            
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setGraphic(hbox);
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
            folderButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand; -fx-opacity: 0.7;");
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
                if (albumPath == null)return;
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
            playButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand; -fx-opacity: 0.7;");
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

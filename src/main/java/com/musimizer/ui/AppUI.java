package com.musimizer.ui;

import com.musimizer.controller.AppController;
import com.musimizer.util.SettingsManager;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
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
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppUI {
    public static BorderPane createRootPane(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // Main content
        ListView<String> albumList = createAlbumListView();
        
        // Create menu
        MenuBar menuBar = createMenuBar(albumList, primaryStage);
        
        // Buttons
        Button pickButton = createPickButton();
        Button backButton = createBackButton();
        HBox buttonBox = new HBox(10, pickButton, backButton);
        
        // Main content area with title and album list
        Label titleLabel = new Label("Randomly Selected Albums");
        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));
        vbox.getChildren().addAll(titleLabel, albumList, buttonBox);
        VBox.setVgrow(albumList, Priority.ALWAYS);
        root.setTop(menuBar);
        root.setCenter(vbox);

        // Initialize controller
        AppController controller = new AppController(primaryStage, albumList, pickButton, backButton, titleLabel);
        root.setUserData(controller);
        
        // Set up button actions
        pickButton.setOnAction(e -> controller.pickAlbums());
        backButton.setOnAction(e -> controller.showRandomPicks());
        
        // Add key handler to the scene
        setupEscapeKeyHandler(primaryStage, controller);

        return root;
    }
    
    private static ListView<String> createAlbumListView() {
        ListView<String> listView = new ListView<>();
        listView.setCellFactory(lv -> new AlbumListCell());
        return listView;
    }
    
    private static MenuBar createMenuBar(ListView<String> albumList, Stage primaryStage) {
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
        // Implementation for showing exclusion list
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Excluded Albums");
        alert.setHeaderText("The following albums are excluded from the random selection:");
        alert.setContentText("Exclusion list functionality will be implemented here.");
        alert.initOwner(owner);
        alert.showAndWait();
    }
    
    private static void showFindDialog(ListView<String> albumList, AppController controller) {
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
    
    private static class AlbumListCell extends ListCell<String> {
        private static final Logger LOGGER = Logger.getLogger(AlbumListCell.class.getName());
        private final Label albumLabel = new Label();
        private final Button excludeButton = new Button();
        private final Button folderButton = new Button();
        private final HBox hbox = new HBox();
        
        public AlbumListCell() {
            super();
            setupCell();
        }
        
        private void setupCell() {
            hbox.getChildren().addAll(albumLabel, folderButton, excludeButton);
            hbox.setSpacing(10);
            HBox.setHgrow(albumLabel, Priority.ALWAYS);
            albumLabel.setMaxWidth(Double.MAX_VALUE);
            hbox.setFillHeight(true);
            hbox.setStyle("-fx-alignment: center-left;");
            
            setupFolderButton();
            setupExcludeButton();
            
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setGraphic(hbox);
        }
        
        private void setupFolderButton() {
            SVGPath folderIcon = new SVGPath();
            folderIcon.setContent(
                "M3 7V5a2 2 0 0 1 2-2h3.17a2 2 0 0 1 1.41.59l1.83 1.82H19a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7zm2 0h14v8H5V7z");
            folderIcon.setStyle("-fx-fill: #d5bd1f;");
            folderIcon.setScaleX(0.9);
            folderIcon.setScaleY(0.9);
            folderButton.setGraphic(folderIcon);
            folderButton.setPrefSize(20, 20);
            folderButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand;");
            folderButton.setTooltip(new Tooltip("Open album folder"));
            
            folderButton.setOnAction(e -> {
                String album = getItem();
                if (album != null) {
                    try {
                        openAlbumFolder(album);
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "Error opening album folder", ex);
                        showError("Error", "Could not open folder", 
                            "Error opening album folder: " + ex.getMessage());
                    }
                }
            });
        }
        
        private void setupExcludeButton() {
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
            excludeButton.setPrefSize(28, 28);
            excludeButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-opacity: 0.7;");
            
            excludeButton.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                excludeButton.setStyle(
                    "-fx-background-color: " + (isNowHovered ? "#ffeeee;" : "transparent;") + 
                    " -fx-padding: 2;" + 
                    " -fx-opacity: " + (isNowHovered ? "1.0;" : "0.7;"));
            });
        }
        
        private void openAlbumFolder(String albumPath) throws IOException {
            String[] parts = albumPath.split(" - ", 2);
            if (parts.length == 2) {
                Path dir = Paths.get(SettingsManager.getMusicDir().toString(), 
                    parts[0], parts[1]);
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
        
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                albumLabel.setText(item);
                setGraphic(hbox);
            }
        }
    }
}

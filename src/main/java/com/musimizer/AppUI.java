package com.musimizer;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import javafx.util.Callback;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.Tooltip;
import java.awt.Desktop;

public class AppUI {
    public static BorderPane createRootPane(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // Create menu
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");

        MenuItem settingsItem = new MenuItem("Settings...");
        settingsItem.setOnAction(e -> showSettings(primaryStage));

        MenuItem showExclusionsItem = new MenuItem("Show Exclusion List...");
        showExclusionsItem.setOnAction(e -> showExclusionList());

        fileMenu.getItems().addAll(settingsItem, showExclusionsItem);
        menuBar.getMenus().add(fileMenu);
        root.setTop(menuBar);

        // Main content
        Label title = new Label("Randomly Selected Albums");
        ListView<String> albumList = new ListView<>();
        
        // Pick button
        Button pickButton = new Button("Pick New Random Selections");
        pickButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        
        VBox vbox = new VBox(10, title, albumList, pickButton);
        vbox.setPadding(new Insets(20));
        VBox.setVgrow(albumList, Priority.ALWAYS);
        root.setCenter(vbox);

        // Controller manages album picking and exclusion
        AppController controller = new AppController(primaryStage, albumList);
        root.setUserData(controller);

        albumList.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<>() {
                    private final Label albumLabel = new Label();
                    private final Button excludeButton = new Button();
                    private final Button folderButton = new Button();
                    private final HBox hbox = new HBox();
                    {
                        hbox.getChildren().addAll(albumLabel, folderButton, excludeButton);
                        hbox.setSpacing(10);
                        HBox.setHgrow(albumLabel, Priority.ALWAYS);
                        albumLabel.setMaxWidth(Double.MAX_VALUE);
                        hbox.setFillHeight(true);
                        hbox.setStyle("-fx-alignment: center-left;");
                        
                        // SVG folder icon
                        SVGPath folderIcon = new SVGPath();
                        folderIcon.setContent(
                                "M3 7V5a2 2 0 0 1 2-2h3.17a2 2 0 0 1 1.41.59l1.83 1.82H19a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7zm2 0h14v8H5V7z");
                        folderIcon.setStyle("-fx-fill: #d5bd1f;");
                        folderIcon.setScaleX(0.9);
                        folderIcon.setScaleY(0.9);
                        folderButton.setGraphic(folderIcon);
                        folderButton.setPrefWidth(20);
                        folderButton.setPrefHeight(20);
                        folderButton.setStyle("-fx-background-color: transparent; -fx-padding: 2;");
                        
                        // SVG do not enter icon (solid circle with single thick diagonal line)
                        SVGPath noEntryIcon = new SVGPath();
                        // Solid circle with single thick diagonal line from top-left to bottom-right
                        noEntryIcon.setContent(
                            // Circle
                            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" +
                            // Single thick diagonal line from top-left to bottom-right
                            "M5 5l14 14");
                        noEntryIcon.setStyle(
                            "-fx-fill: #ffffff;" +
                            "-fx-stroke: #ff0000;" +  // Brighter red for the line
                            "-fx-stroke-width: 3.5;" +  // Thicker line
                            "-fx-stroke-linecap: round;");  // Rounded line ends
                        noEntryIcon.setScaleX(0.7);
                        noEntryIcon.setScaleY(0.7);
                        excludeButton.setGraphic(noEntryIcon);
                        excludeButton.setPrefWidth(28);  // Slightly larger to accommodate the thicker lines
                        excludeButton.setPrefHeight(28);
                        excludeButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-opacity: 0.7;");
                        
                        // Add hover effects
                        excludeButton.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                            excludeButton.setStyle(
                                "-fx-background-color: " + (isNowHovered ? "#ffeeee;" : "transparent;") + 
                                " -fx-padding: 2;" + 
                                " -fx-opacity: " + (isNowHovered ? "1.0;" : "0.7;"));
                        });
                        folderButton.setMinWidth(20);
                        folderButton.setMinHeight(20);
                        folderButton.setMaxWidth(20);
                        folderButton.setMaxHeight(20);
                        folderButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                        folderButton.setTooltip(new Tooltip("Open album folder"));

                        // Add action for folder button
                        folderButton.setOnAction(e -> {
                            String album = getItem();
                            if (album != null) {
                                String[] parts = album.split(" - ", 2);
                                if (parts.length == 2) {
                                    java.io.File dir = new java.io.File(SettingsManager.getMusicDir(), 
                                            parts[0] + java.io.File.separator + parts[1]);
                                    if (dir.exists() && dir.isDirectory()) {
                                        try {
                                            Desktop.getDesktop().open(dir);
                                        } catch (Exception ex) {
                                            // Optionally show error dialog
                                            showError("Could not open folder",
                                                    "Error opening album folder: " + dir.getAbsolutePath(),
                                                    ex.getMessage());
                                        }
                                    } else {
                                        showError("Folder Not Found", "Album directory does not exist:",
                                                dir.getAbsolutePath());
                                    }
                                }
                            }
                        });

                        // Set tooltip for exclude button
                        excludeButton.setTooltip(new Tooltip("Exclude this album"));
                        
                        // Add action for exclude button
                        excludeButton.setOnAction(e -> {
                            String album = getItem();
                            if (album != null) {
                                AppController ctrl = (AppController) param.getScene().getRoot().getUserData();
                                ctrl.excludeAlbum(album);
                                // Remove only this album from the list view
                                param.getItems().remove(album);
                            }
                        });
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
                };
            }
        });

        pickButton.setOnAction(e -> {
            AppController ctrl = (AppController) root.getUserData();
            ctrl.pickAlbums();
        });

        return root;
    }

    private static void showSettings(Stage owner) {
        SettingsDialog dialog = new SettingsDialog(owner);
        dialog.initOwner(owner);
        dialog.showAndWait().ifPresent(settings -> {
            if (settings != null && settings.musicDir != null && !settings.musicDir.isEmpty()) {
                // Update settings
                SettingsManager.setMusicDir(settings.musicDir);
                SettingsManager.setNumberOfPicks(settings.numberOfPicks);
                
                // Refresh the album list with new settings
                AppController ctrl = (AppController) ((BorderPane) owner.getScene().getRoot()).getUserData();
                ctrl.reinitializeWithNewSettings();
            }
        });
    }

    private static void showExclusionList() {
        Path exclusionFile = AppController.getExclusionFilePath();
        if (exclusionFile != null && exclusionFile.toFile().exists()) {
            try {
                Desktop.getDesktop().edit(exclusionFile.toFile());
            } catch (IOException e) {
                try {
                    // Fallback to opening the file in the default application
                    Desktop.getDesktop().open(exclusionFile.toFile());
                } catch (IOException ex) {
                    showError("Could not open exclusion list", "Error opening file: " + exclusionFile, ex.getMessage());
                }
            }
        } else {
            showInfo("Exclusion List", "No exclusions yet. Check albums to add them to the exclusion list.");
        }
    }

    public static void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Shows an error dialog with options to the user
     * @param title The title of the dialog
     * @param header The header text
     * @param content The content text (should include the question for the user)
     * @return true if the user clicks OK/Yes, false if they click Cancel/No
     */
    public static boolean showErrorWithOptions(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        // Customize the button types
        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        alert.getButtonTypes().setAll(yesButton, noButton);
        
        // Show the dialog and wait for user response
        java.util.Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;
    }

    private static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

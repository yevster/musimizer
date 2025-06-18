package com.musimizer;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javafx.util.Callback;

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
    Button pickButton = new Button("Pick New Random Selections");
    VBox vbox = new VBox(10, title, albumList, pickButton);
    vbox.setPadding(new Insets(20));
    VBox.setVgrow(albumList, Priority.ALWAYS);
    root.setCenter(vbox);

    // Controller manages album picking and exclusion
    AppController controller = new AppController(null, albumList);
    root.setUserData(controller);

    albumList.setCellFactory(new Callback<>() {
    @Override
    public ListCell<String> call(ListView<String> param) {
        return new ListCell<>() {
            private final Label albumLabel = new Label();
            private final CheckBox excludeBox = new CheckBox();
            private final HBox hbox = new HBox();
            {
                hbox.getChildren().addAll(albumLabel, excludeBox);
                hbox.setSpacing(10);
                HBox.setHgrow(albumLabel, Priority.ALWAYS);
                albumLabel.setMaxWidth(Double.MAX_VALUE);
                hbox.setFillHeight(true);
                hbox.setStyle("-fx-alignment: center-left;");
                excludeBox.setStyle("-fx-alignment: center-right;");
                excludeBox.setOnAction(e -> {
                    String album = getItem();
                    if (album != null && excludeBox.isSelected()) {
                        AppController ctrl = (AppController) root.getUserData();
                        ctrl.excludeAlbumNoRefresh(album);
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
                    excludeBox.setSelected(false);
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
        dialog.showAndWait().ifPresent(dir -> {
            if (dir != null && !dir.isEmpty()) {
                SettingsManager.setMusicDir(dir);
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
    
    private static void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

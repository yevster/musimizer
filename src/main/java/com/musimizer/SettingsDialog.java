package com.musimizer;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;

public class SettingsDialog extends Dialog<SettingsDialog.Settings> {
    
    public static class Settings {
        public final String musicDir;
        public final int numberOfPicks;

        public Settings(String musicDir, int numberOfPicks) {
            this.musicDir = musicDir;
            this.numberOfPicks = numberOfPicks;
        }
    }

    public SettingsDialog(Window owner) {
        setTitle("Settings");
        
        // Load current settings
        String currentDir = SettingsManager.getMusicDir();
        int currentPicks = SettingsManager.getNumberOfPicks();
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        // Music directory controls
        Label dirLabel = new Label("Music Collection Directory:");
        TextField dirField = new TextField(currentDir != null ? currentDir : "");
        Button browse = new Button("Browse...");
        
        // Number of picks controls
        Label picksLabel = new Label("Number of Picks:");
        Spinner<Integer> picksSpinner = new Spinner<>(1, 100, currentPicks);
        picksSpinner.setEditable(true);
        
        // Set up the spinner to only accept numbers
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = 
            (SpinnerValueFactory.IntegerSpinnerValueFactory) picksSpinner.getValueFactory();
        valueFactory.setConverter(new IntegerStringConverter() {
            @Override
            public Integer fromString(String s) {
                try {
                    return super.fromString(s);
                } catch (NumberFormatException e) {
                    return picksSpinner.getValue();
                }
            }
        });
        
        // If there's a current directory, set it as the initial directory for the chooser
        if (currentDir != null && !currentDir.isEmpty()) {
            File currentDirFile = new File(currentDir);
            if (currentDirFile.exists() && currentDirFile.isDirectory()) {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setInitialDirectory(currentDirFile);
            }
        }
        
        // Add components to grid
        grid.add(dirLabel, 0, 0);
        grid.add(dirField, 1, 0);
        grid.add(browse, 2, 0);
        grid.add(picksLabel, 0, 1);
        grid.add(picksSpinner, 1, 1);
        
        // Set up browse button action
        browse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Music Collection Directory");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                dirField.setText(selected.getAbsolutePath());
            }
        });
        
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Get the OK button and add a custom event filter to handle validation
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        
        // Disable OK button by default if no directory is selected
        okButton.setDisable(dirField.getText().trim().isEmpty());
        
        // Add listener to enable/disable OK button based on directory field
        dirField.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty());
        });
        
        // Add an event filter to the OK button to handle validation before the dialog closes
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String dirPath = dirField.getText().trim();
            File dir = new File(dirPath);
            
            if (!dir.exists() || !dir.isDirectory()) {
                // Show error and consume the event to prevent dialog from closing
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Directory");
                alert.setHeaderText("Directory not found");
                alert.setContentText("The specified music directory does not exist or is not a directory:\n" + dirPath);
                alert.showAndWait();
                event.consume(); // Prevent the dialog from closing
            }
        });
        
        // Set up result converter to return the settings
        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String dirPath = dirField.getText().trim();
                return new Settings(dirPath, picksSpinner.getValue());
            }
            return null;
        });
    }
}

package com.musimizer.ui.dialogs;

import com.musimizer.util.SettingsManager;
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
        public final int numberOfSearchResults;

        public Settings(String musicDir, int numberOfPicks, int numberOfSearchResults) {
            this.musicDir = musicDir;
            this.numberOfPicks = numberOfPicks;
            this.numberOfSearchResults = numberOfSearchResults;
        }
    }

    public SettingsDialog(Window owner) {
        setTitle("Settings");
        initOwner(owner);
        
        // Load current settings
        String currentDir = SettingsManager.getMusicDir();
        int currentPicks = SettingsManager.getNumberOfPicks();
        int currentSearchResults = SettingsManager.getNumberOfSearchResults();
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        // Music directory controls
        Label dirLabel = new Label("Music Collection Directory:");
        TextField dirField = new TextField(currentDir != null ? currentDir : "");
        dirField.setPrefWidth(400);
        Button browse = new Button("Browse...");
        
        // Number of picks controls
        Label picksLabel = new Label("Number of Picks:");
        Spinner<Integer> picksSpinner = createNumberSpinner(1, 100, currentPicks);
        
        // Number of search results controls
        Label searchResultsLabel = new Label("Number of Search Results:");
        Spinner<Integer> searchResultsSpinner = createNumberSpinner(1, 100, currentSearchResults);
        
        // Add components to grid
        grid.add(dirLabel, 0, 0);
        grid.add(dirField, 1, 0);
        grid.add(browse, 2, 0);
        grid.add(picksLabel, 0, 1);
        grid.add(picksSpinner, 1, 1);
        grid.add(searchResultsLabel, 0, 2);
        grid.add(searchResultsSpinner, 1, 2);
        
        // Set up browse button action
        browse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Music Collection Directory");
            
            // Set initial directory if one exists
            if (currentDir != null && !currentDir.isEmpty()) {
                File currentDirFile = new File(currentDir);
                if (currentDirFile.exists() && currentDirFile.isDirectory()) {
                    chooser.setInitialDirectory(currentDirFile);
                }
            }
            
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
                return new Settings(
                    dirPath,
                    picksSpinner.getValue(),
                    searchResultsSpinner.getValue()
                );
            }
            return null;
        });
    }
    
    private Spinner<Integer> createNumberSpinner(int min, int max, int initialValue) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initialValue);
        spinner.setEditable(true);
        
        // Configure the spinner to handle invalid input
        SpinnerValueFactory.IntegerSpinnerValueFactory factory = 
            (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
            
        factory.setConverter(new IntegerStringConverter() {
            @Override
            public Integer fromString(String s) {
                try {
                    return super.fromString(s);
                } catch (NumberFormatException e) {
                    return spinner.getValue();
                }
            }
        });
        
        return spinner;
    }
}

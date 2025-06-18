package com.musimizer;

import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;

public class SettingsDialog extends Dialog<String> {
    public SettingsDialog(Window owner) {
        setTitle("Select Music Collection Directory");
        String currentDir = SettingsManager.getMusicDir();
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        Label label = new Label("Music Collection Directory:");
        TextField dirField = new TextField(currentDir != null ? currentDir : "");
        Button browse = new Button("Browse...");
        
        // If there's a current directory, set it as the initial directory for the chooser
        if (currentDir != null && !currentDir.isEmpty()) {
            File currentDirFile = new File(currentDir);
            if (currentDirFile.exists() && currentDirFile.isDirectory()) {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setInitialDirectory(currentDirFile);
            }
        }
        grid.add(label, 0, 0);
        grid.add(dirField, 1, 0);
        grid.add(browse, 2, 0);
        browse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Music Collection Directory");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                dirField.setText(selected.getAbsolutePath());
            }
        });
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return dirField.getText();
            }
            return null;
        });
    }
}

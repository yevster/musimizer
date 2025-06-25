module com.musimizer {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires java.logging;
    requires java.prefs;
    requires java.desktop;
    requires org.apache.commons.lang3;
    
    // Export all our packages
    exports com.musimizer;
    exports com.musimizer.controller;
    exports com.musimizer.ui;
    exports com.musimizer.ui.dialogs;
    exports com.musimizer.service;
    exports com.musimizer.repository;
    exports com.musimizer.util;
    exports com.musimizer.exception;
    
    // Open packages for reflection
    opens com.musimizer to javafx.fxml, javafx.graphics;
    opens com.musimizer.controller to javafx.fxml, javafx.graphics;
    opens com.musimizer.ui to javafx.fxml, javafx.graphics;
    opens com.musimizer.ui.dialogs to javafx.fxml, javafx.graphics;
}

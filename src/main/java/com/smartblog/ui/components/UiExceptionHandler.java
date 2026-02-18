package com.smartblog.ui.components;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Minimal UI exception handler used by JavaFX controllers.
 * Provided as a lightweight stub so non-UI builds or IDEs can compile.
 */
public final class UiExceptionHandler {
    private UiExceptionHandler() {}

    public static void showError(String title, String message) {
        try {
            Platform.runLater(() -> {
                Alert a = new Alert(AlertType.ERROR);
                a.setTitle(title == null ? "Error" : title);
                a.setHeaderText(null);
                a.setContentText(message == null ? "" : message);
                a.showAndWait();
            });
        } catch (Throwable t) {
            System.err.println((title == null ? "Error" : title) + ": " + (message == null ? "" : message));
        }
    }

    public static void showAuthError(String message) {
        try {
            Platform.runLater(() -> {
                Alert a = new Alert(AlertType.WARNING);
                a.setTitle("Unauthorized");
                a.setHeaderText(null);
                a.setContentText(message == null ? "" : message);
                a.showAndWait();
            });
        } catch (Throwable t) {
            System.err.println("Unauthorized: " + (message == null ? "" : message));
        }
    }
}

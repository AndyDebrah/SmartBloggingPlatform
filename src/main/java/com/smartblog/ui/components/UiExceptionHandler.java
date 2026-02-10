package com.smartblog.ui.components;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public final class UiExceptionHandler {
    private UiExceptionHandler(){}

    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }

    public static void showAuthError(String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(AlertType.WARNING);
            a.setTitle("Unauthorized");
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }
}

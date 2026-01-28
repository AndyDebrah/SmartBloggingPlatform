
package com.smartblog.ui.components;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

public final class LoadingOverlay {
    public static StackPane overlay(StackPane root) {
        StackPane overlay = new StackPane(new ProgressIndicator());
        overlay.getStyleClass().add("overlay");
        overlay.setVisible(false);
        root.getChildren().add(overlay);
        return overlay;
    }
    public static void show(StackPane root, StackPane overlay) {
        overlay.setVisible(true);
    }
    public static void hide(StackPane root, StackPane overlay) {
        overlay.setVisible(false);
    }
}

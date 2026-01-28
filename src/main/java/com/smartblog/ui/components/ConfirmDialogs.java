
package com.smartblog.ui.components;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class ConfirmDialogs {
    public static boolean confirm(String title, String msg) {
        var a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}

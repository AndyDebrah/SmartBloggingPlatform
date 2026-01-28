
package com.smartblog.ui.components;

import javafx.animation.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public final class NotificationToast {
    public static void show(StackPane root, String msg) {
        Label toast = new Label(msg);
        toast.getStyleClass().add("toast");
        root.getChildren().add(toast);
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);

        FadeTransition inT = new FadeTransition(Duration.millis(200), toast);
        inT.setFromValue(0); inT.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.seconds(2));

        FadeTransition outT = new FadeTransition(Duration.millis(300), toast);
        outT.setFromValue(1); outT.setToValue(0);
        outT.setOnFinished(e -> root.getChildren().remove(toast));

        new SequentialTransition(inT, stay, outT).play();
    }
}

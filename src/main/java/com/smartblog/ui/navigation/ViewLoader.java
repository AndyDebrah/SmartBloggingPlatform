
package com.smartblog.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public final class ViewLoader {
    public static Parent load(View view) {
        try {
            return FXMLLoader.load(ViewLoader.class.getResource(view.fxml));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + view, e);
        }
    }
}

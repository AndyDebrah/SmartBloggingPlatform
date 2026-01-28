
package com.smartblog.ui.navigation;

import javafx.scene.Parent;
import javafx.scene.Scene;

public final class SceneManager {
    public static Scene create(Parent root, boolean dark) {
        Scene scene = new Scene(root, 1200, 800);
        
        String variablesUrl = SceneManager.class.getResource("/com/smartblog/ui/themes/variables.css").toExternalForm();
        String themeUrl = SceneManager.class.getResource(
            dark ? "/com/smartblog/ui/themes/styles-dark.css" : "/com/smartblog/ui/themes/styles-light.css"
        ).toExternalForm();
        
        scene.getStylesheets().add(variablesUrl);
        scene.getStylesheets().add(themeUrl);
        
        return scene;
    }
}

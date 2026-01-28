
package com.smartblog.ui.navigation;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;

public final class SceneManager {
    public static Scene create(Parent root, boolean dark) {
        Scene scene = new Scene(root, 1200, 800);
        
        String variablesUrl = SceneManager.class.getResource("/com/smartblog/ui/themes/variables.css").toExternalForm();
        String themeUrl = SceneManager.class.getResource(
            dark ? "/com/smartblog/ui/themes/styles-dark.css" : "/com/smartblog/ui/themes/styles-light.css"
        ).toExternalForm();
        
        scene.getStylesheets().add(variablesUrl);
        scene.getStylesheets().add(themeUrl);
        
        // Add a temporary debug filter to log clicks and their target nodes when troubleshooting UI events
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, ev -> {
            try {
                Object tgt = ev.getTarget();
                Node intersected = ev.getPickResult() != null ? ev.getPickResult().getIntersectedNode() : null;
                String id = (intersected != null && intersected.getId() != null) ? intersected.getId() : "(no-id)";
                System.out.println("[SceneDebug] MouseClicked target=" + tgt + " intersected=" + intersected + " id=" + id + " eventSource=" + ev.getSource());
            } catch (Exception e) {
                System.out.println("[SceneDebug] Failed to log mouse event: " + e.getMessage());
            }
        });

        return scene;
    }
}

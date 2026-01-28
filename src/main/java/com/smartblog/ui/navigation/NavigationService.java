
package com.smartblog.ui.navigation;

import java.util.ArrayDeque;
import java.util.Deque;

import javafx.scene.Parent;
import javafx.stage.Stage;

public final class NavigationService {
    private static Stage primaryStage;
    private static boolean dark = false;

    private static View currentView = null;
    private static ViewParams currentParams = null;
    private static final Deque<ViewEntry> history = new ArrayDeque<>();

    private static record ViewEntry(View view, ViewParams params) {}

    public static void init(Stage stage) { primaryStage = stage; }
    public static void setDark(boolean darkTheme) { dark = darkTheme; }

    public static void navigate(View view) {
        navigate(view, null);
    }

    public static void navigate(View view, ViewParams params) {
        // push current onto history
        if (currentView != null) history.push(new ViewEntry(currentView, currentParams));
        // load target
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(ViewLoader.class.getResource(view.fxml));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ParamReceiver && params != null) {
                ((ParamReceiver) controller).setParams(params);
            }
            primaryStage.setScene(SceneManager.create(root, dark));
            primaryStage.show();
            currentView = view;
            currentParams = params;
        } catch (Exception e) {
            throw new RuntimeException("Failed to navigate to " + view, e);
        }
    }

    public static boolean navigateBack() {
        if (history.isEmpty()) return false;
        ViewEntry entry = history.pop();
        // load without pushing current onto history
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(ViewLoader.class.getResource(entry.view().fxml));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ParamReceiver && entry.params() != null) {
                ((ParamReceiver) controller).setParams(entry.params());
            }
            primaryStage.setScene(SceneManager.create(root, dark));
            primaryStage.show();
            currentView = entry.view();
            currentParams = entry.params();
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to navigate back to " + entry.view(), e);
        }
    }

    public static void clearHistory() { history.clear(); }
}

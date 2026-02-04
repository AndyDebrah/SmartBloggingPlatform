
package com.smartblog.ui.navigation;

import java.util.ArrayDeque;
import java.util.Deque;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Central navigation helper for the JavaFX application.
 * Manages view loading, history and optional debug fallback handlers.
 */
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
        if (currentView != null) history.push(new ViewEntry(currentView, currentParams));
        try {
            System.out.println("[Navigation] Loading view: " + view + " -> " + view.fxml);
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(ViewLoader.class.getResource(view.fxml));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller != null) {
                System.out.println("[Navigation] Controller: " + controller.getClass().getName());
                try {
                    var cs = controller.getClass().getProtectionDomain().getCodeSource();
                    System.out.println("[Navigation] Controller code source: " + (cs != null ? cs.getLocation() : "(unknown)"));
                } catch (Exception ex) {
                    System.out.println("[Navigation] Could not determine controller code source: " + ex.getMessage());
                }
                try {
                    var methods = controller.getClass().getDeclaredMethods();
                    System.out.print("[Navigation] Controller methods: ");
                    for (var m : methods) System.out.print(m.getName() + ",");
                    System.out.println();
                } catch (Exception ex) {
                    System.out.println("[Navigation] Could not list controller methods: " + ex.getMessage());
                }
            } else {
                System.out.println("[Navigation] Controller: null");
            }
            // If this is the PerformanceController, inject services so benchmarks can run
            try {
                if (controller instanceof com.smartblog.ui.view.performance.PerformanceController perfCtrl) {
                    var ctx = com.smartblog.bootstrap.AppBootstrap.start();
                    perfCtrl.setServices(ctx.postService, ctx.commentService, ctx.tagService);
                    System.out.println("[Navigation] Injected services into PerformanceController");
                }
            } catch (Exception ex) {
                System.out.println("[Navigation] Could not inject services into controller: " + ex.getMessage());
            }
            if (controller instanceof ParamReceiver && params != null) {
                ((ParamReceiver) controller).setParams(params);
            }
            primaryStage.setScene(SceneManager.create(root, dark));
            primaryStage.show();
            // fallback: if a button with fx:id/viewPerformanceBtn exists, attach a diagnostic handler
            try {
                Node n = root.lookup("#viewPerformanceBtn");
                if (n instanceof Button b) {
                    System.out.println("[Navigation] Found viewPerformanceBtn via lookup, attaching fallback handlers. getOnAction=" + b.getOnAction());
                    b.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> System.out.println("[Fallback] viewPerformanceBtn mouse clicked (NavigationService)"));
                    b.setOnAction(ev -> {
                        System.out.println("[Fallback] viewPerformanceBtn action fired (NavigationService)");
                        NavigationService.navigate(View.PERFORMANCE);
                    });
                }
            } catch (Exception ex) {
                System.out.println("[Navigation] Could not attach fallback handler: " + ex.getMessage());
            }
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

            // If this is the PerformanceController, inject services so benchmarks can run
            try {
                if (controller instanceof com.smartblog.ui.view.performance.PerformanceController perfCtrl) {
                    var ctx = com.smartblog.bootstrap.AppBootstrap.start();
                    perfCtrl.setServices(ctx.postService, ctx.commentService, ctx.tagService);
                    System.out.println("[Navigation] Injected services into PerformanceController (navigateBack)");
                }
            } catch (Exception ex) {
                System.out.println("[Navigation] Could not inject services into controller (navigateBack): " + ex.getMessage());
            }

            primaryStage.setScene(SceneManager.create(root, dark));
            primaryStage.show();

            // fallback: if a button with fx:id/viewPerformanceBtn exists, attach a diagnostic handler
            try {
                Node n = root.lookup("#viewPerformanceBtn");
                if (n instanceof javafx.scene.control.Button b) {
                    System.out.println("[Navigation] Found viewPerformanceBtn via lookup (navigateBack), attaching fallback handlers. getOnAction=" + b.getOnAction());
                    b.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> System.out.println("[Fallback] viewPerformanceBtn mouse clicked (NavigationService navigateBack)"));
                    b.setOnAction(ev -> {
                        System.out.println("[Fallback] viewPerformanceBtn action fired (NavigationService navigateBack)");
                        NavigationService.navigate(View.PERFORMANCE);
                    });
                }
            } catch (Exception ex) {
                System.out.println("[Navigation] Could not attach fallback handler (navigateBack): " + ex.getMessage());
            }

            currentView = entry.view();
            currentParams = entry.params();
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to navigate back to " + entry.view(), e);
        }
    }

    public static void clearHistory() { history.clear(); }
}

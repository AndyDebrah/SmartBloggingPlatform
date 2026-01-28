
package com.smartblog;

import com.smartblog.ui.navigation.NavigationService;
import com.smartblog.ui.navigation.View;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        NavigationService.init(stage);
        NavigationService.setDark(true); // start in dark mode
        NavigationService.navigate(View.LOGIN);
    }
    public static void main(String[] args) { launch(args); }
}

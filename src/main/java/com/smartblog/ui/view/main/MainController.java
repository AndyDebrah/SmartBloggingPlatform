
package com.smartblog.ui.view.main;

import com.smartblog.application.security.SecurityContext;
import com.smartblog.ui.navigation.NavigationService;
import com.smartblog.ui.navigation.View;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainController {
    @FXML private Label userLabel;
    @FXML private Button logoutBtn, newPostBtn, backBtn;
    @FXML private Button dashboardBtn, postsBtn, commentsBtn, tagsBtn, usersBtn, analyticsBtn;
    @FXML private StackPane contentArea;

    private final MainViewModel vm = new MainViewModel();

    @FXML
    public void initialize() {
        var user = SecurityContext.getUser();
        if (user != null) {
            vm.setUser(user);
            userLabel.textProperty().bind(vm.welcome);
        }

        // Back button (navigate based on user role)
        if (backBtn != null) {
            backBtn.setOnAction(e -> {
                if (SecurityContext.isAdmin()) {
                    NavigationService.navigate(View.ADMIN);
                } else {
                    NavigationService.navigate(View.AUTHOR);
                }
            });
        }

        // Navigation actions
        newPostBtn.setOnAction(e -> NavigationService.navigate(View.POST_EDITOR));
        postsBtn.setOnAction(e -> NavigationService.navigate(View.POSTS));
        commentsBtn.setOnAction(e -> NavigationService.navigate(View.COMMENTS));
        tagsBtn.setOnAction(e -> NavigationService.navigate(View.TAG_MANAGER));
        usersBtn.setOnAction(e -> NavigationService.navigate(View.USERS));
        analyticsBtn.setOnAction(e -> NavigationService.navigate(View.ANALYTICS));
        
        logoutBtn.setOnAction(e -> logout());
    }

    private void logout() {
        SecurityContext.logout();
        NavigationService.navigate(View.LOGIN);
    }
}


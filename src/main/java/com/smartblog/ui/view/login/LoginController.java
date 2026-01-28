package com.smartblog.ui.view.login;

import java.util.Optional;

import com.smartblog.application.security.SecurityContext;
import com.smartblog.application.service.UserService;
import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.model.User;
import com.smartblog.ui.navigation.NavigationService;
import com.smartblog.ui.navigation.View;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.control.Hyperlink registerLink;

    @FXML
    public void initialize() {
        loginBtn.setOnAction(e -> doLogin());
        passwordField.setOnAction(e -> doLogin());
        usernameField.setOnAction(e -> doLogin());
        if (registerLink != null) registerLink.setOnAction(e -> showRegisterDialog());
    }

    private void doLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            showError("Please enter username and password.");
            return;
        }

        var ctx = AppBootstrap.start();
        UserService userService = ctx.userService;
        try {
            System.out.println("[login] attempting login for '" + username + "'");
            Optional<User> maybe = userService.authenticate(username, password);
            System.out.println("[login] authenticate returned present=" + maybe.isPresent());
            if (maybe.isPresent()) {
                User u = maybe.get();
                SecurityContext.login(u);
                routeAfterLogin();
            } else {
                showError("Invalid username or password.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Login failed: " + ex.getMessage());
        }
    }

    private void routeAfterLogin() {
        if (SecurityContext.isAdmin()) {
            NavigationService.navigate(View.ADMIN);
        } else if (SecurityContext.isAuthor()) {
            NavigationService.navigate(View.AUTHOR);
        } else {
            NavigationService.navigate(View.MAIN);
        }
    }

    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg);
        else {
            var a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setTitle("Login Error");
            a.showAndWait();
        }
    }

    private void showRegisterDialog() {
        javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Create Account");
        dialog.setHeaderText("Register for SmartBlog");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField regUsername = new TextField();
        regUsername.setPromptText("Username");
        TextField regEmail = new TextField();
        regEmail.setPromptText("Email");
        PasswordField regPassword = new PasswordField();
        regPassword.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(regUsername, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(regEmail, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(regPassword, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String username = regUsername.getText();
            String email = regEmail.getText();
            String password = regPassword.getText();

            if (username == null || username.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
                showError("All fields are required.");
                return;
            }

            try {
                var ctx = AppBootstrap.start();
                long userId = ctx.userService.register(username, email, password, "AUTHOR");
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Success");
                success.setHeaderText("Account Created");
                success.setContentText("Your account has been created successfully! You can now log in.");
                success.showAndWait();
            } catch (Exception ex) {
                showError("Registration failed: " + ex.getMessage());
            }
        }
    }
}

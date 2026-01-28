
package com.smartblog.ui.view.login;

import com.smartblog.application.service.UserService;
import com.smartblog.core.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LoginViewModel {
    private final UserService userService;
    public final StringProperty username = new SimpleStringProperty();
    public final StringProperty password = new SimpleStringProperty();
    public final StringProperty error = new SimpleStringProperty();

    public LoginViewModel(UserService userService) { this.userService = userService; }

    public User authenticate() {
        var u = userService.authenticate(username.get(), password.get());
        if (u.isEmpty()) { error.set("Invalid username or password."); return null; }
        return u.get();
    }
}

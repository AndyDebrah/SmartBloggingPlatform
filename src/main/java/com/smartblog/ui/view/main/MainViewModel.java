
package com.smartblog.ui.view.main;

import com.smartblog.core.model.User;
import javafx.beans.property.*;

public class MainViewModel {
    public final StringProperty welcome = new SimpleStringProperty("Welcome");
    public void setUser(User user) {
        welcome.set("Welcome, " + user.getUsername());
    }
}

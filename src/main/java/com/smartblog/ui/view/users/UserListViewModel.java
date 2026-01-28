
package com.smartblog.ui.view.users;

import com.smartblog.application.service.UserService;
import com.smartblog.core.dto.UserDTO;
import javafx.beans.property.*;
import javafx.collections.*;

import java.util.List;

public class UserListViewModel {
    private final UserService service;
    public final ObservableList<UserDTO> data = FXCollections.observableArrayList();
    public final StringProperty query = new SimpleStringProperty();

    public UserListViewModel(UserService s) { this.service = s; }

    public void load() { data.setAll(service.list(1, 200)); }

    public void search() {
        String q = query.get();
        List<UserDTO> all = service.list(1, 200);
        if (q == null || q.isBlank()) { data.setAll(all); return; }
        data.setAll(all.stream().filter(u ->
                u.username().contains(q) || u.email().contains(q)
        ).toList());
    }
}

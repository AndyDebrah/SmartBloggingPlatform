
package com.smartblog.ui.view.users;

import com.smartblog.application.security.SecurityContext;
import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.dto.UserDTO;
import com.smartblog.ui.components.ConfirmDialogs;
import com.smartblog.ui.navigation.NavigationService;
import com.smartblog.ui.navigation.View;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

public class UserListController {
    @FXML private TextField searchField;
    @FXML private Button searchBtn, newBtn, editBtn, deleteBtn, backBtn;
    @FXML private TableView<UserDTO> table;
    @FXML private TableColumn<UserDTO, Number> colId;
    @FXML private TableColumn<UserDTO, String> colUsername, colEmail, colRole;

    private UserListViewModel vm;
    private com.smartblog.application.service.UserService userService;

    @FXML
    public void initialize() {
        var ctx = AppBootstrap.start();
        userService = ctx.userService;
        vm = new UserListViewModel(userService);

        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleLongProperty(c.getValue().id()));
        colUsername.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().username()));
        colEmail.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().email()));
        colRole.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().role()));

        table.setItems(vm.data);
        searchField.textProperty().bindBidirectional(vm.query);

        // Back button - navigate to admin dashboard or main view
        if (backBtn != null) {
            backBtn.setOnAction(e -> {
                if (SecurityContext.isAdmin()) {
                    NavigationService.navigate(View.ADMIN);
                } else {
                    NavigationService.navigate(View.MAIN);
                }
            });
        }
        
        searchBtn.setOnAction(e -> vm.search());
        newBtn.setOnAction(e -> createUser());
        editBtn.setOnAction(e -> editEmail());
        deleteBtn.setOnAction(e -> deleteUser());

        vm.load();
    }

    private void createUser() {
        TextInputDialog u = new TextInputDialog(); u.setHeaderText("Username"); var un = u.showAndWait(); if (un.isEmpty()) return;
        TextInputDialog m = new TextInputDialog(); m.setHeaderText("Email"); var em = m.showAndWait(); if (em.isEmpty()) return;
        TextInputDialog p = new TextInputDialog(); p.setHeaderText("Password"); var pw = p.showAndWait(); if (pw.isEmpty()) return;
        long id = userService.register(un.get(), em.get(), pw.get(), "AUTHOR");
        vm.load();
    }

    private void editEmail() {
        var sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TextInputDialog d = new TextInputDialog(sel.email());
        d.setHeaderText("New email");
        d.showAndWait().ifPresent(newEmail -> {
            userService.updateProfile(sel.id(), newEmail);
            vm.load();
        });
    }

    private void deleteUser() {
        var sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (ConfirmDialogs.confirm("Delete", "Delete " + sel.username() + "?")) {
            userService.softDelete(sel.id());
            vm.load();
        }
    }
}

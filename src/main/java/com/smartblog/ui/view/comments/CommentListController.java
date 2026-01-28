
package com.smartblog.ui.view.comments;

import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.ui.components.ConfirmDialogs;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class CommentListController {
    @FXML private TextField postIdField;
    @FXML private Button loadBtn, addBtn, editBtn, deleteBtn;
    @FXML private TableView<com.smartblog.core.dto.CommentDTO> table;
    @FXML private TableColumn<com.smartblog.core.dto.CommentDTO, Number> colId;
    @FXML private TableColumn<com.smartblog.core.dto.CommentDTO, String> colUser, colContent, colCreated;

    private CommentListViewModel vm;

    @FXML
    public void initialize() {
        var ctx = AppBootstrap.start();
        vm = new CommentListViewModel(ctx.commentService);

        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleLongProperty(c.getValue().id()));
        colUser.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().commenterUsername()));
        colContent.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().content()));
        colCreated.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().createdAt() != null ? c.getValue().createdAt().toString() : ""));

        table.setItems(vm.data);

        loadBtn.setOnAction(e -> {
            try {
                vm.postId.set(Long.parseLong(postIdField.getText().trim()));
                vm.load();
            } catch (Exception ignored) {}
        });

        addBtn.setOnAction(e -> {
            if (vm.postId.get() <= 0) return;
            TextInputDialog d = new TextInputDialog();
            d.setHeaderText("Add comment...");
            d.showAndWait().ifPresent(txt -> {
                // TODO: pick current user from SecurityContext
                long uid = com.smartblog.application.security.SecurityContext.getUser().getId();
                ctx.commentService.add(vm.postId.get(), uid, txt);
                vm.load();
            });
        });

        editBtn.setOnAction(e -> {
            var sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            TextInputDialog d = new TextInputDialog(sel.content());
            d.setHeaderText("Edit comment...");
            d.showAndWait().ifPresent(txt -> {
                ctx.commentService.edit(sel.id(), txt);
                vm.load();
            });
        });

        deleteBtn.setOnAction(e -> {
            var sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (ConfirmDialogs.confirm("Delete", "Delete comment #" + sel.id() + "?")) {
                ctx.commentService.remove(sel.id());
                vm.load();
            }
        });
    }
}

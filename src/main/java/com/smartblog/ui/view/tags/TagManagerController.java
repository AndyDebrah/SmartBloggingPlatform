
package com.smartblog.ui.view.tags;

import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.dto.TagDTO;
import com.smartblog.ui.navigation.NavigationService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class TagManagerController implements com.smartblog.ui.navigation.ParamReceiver {
    @FXML private TextField newTagField, postIdField;
    @FXML private Button createBtn, assignBtn, removeBtn, backBtn;
    @FXML private ListView<TagDTO> allTags, postTags;

    private TagManagerViewModel vm;

    @FXML
    public void initialize() {
        var ctx = AppBootstrap.start();
        vm = new TagManagerViewModel(ctx.tagService);

        // display only tag name in lists
        allTags.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TagDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        postTags.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TagDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        allTags.setItems(vm.all);
        postTags.setItems(vm.ofPost);

        vm.loadAll();

        System.out.println("TagManagerController.initialize - vm.postId=" + vm.postId.get());

        // disable assign/remove when no post selected or no selection
        assignBtn.disableProperty().bind(vm.postId.lessThanOrEqualTo(0).or(javafx.beans.binding.Bindings.isNull(allTags.getSelectionModel().selectedItemProperty())));
        removeBtn.disableProperty().bind(vm.postId.lessThanOrEqualTo(0).or(javafx.beans.binding.Bindings.isNull(postTags.getSelectionModel().selectedItemProperty())));

        createBtn.setOnAction(e -> {
            String name = newTagField.getText().trim();
            if (name.isBlank()) return;
            vm.create(name);
            newTagField.clear();
            vm.loadAll();
            vm.loadForPost();
        });

        postIdField.setOnAction(e -> {
            try { vm.postId.set(Long.parseLong(postIdField.getText().trim())); }
            catch (Exception ignored) { vm.postId.set(0); }
            vm.loadForPost();
        });

        assignBtn.setOnAction(e -> {
            var sel = allTags.getSelectionModel().getSelectedItem();
            if (sel == null || vm.postId.get() <= 0) return;
            System.out.println("TagManager.assign -> postId=" + vm.postId.get() + " tagId=" + sel.id());
            boolean ok = vm.assign(sel.id());
            System.out.println("TagManager.assign result=" + ok);
            vm.loadForPost();
        });

        removeBtn.setOnAction(e -> {
            var sel = postTags.getSelectionModel().getSelectedItem();
            if (sel == null || vm.postId.get() <= 0) return;
            System.out.println("TagManager.remove -> postId=" + vm.postId.get() + " tagId=" + sel.id());
            boolean ok = vm.remove(sel.id());
            System.out.println("TagManager.remove result=" + ok);
            vm.loadForPost();
        });

        backBtn.setOnAction(e -> {
            System.out.println("TagManager.back pressed");
            NavigationService.navigateBack();
        });
    }

    @Override
    public void setParams(com.smartblog.ui.navigation.ViewParams params) {
        if (params == null) return;
        Object pid = params.get("postId");
        if (pid instanceof Number) {
            vm.postId.set(((Number) pid).longValue());
            vm.loadForPost();
        } else if (pid instanceof String) {
            try { vm.postId.set(Long.parseLong((String) pid)); vm.loadForPost(); } catch (Exception ignored) {}
        }
    }
}


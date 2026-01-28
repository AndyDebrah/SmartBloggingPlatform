package com.smartblog.ui.view.posts.dialog;

import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.exceptions.NotAuthorizedException;
import com.smartblog.ui.components.UiExceptionHandler;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PostDialogController {

    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label statusLabel;

    private PostDialogViewModel vm;

    @FXML
    public void initialize() {
        var ctx = AppBootstrap.start();
        vm = new PostDialogViewModel(ctx.postService);

        titleField.textProperty().bindBidirectional(vm.title);
        contentArea.textProperty().bindBidirectional(vm.content);

        saveBtn.setOnAction(e -> {
            try {
                vm.save();
                close();
            } catch (NotAuthorizedException nae) {
                UiExceptionHandler.showAuthError(nae.getMessage());
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> close());
    }

    public void load(PostDTO dto) {
        vm.load(dto.id(), dto.title(), dto.content());
    }

    private void close() {
        Stage s = (Stage) saveBtn.getScene().getWindow();
        s.close();
    }
}
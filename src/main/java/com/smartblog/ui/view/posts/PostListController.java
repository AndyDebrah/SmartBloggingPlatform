package com.smartblog.ui.view.posts;

import java.io.IOException;
import java.util.List;

import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.ui.navigation.NavigationService;
import com.smartblog.ui.view.posts.dialog.PostDialogController;
import com.smartblog.application.security.SecurityContext;
import com.smartblog.ui.components.UiExceptionHandler;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PostListController {

    @FXML private TextField searchField;
    @FXML private Button newBtn;
    @FXML private Button backBtn;
    @FXML private TableView<PostDTO> postsTable;
    @FXML private TableColumn<PostDTO, Long> idCol;
    @FXML private TableColumn<PostDTO, String> titleCol;
    @FXML private TableColumn<PostDTO, String> authorCol;
    @FXML private TableColumn<PostDTO, Boolean> publishedCol;
    @FXML private TableColumn<PostDTO, Void> actionsCol;

    private final ObservableList<PostDTO> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        var ctx = AppBootstrap.start();

        idCol.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().id()));
        titleCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().title()));
        authorCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().authorUsername()));
        publishedCol.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().published()));

        postsTable.setItems(data);

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button del = new Button("Delete");
            private final HBox box = new HBox(6, edit, del);

            {
                edit.setOnAction(e -> {
                    var dto = getCurrentDto();
                    if (dto != null) openDialog(dto);
                });
                del.setOnAction(e -> {
                    var dto = getCurrentDto();
                    if (dto == null) return;
                    try {
                        ctx.postService.softDelete(dto.id());
                        refresh();
                    } catch (Exception ex) {
                        UiExceptionHandler.showAuthError(ex.getMessage() != null ? ex.getMessage() : "Not authorized");
                    }
                });
            }

            private PostDTO getCurrentDto() {
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(idx);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                PostDTO dto = getCurrentDto();
                if (dto == null) { setGraphic(null); return; }

                boolean allowed = SecurityContext.isAdmin();
                if (!allowed) {
                    var user = SecurityContext.getUser();
                    try {
                        var op = AppBootstrap.start().postService.getDomain(dto.id());
                        if (op.isPresent() && user != null && user.getId() != null && user.getId().equals(op.get().getAuthorId())) {
                            allowed = true;
                        }
                    } catch (Exception ignored) {}
                }
                edit.setVisible(allowed);
                del.setVisible(allowed);

                setGraphic(box);
            }
        });

        newBtn.setOnAction(e -> openDialog(null));
        backBtn.setOnAction(e -> NavigationService.navigateBack());

        searchField.setOnAction(e -> refresh());

        refresh();
    }

    private void refresh() {
        var ctx = AppBootstrap.start();
        String q = searchField.getText();
        List<PostDTO> list = (q == null || q.isBlank()) ? ctx.postService.list(0, 100) : ctx.postService.search(q, 0, 100);
        List<PostDTO> published = list.stream().filter(PostDTO::published).toList();
        data.setAll(published);
    }

    private void openDialog(PostDTO dto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smartblog/ui/view/posts/dialog/PostDialog.fxml"));
            javafx.scene.Parent root = loader.load();
            PostDialogController ctrl = loader.getController();
            if (dto != null) ctrl.load(dto);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(dto == null ? "New Post" : "Edit Post");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
            refresh();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

package com.smartblog.ui.view.posts.editor;

import com.smartblog.application.security.SecurityContext;
import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.dto.TagDTO;
import com.smartblog.core.model.Post;
import com.smartblog.ui.components.UiExceptionHandler;
import com.smartblog.ui.navigation.NavigationService;
import com.smartblog.ui.navigation.View;
import com.smartblog.ui.navigation.ViewParams;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;

public class PostEditorController implements com.smartblog.ui.navigation.ParamReceiver {

    @FXML private TextField titleField;
    @FXML private HTMLEditor htmlEditor;
    @FXML private WebView previewPane;
    @FXML private Button saveDraftBtn;
    @FXML private Button backBtn;
    @FXML private Button publishBtn;
    @FXML private Button tagBtn;
    @FXML private Label autosaveLabel;
    @FXML private TextField tagInput;
    @FXML private ListView<String> tagSuggestions;
    @FXML private javafx.scene.layout.FlowPane assignedTagsPane;

    private PostEditorViewModel vm;

    @FXML
    public void initialize() {
        updatePublishVisibility();
        var ctx = AppBootstrap.start();
        vm = new PostEditorViewModel(ctx.postService, ctx.tagService);

        titleField.textProperty().bindBidirectional(vm.title);
        vm.htmlContent.addListener((obs, oldVal, newVal) -> htmlEditor.setHtmlText(newVal));
        htmlEditor.setOnMouseReleased(e -> vm.htmlContent.set(htmlEditor.getHtmlText()));
        autosaveLabel.textProperty().bind(vm.autosaveMessage);

        vm.bindPreview(previewPane);

        // Back navigation
        if (backBtn != null) backBtn.setOnAction(e -> NavigationService.navigateBack());

        // Disable publish until title and content present
        publishBtn.disableProperty().bind(vm.title.isEmpty().or(vm.htmlContent.isEmpty()));

        saveDraftBtn.setOnAction(e -> safe(() -> {
            vm.htmlContent.set(htmlEditor.getHtmlText());
            vm.saveDraft();
        }));
        publishBtn.setOnAction(e -> safe(() -> {
            vm.htmlContent.set(htmlEditor.getHtmlText());
            vm.publishPost();
        }));
        tagBtn.setOnAction(e -> openTagManager());

        // Tag input suggestions and actions
        tagSuggestions.setVisible(false);
        tagSuggestions.managedProperty().bind(tagSuggestions.visibleProperty());

        // Debounced suggestions
        javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
        tagInput.textProperty().addListener((obs, oldV, newV) -> {
            debounce.stop();
            debounce.setOnFinished(ev -> {
                try {
                    var suggestions = vm.suggestTags(newV);
                    var names = suggestions.stream().map(TagDTO::name).toList();
                    tagSuggestions.getItems().setAll(names);
                    tagSuggestions.setVisible(!names.isEmpty());
                } catch (Exception ignored) {}
            });
            debounce.playFromStart();
        });

        tagInput.setOnAction(e -> {
            String t = tagInput.getText();
            safe(() -> { vm.addTagByName(t); tagInput.clear(); tagSuggestions.getItems().clear(); tagSuggestions.setVisible(false); });
        });

        tagInput.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DOWN) {
                if (!tagSuggestions.getItems().isEmpty()) {
                    tagSuggestions.requestFocus();
                    tagSuggestions.getSelectionModel().selectFirst();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                tagInput.clear();
                tagSuggestions.getItems().clear();
                tagSuggestions.setVisible(false);
            }
        });

        tagSuggestions.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                var sel = tagSuggestions.getSelectionModel().getSelectedItem();
                if (sel != null) safe(() -> { vm.addTagByName(sel); tagInput.clear(); tagSuggestions.getItems().clear(); tagSuggestions.setVisible(false); });
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                if (tagSuggestions.getSelectionModel().getSelectedIndex() == 0) {
                    tagInput.requestFocus();
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                tagInput.requestFocus();
                tagSuggestions.setVisible(false);
            }
        });

        tagSuggestions.setOnMouseClicked(e -> {
            var sel = tagSuggestions.getSelectionModel().getSelectedItem();
            if (sel != null) safe(() -> { vm.addTagByName(sel); tagInput.clear(); tagSuggestions.getItems().clear(); tagSuggestions.setVisible(false); });
        });

        // render assigned tags
        vm.assignedTags.addListener((javafx.collections.ListChangeListener<String>) c -> renderAssignedTags());
        renderAssignedTags();

        // Add Ctrl+S accelerator for quick save (register when scene is available)
        titleField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                KeyCombination kc = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
                newScene.getAccelerators().put(kc, () -> safe(() -> {
                    vm.htmlContent.set(htmlEditor.getHtmlText());
                    vm.saveDraft();
                }));
            }
        });
    }

    private void safe(Runnable r) {
        try { r.run(); }
        catch (Exception ex) {
            if (ex instanceof com.smartblog.core.exceptions.NotAuthorizedException) {
                UiExceptionHandler.showAuthError(ex.getMessage());
                return;
            }
            if (vm != null) {
                vm.autosaveMessage.set("Error: " + ex.getMessage());
            } else {
                javafx.application.Platform.runLater(() -> autosaveLabel.setText("Error: " + ex.getMessage()));
            }
        }
    }

    public void loadPost(Post p) {
        vm.loadPost(p);
    }

    private void renderAssignedTags() {
        if (assignedTagsPane == null || vm == null) return;
        javafx.application.Platform.runLater(() -> {
            assignedTagsPane.getChildren().clear();
            for (String t : vm.assignedTags) {
                javafx.scene.control.Label lbl = new javafx.scene.control.Label(t);
                lbl.getStyleClass().addAll("tag-pill");
                javafx.scene.control.Button remove = new javafx.scene.control.Button("✕");
                remove.getStyleClass().addAll("tag-remove");
                remove.setOnAction(e -> safe(() -> vm.removeTagByName(t)));
                javafx.scene.layout.HBox h = new javafx.scene.layout.HBox(6, lbl, remove);
                h.getStyleClass().addAll("tag-item");
                assignedTagsPane.getChildren().add(h);
            }
        });
    }

    private void updatePublishVisibility() {
        boolean isAdmin = SecurityContext.isAdmin();
        if(publishBtn != null) {
            publishBtn.setVisible(isAdmin);
            publishBtn.setManaged(isAdmin);
            publishBtn.setDisable(!isAdmin);
        }
    }

    public void publishPost() {
        updatePublishVisibility();
    }

    private void openTagManager() {
        Long id = vm.getPostId();
        if (id == null) {
            if (vm != null) vm.autosaveMessage.set("Save draft before assigning tags.");
            else autosaveLabel.setText("Save draft before assigning tags.");
            return;
        }
        ViewParams params = new ViewParams();
        params.put("postId", id);
        NavigationService.navigate(View.TAG_MANAGER, params);
    }

    @Override
    public void setParams(com.smartblog.ui.navigation.ViewParams params) {
        if (params == null) return;
        Object pid = params.get("postId");
        Long id = null;
        if (pid instanceof Number) id = ((Number) pid).longValue();
        else if (pid instanceof String) {
            try { id = Long.parseLong((String) pid); } catch (Exception ignored) {}
        }
        if (id != null) {
            var ctx = AppBootstrap.start();
            ctx.postService.getDomain(id).ifPresent(this::loadPost);
        }
    }
}
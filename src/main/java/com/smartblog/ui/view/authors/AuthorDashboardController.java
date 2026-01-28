package com.smartblog.ui.view.authors;

import java.util.List;

import com.smartblog.application.security.SecurityContext;
import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.dto.CommentDTO;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.model.User;
import com.smartblog.ui.components.UiExceptionHandler;
import com.smartblog.ui.navigation.NavigationService;
import com.smartblog.ui.navigation.View;
import com.smartblog.ui.navigation.ViewParams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AuthorDashboardController {

    @FXML private Label heading;
    @FXML private ListView<PostDTO> postsList;
    @FXML private ListView<PostDTO> postsFullList;
    @FXML private Button backBtn;
    @FXML private Button newBtn;
    @FXML private Button refreshBtn;

    // Search panel fields
    @FXML private TextField searchKeywordField;
    @FXML private ComboBox<String> searchTagCombo;
    @FXML private TextField searchAuthorField;
    @FXML private ComboBox<String> searchSortCombo;
    @FXML private Button searchExecuteBtn;
    @FXML private Button searchClearBtn;

    // New bindings for improved UI
    @FXML private Label selectedTitle;
    @FXML private Label selectedMeta;
    @FXML private Label selectedContent;
    @FXML private Label draftsCount;
    @FXML private Label publishedCount;
    @FXML private ListView<String> topTagsList;

    private final ObservableList<PostDTO> data = FXCollections.observableArrayList();
    private final ObservableList<CommentDTO> comments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        heading.setText("Author Dashboard — your posts & reviews");
        // Load author-specific stylesheet once the scene is ready
        heading.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                try {
                    String css = getClass().getResource("/com/smartblog/ui/themes/author-overrides.css").toExternalForm();
                    newScene.getStylesheets().add(css);
                } catch (Exception ex) {
                    System.err.println("Failed to load author-overrides.css: " + ex.getMessage());
                }
            }
        });
        
        // Setup search controls
        setupSearchControls();
        
        postsList.setItems(data);
        postsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PostDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    int commentsCount = AppBootstrap.start().commentService.listForPost(item.id(), 0, 100).size();
                    setText((item.published() ? "[P] " : "[D] ") + item.title() + " — " + commentsCount + " comments");
                }
            }
        });

        // full posts list (main content) - each cell shows professional post card
        postsFullList.setItems(data);
        postsFullList.setCellFactory(lv -> new ListCell<>() {
            private final VBox postCard = new VBox(12);
            private final Label titleLbl = new Label();
            private final Label metaLbl = new Label();
            private final javafx.scene.web.WebView contentView = new javafx.scene.web.WebView();
            private final javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
            
            // Icon-style action buttons below post
            private final HBox iconBar = new HBox(12);
            private final Button editBtn = new Button("✏️ Edit");
            private final Button publishBtn = new Button("📢 Publish");
            private final Button commentIconBtn = new Button("💬 Comment");
            private final Button viewCommentsBtn = new Button("👁 View Comments");
            private final Label commentCountLbl = new Label("0 comments");
            
            // Collapsible comment input area
            private final VBox commentInputArea = new VBox(8);
            private final javafx.scene.control.TextArea commentTextArea = new javafx.scene.control.TextArea();
            private final HBox commentActions = new HBox(8);
            private final Button sendBtn = new Button("Send");
            private final Button cancelBtn = new Button("Cancel");
            
            // Collapsible comments display
            private final VBox commentsDisplayArea = new VBox(8);
            private final VBox commentsBox = new VBox(6);

            {
                // Post card styling
                postCard.getStyleClass().add("post-card");
                postCard.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 16; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 8;");
                
                titleLbl.getStyleClass().add("heading-3");
                titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");
                metaLbl.getStyleClass().add("muted");
                metaLbl.setStyle("-fx-font-size: 12px; -fx-padding: 0 0 8 0;");
                contentView.setPrefHeight(140);
                contentView.setMaxHeight(140);
                contentView.getStyleClass().add("post-content");
                
                // CRITICAL: Make WebView background dark to match theme
                contentView.setStyle("-fx-background-color: #2b2d30; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 4;");
                contentView.getEngine().setUserStyleSheetLocation("data:,body{background-color:%232b2d30!important;color:%23e0e0e0!important;margin:12px;padding:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;font-size:14px;line-height:1.6;}p{margin:0 0 8px 0;}h1,h2,h3{margin:0 0 8px 0;color:%23f0f0f0;}a{color:%2364b5f6;text-decoration:none;}code{background:rgba(255,255,255,0.05);padding:2px 6px;border-radius:3px;}pre{background:rgba(255,255,255,0.05);padding:12px;border-radius:4px;overflow-x:auto;}");
                
                // Icon bar setup
                editBtn.getStyleClass().addAll("btn", "btn-secondary");
                editBtn.setStyle("-fx-font-size: 12px;");
                publishBtn.getStyleClass().addAll("btn", "btn-primary");
                publishBtn.setStyle("-fx-font-size: 12px;");
                commentIconBtn.getStyleClass().addAll("btn", "btn-icon");
                viewCommentsBtn.getStyleClass().addAll("btn", "btn-icon");
                commentCountLbl.getStyleClass().add("muted");
                commentCountLbl.setStyle("-fx-font-size: 11px;");
                Region spacer1 = new Region();
                HBox.setHgrow(spacer1, javafx.scene.layout.Priority.ALWAYS);
                iconBar.getChildren().addAll(editBtn, publishBtn, commentIconBtn, viewCommentsBtn, spacer1, commentCountLbl);
                iconBar.setStyle("-fx-padding: 8 0 0 0;");
                
                // Comment input area setup
                commentTextArea.setPromptText("Write your comment...");
                commentTextArea.setPrefRowCount(3);
                commentTextArea.setWrapText(true);
                sendBtn.getStyleClass().addAll("btn", "btn-primary");
                cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
                commentActions.getChildren().addAll(sendBtn, cancelBtn);
                commentInputArea.getChildren().addAll(commentTextArea, commentActions);
                commentInputArea.setVisible(false);
                commentInputArea.setManaged(false);
                commentInputArea.setStyle("-fx-padding: 8 0 0 0;");
                
                // Comments display area setup
                commentsDisplayArea.getChildren().add(commentsBox);
                commentsDisplayArea.setVisible(false);
                commentsDisplayArea.setManaged(false);
                commentsDisplayArea.setStyle("-fx-padding: 12; -fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 6;");
                
                // Assemble card
                postCard.getChildren().addAll(titleLbl, metaLbl, contentView, sep, iconBar, commentInputArea, commentsDisplayArea);
                
                // Edit button action - navigate to editor with post ID
                editBtn.setOnAction(e -> {
                    PostDTO p = getItem();
                    if (p != null) {
                        NavigationService.navigate(View.POST_EDITOR, new ViewParams().put("postId", p.id()));
                    }
                });
                
                // Publish button action - admin can publish any post
                publishBtn.setOnAction(e -> {
                    PostDTO p = getItem();
                    if (p == null) return;
                    try {
                        var ctx = AppBootstrap.start();
                        ctx.postService.publish(p.id());
                        loadData(); // Refresh to show updated status
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("Post Published");
                        alert.setContentText("The post has been published successfully.");
                        alert.showAndWait();
                    } catch (Exception ex) {
                        UiExceptionHandler.showError("Publish Error", ex.getMessage());
                    }
                });
                
                // Comment icon action - toggle input area
                commentIconBtn.setOnAction(e -> {
                    boolean show = !commentInputArea.isVisible();
                    commentInputArea.setVisible(show);
                    commentInputArea.setManaged(show);
                    if (show) commentTextArea.requestFocus();
                });
                
                // View comments action - toggle display area
                viewCommentsBtn.setOnAction(e -> {
                    boolean show = !commentsDisplayArea.isVisible();
                    commentsDisplayArea.setVisible(show);
                    commentsDisplayArea.setManaged(show);
                    if (show) {
                        PostDTO p = getItem();
                        if (p != null) loadCommentsForPost(p);
                    }
                });
                
                // Send comment action
                sendBtn.setOnAction(e -> {
                    PostDTO p = getItem();
                    if (p == null) return;
                    String text = commentTextArea.getText();
                    if (text == null || text.isBlank()) return;
                    var ctx = AppBootstrap.start();
                    User cur = SecurityContext.getUser();
                    if (cur == null) { UiExceptionHandler.showAuthError("Login required to add comments."); return; }
                    try {
                        ctx.commentService.add(p.id(), cur.getId(), text);
                        commentTextArea.clear();
                        commentInputArea.setVisible(false);
                        commentInputArea.setManaged(false);
                        updateCommentCount(p);
                        if (commentsDisplayArea.isVisible()) loadCommentsForPost(p);
                    } catch (Exception ex) {
                        UiExceptionHandler.showError("Comment Error", ex.getMessage());
                    }
                });
                
                // Cancel action
                cancelBtn.setOnAction(e -> {
                    commentTextArea.clear();
                    commentInputArea.setVisible(false);
                    commentInputArea.setManaged(false);
                });
            }
            
            private void updateCommentCount(PostDTO p) {
                try {
                    int count = AppBootstrap.start().commentService.listForPost(p.id(), 0, 1000).size();
                    commentCountLbl.setText(count + (count == 1 ? " comment" : " comments"));
                } catch (Exception ignored) {}
            }

            private void loadCommentsForPost(PostDTO p) {
                try {
                    var list = AppBootstrap.start().commentService.listForPost(p.id(), 0, 200);
                    commentsBox.getChildren().clear();
                    if (list.isEmpty()) {
                        Label noComments = new Label("No comments yet. Be the first to comment!");
                        noComments.getStyleClass().add("muted");
                        commentsBox.getChildren().add(noComments);
                    } else {
                        for (CommentDTO c : list) {
                            VBox commentItem = new VBox(4);
                            commentItem.setStyle("-fx-padding: 8; -fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 4;");
                            
                            Label authorLbl = new Label(c.commenterUsername());
                            authorLbl.setStyle("-fx-font-weight: 600; -fx-font-size: 12px;");
                            
                            Label contentLbl = new Label(c.content());
                            contentLbl.setWrapText(true);
                            contentLbl.setStyle("-fx-font-size: 13px;");
                            
                            HBox commentFooter = new HBox(8);
                            Label timeLbl = new Label(c.createdAt() != null ? c.createdAt().toString() : "");
                            timeLbl.getStyleClass().add("muted");
                            timeLbl.setStyle("-fx-font-size: 10px;");
                            
                            Button reviewBtn = new Button("✓ Review");
                            reviewBtn.getStyleClass().addAll("btn", "btn-icon");
                            reviewBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 8;");
                            reviewBtn.setOnAction(ev -> {
                                if (contentLbl.getStyleClass().contains("muted")) {
                                    contentLbl.getStyleClass().remove("muted");
                                    reviewBtn.setText("✓ Review");
                                } else {
                                    contentLbl.getStyleClass().add("muted");
                                    reviewBtn.setText("✓ Reviewed");
                                }
                            });
                            
                            Region spacer2 = new Region();
                            HBox.setHgrow(spacer2, javafx.scene.layout.Priority.ALWAYS);
                            commentFooter.getChildren().addAll(timeLbl, spacer2, reviewBtn);
                            
                            commentItem.getChildren().addAll(authorLbl, contentLbl, commentFooter);
                            commentsBox.getChildren().add(commentItem);
                        }
                    }
                } catch (Exception ex) {
                    UiExceptionHandler.showError("Comments", ex.getMessage());
                }
            }

            @Override
            protected void updateItem(PostDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { 
                    setGraphic(null); 
                    setText(null); 
                } else {
                    titleLbl.setText(item.title());
                    metaLbl.setText((item.published() ? "📢 Published" : "📝 Draft") + " • by " + item.authorUsername());
                    String content = item.content() == null ? "" : item.content();
                    String safe = content.replaceAll("(?i)<script.*?>.*?</script>", "");
                    
                    // Load content - CSS styling is applied via user stylesheet set in initialization
                    try {
                        contentView.getEngine().loadContent(safe, "text/html");
                    } catch (Exception ex) {
                        contentView.getEngine().loadContent("<pre>" + safe.replaceAll("<", "&lt;") + "</pre>", "text/html");
                    }
                    updateCommentCount(item);
                    
                    // Show edit button only for posts owned by current user
                    User currentUser = SecurityContext.getUser();
                    boolean canEdit = currentUser != null && item.authorUsername().equals(currentUser.getUsername());
                    editBtn.setVisible(canEdit);
                    editBtn.setManaged(canEdit);
                    
                    // Show publish button only for admins on unpublished posts
                    boolean isAdmin = SecurityContext.isAdmin();
                    boolean canPublish = isAdmin && !item.published();
                    publishBtn.setVisible(canPublish);
                    publishBtn.setManaged(canPublish);
                    
                    setGraphic(postCard);
                    commentInputArea.setVisible(false);
                    commentInputArea.setManaged(false);
                    commentsDisplayArea.setVisible(false);
                    commentsDisplayArea.setManaged(false);
                }
            }
        });

        // comments are shown inline per-post in the full posts list; no standalone commentsList

        postsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> loadCommentsForSelected());

        backBtn.setOnAction(e -> NavigationService.navigate(View.MAIN));
        newBtn.setOnAction(e -> NavigationService.navigate(View.POST_EDITOR));
        refreshBtn.setOnAction(e -> loadData());

        // inline comment controls live inside each post cell; no global comment input

        loadData();
    }

    private void setupSearchControls() {
        // Populate sort options
        searchSortCombo.setItems(FXCollections.observableArrayList(
            "Newest First",
            "Oldest First", 
            "Title A-Z",
            "Title Z-A",
            "By Author"
        ));
        searchSortCombo.setValue("Newest First");
        
        // Load tags into combo
        try {
            var ctx = AppBootstrap.start();
            List<String> tagNames = ctx.tagService.list().stream()
                .map(tag -> tag.name())
                .sorted()
                .toList();
            searchTagCombo.setItems(FXCollections.observableArrayList(tagNames));
        } catch (Exception e) {
            System.err.println("Failed to load tags for search: " + e.getMessage());
        }
        
        // Wire up buttons
        searchExecuteBtn.setOnAction(e -> performSearch());
        searchClearBtn.setOnAction(e -> clearSearch());
    }

    private void performSearch() {
        String keyword = searchKeywordField.getText();
        String tag = searchTagCombo.getValue();
        String author = searchAuthorField.getText();
        String sortBy = getSortByValue(searchSortCombo.getValue());
        
        try {
            var ctx = AppBootstrap.start();
            List<PostDTO> results;
            
            // If all filters are empty, show all posts
            if ((keyword == null || keyword.isBlank()) && 
                (tag == null) && 
                (author == null || author.isBlank())) {
                results = ctx.postService.list(1, 200);
            } else {
                // Use combined search - note: parameter order is keyword, author, tag, sortBy
                results = ctx.postService.searchCombined(keyword, author, tag, sortBy, 1, 200);
            }
            
            data.setAll(results);
            
            System.out.println("Search completed. Found " + results.size() + " posts.");
            
            // Update stats for current user
            User cur = SecurityContext.getUser();
            if (cur != null && draftsCount != null && publishedCount != null) {
                List<PostDTO> myPosts = results.stream()
                    .filter(p -> p.authorUsername().equals(cur.getUsername()))
                    .toList();
                draftsCount.setText(String.valueOf(myPosts.stream().filter(p -> !p.published()).count()));
                publishedCount.setText(String.valueOf(myPosts.stream().filter(PostDTO::published).count()));
            }
        } catch (Exception ex) {
            UiExceptionHandler.showError("Search Error", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String getSortByValue(String displayValue) {
        return switch (displayValue) {
            case "Oldest First" -> "date_asc";
            case "Title A-Z" -> "title_asc";
            case "Title Z-A" -> "title_desc";
            case "By Author" -> "author";
            default -> "date_desc"; // Newest First
        };
    }

    private void clearSearch() {
        searchKeywordField.clear();
        searchTagCombo.setValue(null);
        searchAuthorField.clear();
        searchSortCombo.setValue("Newest First");
        loadData(); // Reload all posts
    }

    private void loadCommentsForSelected() {
        var sel = postsList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            comments.clear();
            if (selectedTitle != null) selectedTitle.setText("Select a post");
            if (selectedContent != null) selectedContent.setText("");
            if (selectedMeta != null) selectedMeta.setText("");
            return;
        }
        var ctx = AppBootstrap.start();
        List<CommentDTO> list = ctx.commentService.listForPost(sel.id(), 0, 200);
        comments.setAll(list);
        if (selectedTitle != null) selectedTitle.setText(sel.title());
        if (selectedContent != null) selectedContent.setText(sel.content() == null ? "" : sel.content());
        if (selectedMeta != null) selectedMeta.setText(sel.published() ? "Published" : "Draft");
    }

    private void loadData() {
        var ctx = AppBootstrap.start();
        User cur = SecurityContext.getUser();
        if (cur == null) {
            data.clear();
            return;
        }
        // Load ALL posts (not just current author's) so users can see all content
        List<PostDTO> allPosts = ctx.postService.list(0, 200);
        data.setAll(allPosts);
        
        // Get current user's posts for stats
        List<PostDTO> myPosts = allPosts.stream()
            .filter(p -> p.authorUsername().equals(cur.getUsername()))
            .toList();
        // select first post to show preview
        if (!allPosts.isEmpty()) {
            postsList.getSelectionModel().select(0);
            PostDTO first = allPosts.get(0);
            if (selectedTitle != null) selectedTitle.setText(first.title());
            if (selectedContent != null) selectedContent.setText(first.content() == null ? "" : first.content());
            if (selectedMeta != null) selectedMeta.setText(first.published() ? "Published" : "Draft");
        }

        // quick stats
        if (draftsCount != null) draftsCount.setText("Drafts: " + myPosts.stream().filter(p -> !p.published()).count());
        if (publishedCount != null) publishedCount.setText("Published: " + myPosts.stream().filter(PostDTO::published).count());

        // top tags (simple aggregate)
        if (topTagsList != null) {
            try {
                var tags = ctx.tagService.listAll().stream().map(t -> t.name()).toList();
                topTagsList.getItems().setAll(tags);
            } catch (Exception ignored) { }
        }
    }
}
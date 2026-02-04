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

/**
 * Controller for the Author Dashboard view.
 * Provides listing, searching and inline comment interactions for authors.
 */
public class AuthorDashboardController {

    @FXML private Label heading;
    @FXML private ListView<PostDTO> postsList;
    @FXML private ListView<PostDTO> postsFullList;
    @FXML private Button backBtn;
    @FXML private Button newBtn;
    @FXML private Button refreshBtn;

    @FXML private TextField searchKeywordField;
    @FXML private ComboBox<String> searchTagCombo;
    @FXML private TextField searchAuthorField;
    @FXML private ComboBox<String> searchSortCombo;
    @FXML private Button searchExecuteBtn;
    @FXML private Button searchClearBtn;

    // UI bindings
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

        // full posts list (main content)
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
                postCard.setStyle("-fx-background-color: linear-gradient(180deg, #1e222a 0%, #1a1d23 100%); -fx-padding: 20; -fx-background-radius: 16; -fx-border-color: #374151; -fx-border-radius: 16; -fx-border-width: 1;");
                
                titleLbl.getStyleClass().add("heading-3");
                titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #ffffff;");
                metaLbl.getStyleClass().add("muted");
                metaLbl.setStyle("-fx-font-size: 13px; -fx-padding: 4 0 12 0; -fx-text-fill: #94a3b8;");
                contentView.setPrefHeight(160);
                contentView.setMaxHeight(160);
                contentView.getStyleClass().add("post-content");
                
                contentView.setStyle("-fx-background-color: #1a1d23; -fx-border-color: #374151; -fx-border-radius: 8; -fx-border-width: 1;");
                contentView.getEngine().setUserStyleSheetLocation("data:,body{background-color:%231a1d23!important;color:%23ffffff!important;margin:16px;padding:0;font-family:'Segoe UI','Inter',system-ui,sans-serif;font-size:15px;line-height:1.7;}p{margin:0 0 12px 0;color:%23ffffff;}h1,h2,h3{margin:0 0 12px 0;color:%23ffffff;font-weight:700;}h1{font-size:24px;}h2{font-size:20px;}h3{font-size:16px;}a{color:%236366f1;text-decoration:none;}a:hover{text-decoration:underline;}code{background:%232a2f3a;color:%23a5b4fc;padding:3px 8px;border-radius:4px;font-size:13px;}pre{background:%232a2f3a;color:%23e2e8f0;padding:16px;border-radius:8px;overflow-x:auto;font-size:13px;line-height:1.5;}ul,ol{color:%23ffffff;padding-left:24px;}li{margin-bottom:6px;}blockquote{border-left:3px solid %236366f1;padding-left:16px;margin:12px 0;color:%2394a3b8;}strong{color:%23ffffff;font-weight:600;}em{color:%23e2e8f0;}");
                
                // Icon bar setup
                editBtn.getStyleClass().addAll("btn", "btn-secondary");
                editBtn.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffffff;");
                publishBtn.getStyleClass().addAll("btn", "btn-primary");
                publishBtn.setStyle("-fx-font-size: 13px;");
                commentIconBtn.getStyleClass().addAll("btn", "btn-icon");
                commentIconBtn.setStyle("-fx-text-fill: #e2e8f0;");
                viewCommentsBtn.getStyleClass().addAll("btn", "btn-icon");
                viewCommentsBtn.setStyle("-fx-text-fill: #e2e8f0;");
                commentCountLbl.getStyleClass().add("muted");
                commentCountLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
                Region spacer1 = new Region();
                HBox.setHgrow(spacer1, javafx.scene.layout.Priority.ALWAYS);
                iconBar.getChildren().addAll(editBtn, publishBtn, commentIconBtn, viewCommentsBtn, spacer1, commentCountLbl);
                iconBar.setStyle("-fx-padding: 12 0 0 0;");
                
                // Comment input area setup
                commentTextArea.setPromptText("Write your comment...");
                commentTextArea.setPrefRowCount(3);
                commentTextArea.setWrapText(true);
                commentTextArea.setStyle("-fx-text-fill: #ffffff; -fx-background-color: #1e222a; -fx-border-color: #4b5563;");
                sendBtn.getStyleClass().addAll("btn", "btn-primary");
                cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
                cancelBtn.setStyle("-fx-text-fill: #ffffff;");
                commentActions.getChildren().addAll(sendBtn, cancelBtn);
                commentInputArea.getChildren().addAll(commentTextArea, commentActions);
                commentInputArea.setVisible(false);
                commentInputArea.setManaged(false);
                commentInputArea.setStyle("-fx-padding: 12 0 0 0;");
                
                // Comments display area setup
                commentsDisplayArea.getChildren().add(commentsBox);
                commentsDisplayArea.setVisible(false);
                commentsDisplayArea.setManaged(false);
                commentsDisplayArea.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(180deg, rgba(30,34,42,0.95) 0%, rgba(26,29,35,0.95) 100%); -fx-background-radius: 12; -fx-border-color: #374151; -fx-border-radius: 12; -fx-border-width: 1;");
                
                // Assemble card
                postCard.getChildren().addAll(titleLbl, metaLbl, contentView, sep, iconBar, commentInputArea, commentsDisplayArea);
                
                // Edit button action
                editBtn.setOnAction(e -> {
                    PostDTO p = getItem();
                    if (p != null) {
                        NavigationService.navigate(View.POST_EDITOR, new ViewParams().put("postId", p.id()));
                    }
                });
                
                // Publish button action
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
                
                // View comments action
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
                        noComments.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
                        commentsBox.getChildren().add(noComments);
                    } else {
                        for (CommentDTO c : list) {
                            VBox commentItem = new VBox(6);
                            commentItem.setStyle("-fx-padding: 14; -fx-background-color: linear-gradient(180deg, rgba(42,47,58,0.8) 0%, rgba(36,40,48,0.8) 100%); -fx-background-radius: 10; -fx-border-color: #374151; -fx-border-radius: 10; -fx-border-width: 1;");
                            
                            Label authorLbl = new Label(c.commenterUsername());
                            authorLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #ffffff;");
                            
                            Label contentLbl = new Label(c.content());
                            contentLbl.setWrapText(true);
                            contentLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #e2e8f0; -fx-padding: 4 0 8 0;");
                            
                            HBox commentFooter = new HBox(8);
                            Label timeLbl = new Label(c.createdAt() != null ? c.createdAt().toString() : "");
                            timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                            
                            Button reviewBtn = new Button("✓ Review");
                            reviewBtn.getStyleClass().addAll("btn", "btn-icon");
                            reviewBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 10; -fx-text-fill: #a5b4fc; -fx-background-color: rgba(99,102,241,0.15); -fx-border-color: rgba(99,102,241,0.3); -fx-background-radius: 6; -fx-border-radius: 6;");
                            reviewBtn.setOnAction(ev -> {
                                if (contentLbl.getStyle().contains("#64748b")) {
                                    contentLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #e2e8f0; -fx-padding: 4 0 8 0;");
                                    reviewBtn.setText("✓ Review");
                                    reviewBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 10; -fx-text-fill: #a5b4fc; -fx-background-color: rgba(99,102,241,0.15); -fx-border-color: rgba(99,102,241,0.3); -fx-background-radius: 6; -fx-border-radius: 6;");
                                } else {
                                    contentLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-padding: 4 0 8 0;");
                                    reviewBtn.setText("✓ Reviewed");
                                    reviewBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 10; -fx-text-fill: #86efac; -fx-background-color: rgba(34,197,94,0.15); -fx-border-color: rgba(34,197,94,0.3); -fx-background-radius: 6; -fx-border-radius: 6;");
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
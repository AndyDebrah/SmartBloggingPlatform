package com.smartblog.ui.view.admin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.ui.components.UiExceptionHandler;
import com.smartblog.ui.navigation.NavigationService;
import com.smartblog.ui.navigation.View;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AdminDashboardController {
    @FXML private Label statsLabel;
    @FXML private Label draftCountLabel;
    @FXML private ListView<String> topTagsList;
    @FXML private ListView<PostDTO> draftsList;
    @FXML private Button manageUsersBtn;
    @FXML private Button backBtn;
    @FXML private Button refreshBtn;
    @FXML private Button viewAllPostsBtn;
    @FXML private Button viewCommentsBtn;
    @FXML private Button viewTagsBtn;
    
    // Search controls
    @FXML private TextField searchKeywordField;
    @FXML private ComboBox<String> searchTagCombo;
    @FXML private TextField searchAuthorField;
    @FXML private ComboBox<String> searchSortCombo;
    @FXML private Button searchExecuteBtn;
    @FXML private Button searchClearBtn;
    
    private final ObservableList<String> topTags = FXCollections.observableArrayList();
    private final ObservableList<PostDTO> drafts = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        topTagsList.setItems(topTags);
        draftsList.setItems(drafts);
        
        // Back button
        if (backBtn != null) {
            backBtn.setOnAction(e -> NavigationService.navigate(View.MAIN));
        }
        
        // Refresh button
        if (refreshBtn != null) {
            refreshBtn.setOnAction(e -> loadData());
        }
        
        // Navigation buttons
        manageUsersBtn.setOnAction(e -> NavigationService.navigate(View.USERS));
        if (viewAllPostsBtn != null) {
            viewAllPostsBtn.setOnAction(e -> NavigationService.navigate(View.POSTS));
        }
        if (viewCommentsBtn != null) {
            viewCommentsBtn.setOnAction(e -> NavigationService.navigate(View.COMMENTS));
        }
        if (viewTagsBtn != null) {
            viewTagsBtn.setOnAction(e -> NavigationService.navigate(View.TAG_MANAGER));
        }
        
        // Setup drafts list with custom cells
        draftsList.setCellFactory(lv -> new ListCell<PostDTO>() {
            private final VBox card = new VBox(8);
            private final Label titleLabel = new Label();
            private final Label metaLabel = new Label();
            private final HBox actionBar = new HBox(12);
            private final Button publishBtn = new Button("📢 Publish");
            private final Button editBtn = new Button("✏️ Edit");
            private final Label statusLabel = new Label("📝 Draft");
            
            {
                // Card styling
                card.setStyle("-fx-background-color: #f8f9fc; -fx-padding: 16; -fx-background-radius: 8; -fx-border-color: #e8ecf4; -fx-border-radius: 8; -fx-border-width: 1;");
                
                titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #2d3748;");
                metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8b93a7;");
                statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #f5576c; -fx-background-color: rgba(245,87,108,0.1); -fx-padding: 4 8; -fx-background-radius: 4;");
                
                publishBtn.setStyle("-fx-background-color: #f5576c; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 8 16; -fx-background-radius: 6;");
                publishBtn.setOnAction(e -> {
                    PostDTO post = getItem();
                    if (post != null) {
                        publishPost(post);
                    }
                });
                
                editBtn.setStyle("-fx-background-color: white; -fx-text-fill: #2d3748; -fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-border-color: #e8ecf4; -fx-border-radius: 6;");
                editBtn.setOnAction(e -> {
                    PostDTO post = getItem();
                    if (post != null) {
                        NavigationService.navigate(View.POST_EDITOR, new com.smartblog.ui.navigation.ViewParams().put("postId", post.id()));
                    }
                });
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                actionBar.getChildren().addAll(statusLabel, spacer, editBtn, publishBtn);
                
                card.getChildren().addAll(titleLabel, metaLabel, actionBar);
            }
            
            @Override
            protected void updateItem(PostDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    titleLabel.setText(item.title());
                    metaLabel.setText("by " + item.authorUsername() + " • " + getCommentCount(item) + " comments");
                    setGraphic(card);
                }
            }
            
            private String getCommentCount(PostDTO post) {
                try {
                    int count = AppBootstrap.start().commentService.listForPost(post.id(), 0, 1000).size();
                    return String.valueOf(count);
                } catch (Exception e) {
                    return "0";
                }
            }
        });
        
        setupSearchControls();
        loadData();
    }
    
    private void setupSearchControls() {
        if (searchSortCombo == null) return;
        
        // Populate sort combo
        searchSortCombo.getItems().addAll(
            "Newest First",
            "Oldest First",
            "Title A-Z",
            "Title Z-A",
            "By Author"
        );
        searchSortCombo.getSelectionModel().selectFirst();
        
        // Load tags for tag filter
        try {
            var ctx = AppBootstrap.start();
            List<String> tagNames = ctx.tagService.list().stream()
                .map(t -> t.name())
                .sorted()
                .toList();
            searchTagCombo.getItems().add("All Tags");
            searchTagCombo.getItems().addAll(tagNames);
            searchTagCombo.getSelectionModel().selectFirst();
        } catch (Exception e) {
            System.err.println("Failed to load tags: " + e.getMessage());
        }
        
        // Wire search button
        searchExecuteBtn.setOnAction(e -> performSearch());
        
        // Wire clear button
        searchClearBtn.setOnAction(e -> clearSearch());
    }
    
    private void performSearch() {
        try {
            var ctx = AppBootstrap.start();
            
            String keyword = searchKeywordField.getText().trim();
            String tag = searchTagCombo.getValue();
            String author = searchAuthorField.getText().trim();
            String sortBy = getSortByValue(searchSortCombo.getValue());
            
            // Convert "All Tags" to null
            if ("All Tags".equals(tag)) {
                tag = null;
            }
            
            // If all filters empty, just load all data
            if ((keyword == null || keyword.isEmpty()) &&
                (tag == null) &&
                (author == null || author.isEmpty())) {
                loadData();
                return;
            }
            
            // Perform search with correct parameter order: keyword, author, tag, sortBy
            List<PostDTO> results = ctx.postService.searchCombined(
                keyword.isEmpty() ? null : keyword,
                author.isEmpty() ? null : author,
                tag,
                sortBy,
                1,
                200
            );
            
            System.out.println("Admin search completed. Found " + results.size() + " posts.");
            
            // Update drafts list with search results
            List<PostDTO> draftResults = results.stream()
                .filter(p -> !p.published())
                .toList();
            drafts.setAll(draftResults);
            
            // Update stats
            int totalComments = results.stream()
                .mapToInt(p -> ctx.commentService.listForPost(p.id(), 0, 1000).size())
                .sum();
            statsLabel.setText("Posts: " + results.size() + "    Comments: " + totalComments);
            
        } catch (Exception ex) {
            UiExceptionHandler.showError("Search Error", ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private String getSortByValue(String displayValue) {
        if (displayValue == null) return "date_desc";
        return switch (displayValue) {
            case "Newest First" -> "date_desc";
            case "Oldest First" -> "date_asc";
            case "Title A-Z" -> "title_asc";
            case "Title Z-A" -> "title_desc";
            case "By Author" -> "author";
            default -> "date_desc";
        };
    }
    
    private void clearSearch() {
        if (searchKeywordField != null) searchKeywordField.clear();
        if (searchAuthorField != null) searchAuthorField.clear();
        if (searchTagCombo != null) searchTagCombo.getSelectionModel().selectFirst();
        if (searchSortCombo != null) searchSortCombo.getSelectionModel().selectFirst();
        loadData();
    }
    
    private void publishPost(PostDTO post) {
        try {
            var ctx = AppBootstrap.start();
            ctx.postService.publish(post.id());
            
            // Show success alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Post Published");
            alert.setContentText("The post '" + post.title() + "' has been published successfully.");
            alert.showAndWait();
            
            // Refresh data
            loadData();
        } catch (Exception ex) {
            UiExceptionHandler.showError("Publish Error", ex.getMessage());
        }
    }

    private void loadData() {
        var ctx = AppBootstrap.start();
        List<PostDTO> posts = ctx.postService.list(0, 1000);
        int totalPosts = posts.size();
        int totalComments = posts.stream().mapToInt(p -> ctx.commentService.listForPost(p.id(), 0, 1000).size()).sum();

        statsLabel.setText("Posts: " + totalPosts + "    Comments: " + totalComments);

        // Top tags
        Map<String, Integer> freq = new HashMap<>();
        for (PostDTO p : posts) {
            for (String t : p.tags()) freq.put(t, freq.getOrDefault(t, 0) + 1);
        }
        topTags.setAll(freq.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String,Integer>>comparingInt(Map.Entry::getValue).reversed())
                .limit(10)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .toList());

        // Recent drafts
        List<PostDTO> draftPosts = posts.stream()
                .filter(p -> !p.published())
                .sorted(Comparator.comparing(PostDTO::id).reversed())
                .limit(50)
                .toList();
        
        drafts.setAll(draftPosts);
        if (draftCountLabel != null) {
            draftCountLabel.setText(draftPosts.size() + " draft" + (draftPosts.size() == 1 ? "" : "s"));
        }
    }
}
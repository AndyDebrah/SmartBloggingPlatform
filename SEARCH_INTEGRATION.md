# Search Panel Integration Guide

## Step 1: Add Search Panel to AuthorDashboardView.fxml

Replace the sidebar VBox (lines 34-64) with this version that includes the search panel:

```xml
<!-- Modern Sidebar with Search Panel -->
<VBox spacing="16" prefWidth="320" style="-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);">
    
    <!-- Search & Filter Section -->
    <VBox spacing="12" style="-fx-background-color: #f8f9fc; -fx-background-radius: 8; -fx-padding: 16; -fx-border-color: #e8ecf4; -fx-border-width: 1; -fx-border-radius: 8;">
        <Label text="🔍 SEARCH & FILTER" style="-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #8b93a7;"/>
        
        <TextField fx:id="searchKeywordField" promptText="Search by keyword..." 
                   style="-fx-background-radius: 6; -fx-background-color: white;"/>
        
        <ComboBox fx:id="searchTagCombo" promptText="Filter by tag" maxWidth="Infinity" 
                  style="-fx-background-radius: 6;"/>
        
        <TextField fx:id="searchAuthorField" promptText="Search by author..." 
                   style="-fx-background-radius: 6; -fx-background-color: white;"/>
        
        <HBox spacing="4" alignment="CENTER_LEFT">
            <Label text="Sort:" style="-fx-font-size: 11px; -fx-text-fill: #8b93a7;"/>
            <ComboBox fx:id="searchSortCombo" maxWidth="Infinity" 
                      style="-fx-background-radius: 6; -fx-font-size: 11px;" HBox.hgrow="ALWAYS"/>
        </HBox>
        
        <HBox spacing="8">
            <Button fx:id="searchExecuteBtn" text="🔍 Search" 
                    style="-fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 600;" 
                    HBox.hgrow="ALWAYS" maxWidth="Infinity"/>
            <Button fx:id="searchClearBtn" text="Clear" 
                    style="-fx-background-color: white; -fx-text-fill: #2d3748; -fx-border-color: #e8ecf4; -fx-background-radius: 6; -fx-font-size: 12px;"/>
        </HBox>
    </VBox>
    
    <Label text="YOUR POSTS" style="-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #8b93a7; -fx-letter-spacing: 0.5;"/>
    <ListView fx:id="postsList" prefHeight="200" 
             style="-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;" />
    
    <!-- Quick Stats Card -->
    <VBox spacing="12" style="-fx-background-color: #f8f9fc; -fx-background-radius: 8; -fx-padding: 16; -fx-border-color: #e8ecf4; -fx-border-width: 1; -fx-border-radius: 8;">
        <Label text="QUICK STATS" style="-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #8b93a7;"/>
        <HBox spacing="8" alignment="CENTER_LEFT">
            <Label text="📝" style="-fx-font-size: 20px;"/>
            <VBox spacing="2">
                <Label fx:id="draftsCount" text="0" style="-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2d3748;"/>
                <Label text="Drafts" style="-fx-font-size: 12px; -fx-text-fill: #8b93a7;"/>
            </VBox>
        </HBox>
        <Separator style="-fx-background-color: #e8ecf4;"/>
        <HBox spacing="8" alignment="CENTER_LEFT">
            <Label text="📢" style="-fx-font-size: 20px;"/>
            <VBox spacing="2">
                <Label fx:id="publishedCount" text="0" style="-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2d3748;"/>
                <Label text="Published" style="-fx-font-size: 12px; -fx-text-fill: #8b93a7;"/>
            </VBox>
        </HBox>
        <Separator style="-fx-background-color: #e8ecf4;"/>
        <VBox spacing="8">
            <Label text="TOP TAGS" style="-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #8b93a7;"/>
            <ListView fx:id="topTagsList" prefHeight="100" 
                     style="-fx-background-color: transparent; -fx-background-insets: 0;"/>
        </VBox>
    </VBox>
</VBox>
```

## Step 2: Update AuthorDashboardController.java

Add these fields after line 40:

```java
// Search panel fields
@FXML private TextField searchKeywordField;
@FXML private ComboBox<String> searchTagCombo;
@FXML private TextField searchAuthorField;
@FXML private ComboBox<String> searchSortCombo;
@FXML private Button searchExecuteBtn;
@FXML private Button searchClearBtn;
```

Add this method call in initialize() after line 48:

```java
setupSearchControls();
```

Add these methods before loadData() method (around line 340):

```java
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
            // Use combined search
            results = ctx.postService.searchCombined(keyword, tag, author, sortBy, 1, 200);
        }
        
        data.setAll(results);
        
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
```

Add this import at the top:

```java
import com.smartblog.core.dto.TagDTO;
```

## Step 3: Compile and Test

```bash
.\mvnw.cmd compile
```

## Step 4: Same for AdminDashboardView.fxml (Optional)

Apply the same search panel integration to the Admin Dashboard following the same pattern.

## Expected Features

✅ **Keyword Search** - Search post titles and content
✅ **Tag Filtering** - Filter by specific tags
✅ **Author Search** - Find posts by author name
✅ **Sorting Options** - Sort by date, title, or author
✅ **Clear Filters** - Reset all filters
✅ **Real-time Results** - Updates main post list immediately

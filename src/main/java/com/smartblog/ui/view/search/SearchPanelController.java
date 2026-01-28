package com.smartblog.ui.view.search;

import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.dto.PostDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.function.Consumer;

public class SearchPanelController {
    @FXML private TextField keywordField;
    @FXML private ComboBox<String> tagCombo;
    @FXML private TextField authorField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button searchBtn;
    @FXML private Button clearBtn;

    private Consumer<List<PostDTO>> onSearchResultsCallback;

    @FXML
    public void initialize() {
        // Populate sort options
        sortCombo.setItems(FXCollections.observableArrayList(
                "Date (Newest First)",
                "Date (Oldest First)",
                "Title (A-Z)",
                "Title (Z-A)",
                "Author Name"
        ));
        sortCombo.setValue("Date (Newest First)");

        // Load available tags
        loadTags();

        // Wire up buttons
        searchBtn.setOnAction(e -> performSearch());
        clearBtn.setOnAction(e -> clearFilters());
    }

    private void loadTags() {
        try {
            var ctx = AppBootstrap.start();
            List<String> tags = ctx.tagService.list().stream()
                    .map(tag -> tag.name())
                    .sorted()
                    .toList();
            tagCombo.setItems(FXCollections.observableArrayList(tags));
        } catch (Exception e) {
            System.err.println("Failed to load tags: " + e.getMessage());
        }
    }

    private void performSearch() {
        String keyword = keywordField.getText();
        String tag = tagCombo.getValue();
        String author = authorField.getText();
        String sortBy = getSortValue(sortCombo.getValue());

        try {
            var ctx = AppBootstrap.start();
            List<PostDTO> results;

            // If all filters are empty, show all posts
            if ((keyword == null || keyword.isBlank()) &&
                    (tag == null || tag.isBlank()) &&
                    (author == null || author.isBlank())) {
                results = ctx.postService.list(1, 100);
            } else {
                // Use combined search - fix parameter order to match repository
                results = ctx.postService.searchCombined(keyword, tag, author, sortBy, 1, 100);
            }

            // Notify callback with results
            if (onSearchResultsCallback != null) {
                onSearchResultsCallback.accept(results);
            }
        } catch (Exception e) {
            System.err.println("Search failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getSortValue(String displayValue) {
        return switch (displayValue) {
            case "Date (Oldest First)" -> "date_asc";
            case "Title (A-Z)" -> "title_asc";
            case "Title (Z-A)" -> "title_desc";
            case "Author Name" -> "author";
            default -> "date_desc";
        };
    }

    private void clearFilters() {
        keywordField.clear();
        tagCombo.setValue(null);
        authorField.clear();
        sortCombo.setValue("Date (Newest First)");
        performSearch(); // Show all posts
    }

    public void setOnSearchResults(Consumer<List<PostDTO>> callback) {
        this.onSearchResultsCallback = callback;
    }
}

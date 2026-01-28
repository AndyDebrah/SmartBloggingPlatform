
package com.smartblog.ui.view.posts.editor;

import java.util.List;
import java.util.stream.Collectors;

import com.smartblog.application.security.SecurityContext;
import com.smartblog.application.service.PostService;
import com.smartblog.application.service.TagService;
import com.smartblog.core.dto.TagDTO;
import com.smartblog.core.exceptions.ValidationException;
import com.smartblog.core.model.Post;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.web.WebView;
import javafx.util.Duration;

public class PostEditorViewModel {

    private final PostService postService;
    private final TagService tagService;

    private Long postId = null;
    private final Long authorId;

    public final StringProperty title = new SimpleStringProperty();
    public final StringProperty htmlContent = new SimpleStringProperty();
    public final StringProperty autosaveMessage = new SimpleStringProperty("");
    public final ObservableList<String> assignedTags = FXCollections.observableArrayList();

    private final Timeline autosaveTimer;

    public PostEditorViewModel(PostService postService, TagService tagService) {
        this.postService = postService;
        this.tagService = tagService;
        this.authorId = SecurityContext.getUser().getId();

        autosaveTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> autosave()));
        autosaveTimer.setCycleCount(Timeline.INDEFINITE);
        autosaveTimer.play();
    }

    public void loadPost(Post p) {
        this.postId = p.getId();
        title.set(p.getTitle());
        htmlContent.set(p.getContent());
        // load assigned tags for this post
        try {
            assignedTags.setAll(tagService.listForPost(postId).stream().map(TagDTO::name).collect(Collectors.toList()));
        } catch (Exception ignored) {}
    }

    public void bindPreview(WebView preview) {
        htmlContent.addListener((obs, oldV, newV) ->
                preview.getEngine().loadContent(newV)
        );
    }

    public void saveDraft() {
        validate();

        if (postId == null) {
            postId = postService.createDraft(authorId, title.get(), htmlContent.get());
        } else {
            postService.update(postId, title.get(), htmlContent.get(), false);
        }

        autosaveMessage.set("Draft saved");
    }

    public void publishPost() {
        if (postId == null)
            saveDraft();

        postService.publish(postId);
        autosaveMessage.set("Post published");
    }

    private void autosave() {
        try {
            saveDraft();
            autosaveMessage.set("Autosaved at " + java.time.LocalTime.now());
        } catch (Exception ignored) {}
    }

    private void validate() {
        if (title.get().isBlank()) throw new ValidationException("Title is required");
        if (htmlContent.get().isBlank()) throw new ValidationException("Content is required");
    }

    public Long getPostId() { return postId; }

    public List<TagDTO> suggestTags(String prefix) {
        String p = prefix == null ? "" : prefix.trim().toLowerCase();
        return tagService.listAll().stream()
                .filter(t -> t.name().toLowerCase().startsWith(p))
                .collect(Collectors.toList());
    }

    public void addTagByName(String name) {
        if (name == null || name.isBlank()) return;
        if (postId == null) saveDraft();

        // find existing tag
        var tags = tagService.listAll();
        var existing = tags.stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst();
        long tagId = existing.map(TagDTO::id).orElseGet(() -> tagService.create(name));

        tagService.assignToPost(postId, tagId);
        if (!assignedTags.contains(name)) assignedTags.add(name);
        autosaveMessage.set("Tag added: " + name);
    }

    public void removeTagByName(String name) {
        if (name == null || name.isBlank() || postId == null) return;
        try {
            var tags = tagService.listAll();
            var existing = tags.stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst();
            if (existing.isPresent()) {
                tagService.removeFromPost(postId, existing.get().id());
                assignedTags.removeIf(s -> s.equalsIgnoreCase(name));
                autosaveMessage.set("Tag removed: " + name);
            }
        } catch (Exception ignored) {}
    }
}

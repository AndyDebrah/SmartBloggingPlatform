
package com.smartblog.ui.view.posts.dialog;

import com.smartblog.application.service.PostService;
import com.smartblog.application.security.SecurityContext;
import com.smartblog.core.exceptions.ValidationException;

import javafx.beans.property.*;

public class PostDialogViewModel {

    private final PostService postService;
    private Long postId = null;

    public final StringProperty title = new SimpleStringProperty();
    public final StringProperty content = new SimpleStringProperty();

    public PostDialogViewModel(PostService postService) {
        this.postService = postService;
    }

    public void load(Long id, String titleVal, String contentVal) {
        this.postId = id;
        title.set(titleVal);
        content.set(contentVal);
    }

    public Long save() {
        if (title.get().isBlank()) throw new ValidationException("Title required");
        if (content.get().isBlank()) throw new ValidationException("Content required");

        Long authorId = SecurityContext.getUser().getId();

        if (postId == null) {
            return postService.createDraft(authorId, title.get(), content.get());
        } else {
            postService.update(postId, title.get(), content.get(), false);
            return postId;
        }
    }
}

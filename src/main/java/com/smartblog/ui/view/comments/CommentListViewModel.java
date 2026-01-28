
package com.smartblog.ui.view.comments;

import com.smartblog.application.service.CommentService;
import com.smartblog.core.dto.CommentDTO;
import javafx.beans.property.*;
import javafx.collections.*;

public class CommentListViewModel {
    private final CommentService service;
    public final LongProperty postId = new SimpleLongProperty(0);
    public final ObservableList<CommentDTO> data = FXCollections.observableArrayList();

    public CommentListViewModel(CommentService service) { this.service = service; }

    public void load() {
        if (postId.get() <= 0) { data.clear(); return; }
        data.setAll(service.listForPost(postId.get(), 1, 200));
    }
}

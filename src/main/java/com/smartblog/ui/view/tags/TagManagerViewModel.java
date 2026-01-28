
package com.smartblog.ui.view.tags;

import com.smartblog.application.service.TagService;
import com.smartblog.core.dto.TagDTO;
import javafx.beans.property.*;
import javafx.collections.*;

public class TagManagerViewModel {
    private final TagService tagService;

    public final ObservableList<TagDTO> all = FXCollections.observableArrayList();
    public final ObservableList<TagDTO> ofPost = FXCollections.observableArrayList();
    public final LongProperty postId = new SimpleLongProperty(0);

    public TagManagerViewModel(TagService service) { this.tagService = service; }

    public void loadAll() { all.setAll(tagService.listAll()); }
    public void loadForPost() {
        if (postId.get() > 0) ofPost.setAll(tagService.listForPost(postId.get()));
        else ofPost.clear();
    }
    public long create(String name) { return tagService.create(name); }
    public boolean assign(long tagId) { return tagService.assignToPost(postId.get(), tagId); }
    public boolean remove(long tagId) { return tagService.removeFromPost(postId.get(), tagId); }
}

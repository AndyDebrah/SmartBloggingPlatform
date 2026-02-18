package com.smartblog.bootstrap;

import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.model.Tag;

import java.util.Collections;
import java.util.List;

/**
 * Lightweight bootstrap stub used to satisfy UI compile-time references in the server build.
 * Provides minimal, no-op implementations for services accessed by JavaFX controllers.
 */
public final class AppBootstrap {

    private static final Context CTX = new Context();

    private AppBootstrap() {}

    public static Context start() {
        return CTX;
    }

    public static class Context {
        public final PostServiceBridge postService = new PostServiceBridge() {
            @Override
            public List<PostDTO> list(int page, int size) { return Collections.emptyList(); }

            @Override
            public List<PostDTO> searchCombined(String keyword, String author, String tag, String sortBy, int page, int size) { return Collections.emptyList(); }

            @Override
            public void publish(long postId) { /* no-op */ }
        };

        public final CommentServiceBridge commentService = new CommentServiceBridge() {
            @Override
            public List<?> listForPost(long postId, int page, int size) { return Collections.emptyList(); }
        };

        public final TagServiceBridge tagService = new TagServiceBridge() {
            @Override
            public List<Tag> list() { return Collections.emptyList(); }
        };
    }

    public interface PostServiceBridge {
        List<PostDTO> list(int page, int size);
        List<PostDTO> searchCombined(String keyword, String author, String tag, String sortBy, int page, int size);
        void publish(long postId);
    }

    public interface CommentServiceBridge {
        List<?> listForPost(long postId, int page, int size);
    }

    public interface TagServiceBridge {
        List<Tag> list();
    }
}


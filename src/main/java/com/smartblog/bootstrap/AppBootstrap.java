package com.smartblog.bootstrap;

import com.smartblog.application.service.CommentService;
import com.smartblog.application.service.PostService;
import com.smartblog.application.service.TagService;
import com.smartblog.application.service.UserService;

/**
 * Application bootstrap utility for UI initialization.
 * This is a placeholder to satisfy compilation dependencies for UI components.
 */
public class AppBootstrap {

    /**
     * Context holder for application services.
     * This is a stub implementation to satisfy UI compilation dependencies.
     */
    public static class AppContext {
        public final PostService postService;
        public final CommentService commentService;
        public final TagService tagService;
        public final UserService userService;

        public AppContext(PostService postService, CommentService commentService, TagService tagService, UserService userService) {
            this.postService = postService;
            this.commentService = commentService;
            this.tagService = tagService;
            this.userService = userService;
        }
    }

    /**
     * Starts the application bootstrap process.
     * Note: This is a stub implementation as UI components are excluded from compilation.
     * @return AppContext with null services (UI not functional in this build)
     */
    public static AppContext start() {
        // Stub implementation: returns context with null services
        // UI functionality is not supported in API-only builds
        return new AppContext(null, null, null, null);
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private AppBootstrap() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}


package com.smartblog.bootstrap;

import java.io.InputStream;
import java.util.Properties;

import javax.sql.DataSource;

import com.smartblog.application.service.CommentService;
import com.smartblog.application.service.CommentServiceImpl;
import com.smartblog.application.service.PostService;
import com.smartblog.application.service.PostServiceImpl;
import com.smartblog.application.service.TagService;
import com.smartblog.application.service.TagServiceImpl;
import com.smartblog.application.service.UserService;
import com.smartblog.application.service.UserServiceImpl;
import com.smartblog.infrastructure.datasource.DataSourceFactory;
import com.smartblog.infrastructure.migration.MigrationRunner;
import com.smartblog.infrastructure.repository.api.CommentRepository;
import com.smartblog.infrastructure.repository.api.PostRepository;
import com.smartblog.infrastructure.repository.api.TagRepository;
import com.smartblog.infrastructure.repository.api.UserRepository;
import com.smartblog.infrastructure.repository.jdbc.CommentRepositoryJdbc;
import com.smartblog.infrastructure.repository.jdbc.PostRepositoryJdbc;
import com.smartblog.infrastructure.repository.jdbc.TagRepositoryJdbc;
import com.smartblog.infrastructure.repository.jdbc.UserRepositoryJdbc;

/**
 * Starts the infrastructure:
 * - Loads properties
 * - Builds Hikari DataSource
 * - Runs Flyway migrations
 * - Wires repositories and services
 *
 * Use from your UI main() or tests to get service instances.
 */
public class AppBootstrap {

    public static final class Context {
        public final DataSource ds;

        public final UserRepository userRepo;
        public final PostRepository postRepo;
        public final CommentRepository commentRepo;
        public final TagRepository tagRepo;

        public final UserService userService;
        public final PostService postService;
        public final CommentService commentService;
        public final TagService tagService;

        private Context(DataSource ds,
                        UserRepository userRepo, PostRepository postRepo,
                        CommentRepository commentRepo, TagRepository tagRepo,
                        UserService userService, PostService postService,
                        CommentService commentService, TagService tagService) {
            this.ds = ds;
            this.userRepo = userRepo; this.postRepo = postRepo;
            this.commentRepo = commentRepo; this.tagRepo = tagRepo;
            this.userService = userService; this.postService = postService;
            this.commentService = commentService; this.tagService = tagService;
        }
    }

    public static Context start() {
        try {
            Properties props = new Properties();
            try (InputStream in = AppBootstrap.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (in == null) throw new IllegalStateException("application.properties not found");
                props.load(in);
            }
            DataSource ds = DataSourceFactory.get(props);
            MigrationRunner.migrate(ds, props);

            // repositories
            var userRepo = new UserRepositoryJdbc(ds);
            var postRepo = new PostRepositoryJdbc(ds);
            var commentRepo = new CommentRepositoryJdbc(ds);
            var tagRepo = new TagRepositoryJdbc(ds);

            // services
            var userService = new UserServiceImpl(userRepo);
            var postService = new PostServiceImpl(postRepo, userRepo, tagRepo);
            var commentService = new CommentServiceImpl(commentRepo, postRepo, userRepo);
            var tagService = new TagServiceImpl(tagRepo);

            return new Context(ds, userRepo, postRepo, commentRepo, tagRepo, userService, postService, commentService, tagService);
        } catch (Exception e) {
            throw new RuntimeException("Bootstrap failed", e);
        }
    }
}

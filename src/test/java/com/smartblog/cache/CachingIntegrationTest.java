package com.smartblog.cache;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ActiveProfiles;

import com.smartblog.application.service.PostService;
import com.smartblog.application.service.UserService;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

@SpringBootTest(classes = com.smartblog.SmartBlogApplication.class)
@ActiveProfiles("test")
@EnableCaching
@TestInstance(Lifecycle.PER_CLASS)
public class CachingIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CacheManager cacheManager;

    // No Mockito spies or mocks — use real JPA repositories against H2 for realistic caching behavior

    private User author;
    private Post post;

    private static final java.text.DecimalFormat DF = new java.text.DecimalFormat("0.0");

    @BeforeAll
    void initData() {
        // create unique test data once for all tests
        long suffix = System.currentTimeMillis();
        author = User.builder()
                .username("cacheuser" + suffix)
                .email("cache+" + suffix + "@example.com")
                .passwordHash("p")
                .build();
        userJpaRepository.save(author);

        post = Post.builder().author(author).title("cache-test").content("cache content").published(true).build();
        postJpaRepository.save(post);
    }

    @BeforeEach
    void clearCachesAndSpies() {
        // clear caches before each test so first call is a cold read
        if (cacheManager.getCache("postView") != null) cacheManager.getCache("postView").clear();
        if (cacheManager.getCache("postsByAuthor") != null) cacheManager.getCache("postsByAuthor").clear();
        if (cacheManager.getCache("userById") != null) cacheManager.getCache("userById").clear();
        if (cacheManager.getCache("userByUsername") != null) cacheManager.getCache("userByUsername").clear();

        // No need to reset spy invocation counts as we are using real repositories
    }

    @Test
    void getView_isCached() {
        // Cold call
        long s1 = System.nanoTime();
        postService.getView(post.getId());
        long coldNs = System.nanoTime() - s1;

        // Warm call (immediately after, identical args)
        long s2 = System.nanoTime();
        postService.getView(post.getId());
        long warmNs = System.nanoTime() - s2;

        double coldMs = coldNs / 1_000_000.0;
        double warmMs = warmNs / 1_000_000.0;
        double improvement = (1.0 - (warmMs / coldMs)) * 100.0;

        System.out.println();
        System.out.println("COLD call took: " + Math.round(coldMs) + " ms");
        System.out.println("WARM call took: " + DF.format(warmMs) + " ms");
        System.out.println("Cache improvement: " + DF.format(improvement) + "%");
        System.out.println();

        // warm should be significantly faster
        assertThat(warmMs).isLessThan(coldMs * 0.6);

        // verify cache entry exists for the post view
        assertThat(cacheManager.getCache("postView").get(post.getId())).isNotNull();
    }

    @Test
    void listByAuthor_isCached() {
        // Cold call
        long s1 = System.nanoTime();
        postService.listByAuthor(author.getId(), 0, 10);
        long coldNs = System.nanoTime() - s1;

        // Warm call
        long s2 = System.nanoTime();
        postService.listByAuthor(author.getId(), 0, 10);
        long warmNs = System.nanoTime() - s2;

        double coldMs = coldNs / 1_000_000.0;
        double warmMs = warmNs / 1_000_000.0;
        double improvement = (1.0 - (warmMs / coldMs)) * 100.0;

        System.out.println();
        System.out.println("COLD listByAuthor took: " + Math.round(coldMs) + " ms");
        System.out.println("WARM listByAuthor took: " + DF.format(warmMs) + " ms");
        System.out.println("Cache improvement: " + DF.format(improvement) + "%");
        System.out.println();

        assertThat(warmMs).isLessThan(coldMs * 0.6);

        // verify cache entry exists for postsByAuthor with the service key format
        String postsByAuthorKey = author.getId() + "-" + 0 + "-" + 10;
        assertThat(cacheManager.getCache("postsByAuthor").get(postsByAuthorKey)).isNotNull();
    }

    @Test
    void findByUsername_isCached() {
        // Cold call
        long s1 = System.nanoTime();
        userService.findByUsername(author.getUsername());
        long coldNs = System.nanoTime() - s1;

        // Warm call
        long s2 = System.nanoTime();
        userService.findByUsername(author.getUsername());
        long warmNs = System.nanoTime() - s2;

        double coldMs = coldNs / 1_000_000.0;
        double warmMs = warmNs / 1_000_000.0;
        double improvement = (1.0 - (warmMs / coldMs)) * 100.0;

        System.out.println();
        System.out.println("COLD findByUsername took: " + Math.round(coldMs) + " ms");
        System.out.println("WARM findByUsername took: " + DF.format(warmMs) + " ms");
        System.out.println("Cache improvement: " + DF.format(improvement) + "%");
        System.out.println();

        assertThat(warmMs).isLessThan(coldMs * 0.6);

        // verify cache entry exists for userByUsername
        assertThat(cacheManager.getCache("userByUsername").get(author.getUsername())).isNotNull();
    }
}

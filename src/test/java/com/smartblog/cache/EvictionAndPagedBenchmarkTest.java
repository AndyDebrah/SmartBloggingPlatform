package com.smartblog.cache;

import java.text.DecimalFormat;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.smartblog.application.service.PostService;
import com.smartblog.application.service.UserService;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

/**
 * Benchmarks paged `listByAuthor` and verifies cache eviction after publish/update.
 */
@SpringBootTest(classes = com.smartblog.SmartBlogApplication.class)
@ActiveProfiles("test")
@Transactional
@Rollback
public class EvictionAndPagedBenchmarkTest {

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

    private User author;
    private static final DecimalFormat DF = new DecimalFormat("0.0");

    @BeforeEach
    void setUp() {
        long suffix = System.currentTimeMillis();
        author = User.builder().username("evictuser" + suffix).email("evict+" + suffix + "@example.com").passwordHash("x").build();
        userJpaRepository.save(author);

        // create multiple posts for pagination
        IntStream.rangeClosed(1, 30).forEach(i -> {
            Post p = Post.builder().author(author).title("p" + i).content("content" + i).published(true).build();
            postJpaRepository.save(p);
        });

        if (cacheManager.getCache("postsByAuthor") != null) cacheManager.getCache("postsByAuthor").clear();
        if (cacheManager.getCache("postView") != null) cacheManager.getCache("postView").clear();
    }

    @Test
    void pagedList_and_evict_onPublish() {
        final int page = 0, size = 10;

        // Cold paged list
        long sCold = System.nanoTime();
        var page1 = postService.listByAuthor(author.getId(), page, size);
        long coldNs = System.nanoTime() - sCold;

        // Warm paged list
        long sWarm = System.nanoTime();
        var pageWarm = postService.listByAuthor(author.getId(), page, size);
        long warmNs = System.nanoTime() - sWarm;

        double coldMs = coldNs / 1_000_000.0;
        double warmMs = warmNs / 1_000_000.0;

        System.out.println();
        System.out.println("Paged listByAuthor - cold: " + Math.round(coldMs) + " ms, warm: " + DF.format(warmMs) + " ms");

        assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(30);
        assertThat(warmMs).isLessThan(coldMs * 0.6);

        // pick a post from page1 and publish it to trigger eviction
        PostDTO first = page1.getContent().get(0);
        long postId = first.id();

        // publish (this should evict the relevant caches per PostServiceImpl)
        boolean published = postService.publish(postId);
        assertThat(published).isTrue();

        // Immediately read paged list again - should be a cold read after eviction
        long sAfterEvict = System.nanoTime();
        var pageAfter = postService.listByAuthor(author.getId(), page, size);
        long afterNs = System.nanoTime() - sAfterEvict;
        double afterMs = afterNs / 1_000_000.0;

        System.out.println("After eviction paged list took: " + DF.format(afterMs) + " ms");

        // after eviction should be similar to original cold (not the warm time)
        assertThat(afterMs).isGreaterThan(warmMs * 2);

        // verify cache entry present for postsByAuthor key (service key format)
        String key = author.getId() + "-" + page + "-" + size;
        assertThat(cacheManager.getCache("postsByAuthor").get(key)).isNotNull();
    }
}

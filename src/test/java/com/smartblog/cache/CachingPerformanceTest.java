package com.smartblog.cache;

import java.text.DecimalFormat;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.smartblog.application.service.PostService;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

/**
 * Simple performance harness to observe caching impact.
 * - populates N posts
 * - measures cold read (first access)
 * - measures hot read (repeat accesses)
 * - prints basic timings and asserts cached repository call counts
 */
@SpringBootTest(classes = com.smartblog.SmartBlogApplication.class)
@ActiveProfiles("test")
@Transactional
@Rollback
public class CachingPerformanceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CacheManager cacheManager;

    @SpyBean
    private PostJpaRepository spyPostJpaRepository;

    private User author;
    private Post post;

    private static final DecimalFormat DF = new DecimalFormat("0.0");

    @BeforeEach
    void setUp() {
        // clear caches and DB
        if (cacheManager.getCache("postView") != null) cacheManager.getCache("postView").clear();
        // create unique user/post for this test (transactional + rollback will avoid persisting)
        long suffix = System.currentTimeMillis();
        author = User.builder()
                .username("perfuser" + suffix)
                .email("perf+" + suffix + "@example.com")
                .passwordHash("x")
                .build();
        userJpaRepository.save(author);

        post = Post.builder().author(author).title("perf").content("perf").published(true).build();
        postJpaRepository.save(post);
    }

    @Test
    void measureColdAndHotReads() {
        // Cold read: should hit repository
        long t0 = System.nanoTime();
        postService.getView(post.getId());
        long coldNs = System.nanoTime() - t0;

        // Hot reads: repeated accesses should be served from cache
        int runs = 200;
        long t1 = System.nanoTime();
        IntStream.range(0, runs).forEach(i -> postService.getView(post.getId()));
        long hotNs = System.nanoTime() - t1;

        double coldMs = coldNs / 1_000_000.0;
        double avgHotMs = (hotNs / (double) runs) / 1_000_000.0;
        double improvement = (1.0 - (avgHotMs / coldMs)) * 100.0;

        System.out.println();
        System.out.println("Cache performance results for getView(postId=" + post.getId() + ")");
        System.out.println("-----------------------------------------------");
        System.out.println("First call (no cache): " + Math.round(coldMs) + "ms");
        System.out.println("Warm call avg (" + runs + " runs): " + DF.format(avgHotMs) + "ms");
        System.out.println("Improvement: " + DF.format(improvement) + "%");
        System.out.println();

        // Sanity assertions: hot average must be significantly less than cold (heuristic)
        assertThat(avgHotMs).isLessThan(coldMs * 0.6);

        // Verify repository was called at least once for cold read
        org.mockito.Mockito.verify(spyPostJpaRepository, org.mockito.Mockito.atLeastOnce()).findById(post.getId());
    }
}

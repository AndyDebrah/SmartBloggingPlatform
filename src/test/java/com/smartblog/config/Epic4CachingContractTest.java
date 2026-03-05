package com.smartblog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.smartblog.application.service.impl.CommentServiceImpl;
import com.smartblog.application.service.impl.ReviewServiceImpl;

class Epic4CachingContractTest {

    @Test
    void cacheConfig_shouldRegisterEpic4Caches() {
        CacheConfig cacheConfig = new CacheConfig();
        Caffeine<Object, Object> caffeine = cacheConfig.caffeineConfig();
        CacheManager cacheManager = cacheConfig.cacheManager(caffeine);

        assertThat(cacheManager.getCache("commentsByPost")).isNotNull();
        assertThat(cacheManager.getCache("reviewsByPost")).isNotNull();
        assertThat(cacheManager.getCache("reviewsByUser")).isNotNull();
        assertThat(cacheManager.getCache("reviewStatsByPost")).isNotNull();
    }

    @Test
    void commentAndReviewMethods_shouldHaveExpectedCacheAnnotations() throws Exception {
        Method listForPost = CommentServiceImpl.class.getMethod("listForPost", long.class, int.class, int.class);
        Cacheable commentListCacheable = listForPost.getAnnotation(Cacheable.class);
        assertThat(commentListCacheable).isNotNull();
        assertThat(commentListCacheable.value()).containsExactly("commentsByPost");
        assertThat(commentListCacheable.key()).isEqualTo("#postId + '-' + #page + '-' + #size");

        Method reviewByPost = ReviewServiceImpl.class.getMethod("getReviewsByPost", Long.class, int.class, int.class);
        Cacheable reviewByPostCacheable = reviewByPost.getAnnotation(Cacheable.class);
        assertThat(reviewByPostCacheable).isNotNull();
        assertThat(reviewByPostCacheable.value()).containsExactly("reviewsByPost");

        Method reviewByUser = ReviewServiceImpl.class.getMethod("getReviewsByUser", Long.class, int.class, int.class);
        Cacheable reviewByUserCacheable = reviewByUser.getAnnotation(Cacheable.class);
        assertThat(reviewByUserCacheable).isNotNull();
        assertThat(reviewByUserCacheable.value()).containsExactly("reviewsByUser");

        Method reviewStats = ReviewServiceImpl.class.getMethod("getPostRatingStats", Long.class);
        Cacheable reviewStatsCacheable = reviewStats.getAnnotation(Cacheable.class);
        assertThat(reviewStatsCacheable).isNotNull();
        assertThat(reviewStatsCacheable.value()).containsExactly("reviewStatsByPost");
        assertThat(reviewStatsCacheable.key()).isEqualTo("#postId");
    }

    @Test
    void reviewMutations_shouldEvictAllRelatedCaches() throws Exception {
        Method createReview = ReviewServiceImpl.class.getMethod("createReview", Long.class, Long.class, Integer.class, String.class);
        Method updateReview = ReviewServiceImpl.class.getMethod("updateReview", Long.class, Integer.class, String.class);
        Method deleteReview = ReviewServiceImpl.class.getMethod("deleteReview", Long.class);

        assertThat(extractEvictedCaches(createReview))
                .containsExactlyInAnyOrder("reviewStatsByPost", "reviewsByPost", "reviewsByUser");
        assertThat(extractEvictedCaches(updateReview))
                .containsExactlyInAnyOrder("reviewStatsByPost", "reviewsByPost", "reviewsByUser");
        assertThat(extractEvictedCaches(deleteReview))
                .containsExactlyInAnyOrder("reviewStatsByPost", "reviewsByPost", "reviewsByUser");
    }

    private Set<String> extractEvictedCaches(Method method) {
        Caching caching = method.getAnnotation(Caching.class);
        assertThat(caching).isNotNull();
        return Arrays.stream(caching.evict())
                .map(CacheEvict::value)
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());
    }
}


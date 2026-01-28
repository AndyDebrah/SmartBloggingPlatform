
package com.smartblog.infrastructure.caching;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public final class CacheManager {

    public static final Cache<Long, Object> postCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(500)
                    .build();

    public static final Cache<Long, Object> userCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .maximumSize(1000)
                    .build();

        /**
         * Clear all application caches managed here.
         * Can be used by diagnostics or benchmarks to force cold runs.
         */
        public static void clearAll() {
                postCache.invalidateAll();
                userCache.invalidateAll();
        }
}

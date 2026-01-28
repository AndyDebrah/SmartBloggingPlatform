
package com.smartblog.infrastructure.caching;

import com.github.benmanes.caffeine.cache.*;

import java.util.concurrent.TimeUnit;

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
}

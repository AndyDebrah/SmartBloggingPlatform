package com.smartblog.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "security.revocation.store", havingValue = "redis")
public class RedisRevocationService implements RevocationService {

    private final StringRedisTemplate redis;

    @Autowired
    public RedisRevocationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void revoke(String token, Instant expiry) {
        long seconds = Math.max(1, expiry.getEpochSecond() - Instant.now().getEpochSecond());
        String key = "revoked:" + token;
        redis.opsForValue().set(key, Long.toString(expiry.getEpochSecond()), Duration.ofSeconds(seconds));
    }

    @Override
    public boolean isRevoked(String token) {
        String key = "revoked:" + token;
        return Boolean.TRUE.equals(redis.hasKey(key));
    }
}

package com.smartblog.auth;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "security.refresh.store", havingValue = "redis")
public class RedisRefreshTokenService implements RefreshTokenService {

    private final StringRedisTemplate redis;
    private final Duration ttl = Duration.ofDays(7);

    @Autowired
    public RedisRefreshTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String createToken(String username) {
        String token = UUID.randomUUID().toString();
        String key = "refresh:" + token;
        redis.opsForValue().set(key, username, ttl);
        return token;
    }

    @Override
    public String validateAndConsume(String token) {
        String key = "refresh:" + token;
        String user = redis.opsForValue().get(key);
        if (user == null) return null;
        // consume
        redis.delete(key);
        return user;
    }

    @Override
    public void revoke(String token) {
        redis.delete("refresh:" + token);
    }
}

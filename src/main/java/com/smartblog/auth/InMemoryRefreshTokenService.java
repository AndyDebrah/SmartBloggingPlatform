package com.smartblog.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class InMemoryRefreshTokenService implements RefreshTokenService {

    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();
    private final Map<String, String> tokenToUser = new ConcurrentHashMap<>();

    // Default 7 days
    private final Duration ttl = Duration.ofDays(7);

    @Override
    public String createToken(String username) {
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(ttl);
        tokens.put(token, expiry);
        tokenToUser.put(token, username);
        return token;
    }

    @Override
    public String validateAndConsume(String token) {
        Instant exp = tokens.get(token);
        if (exp == null) return null;
        if (Instant.now().isAfter(exp)) {
            tokens.remove(token);
            tokenToUser.remove(token);
            return null;
        }
        // consume the refresh token (rotate)
        String user = tokenToUser.remove(token);
        tokens.remove(token);
        return user;
    }

    @Override
    public void revoke(String token) {
        tokens.remove(token);
        tokenToUser.remove(token);
    }

    // Cleanup expired tokens every hour
    @Scheduled(fixedDelayString = "PT1H")
    public void cleanup() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}

package com.smartblog.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class InMemoryRefreshTokenService implements RefreshTokenService {

    private final ConcurrentHashMap<String, RefreshTokenEntry> tokens = new ConcurrentHashMap<>();

    // Default 7 days
    private final Duration ttl = Duration.ofDays(7);

    @Override
    public String createToken(String username) {
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(ttl);
        tokens.put(token, new RefreshTokenEntry(username, expiry));
        return token;
    }

    @Override
    public String validateAndConsume(String token) {
        Instant now = Instant.now();
        AtomicReference<String> userRef = new AtomicReference<>(null);
        tokens.compute(token, (k, entry) -> {
            if (entry == null) {
                return null;
            }
            if (now.isAfter(entry.expiry())) {
                return null;
            }
            // One-shot consume: return null to remove entry atomically.
            userRef.set(entry.username());
            return null;
        });
        return userRef.get();
    }

    @Override
    public void revoke(String token) {
        tokens.remove(token);
    }

    // Cleanup expired tokens every hour
    @Scheduled(fixedDelayString = "PT1H")
    public void cleanup() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(e -> e.getValue().expiry().isBefore(now));
    }

    private record RefreshTokenEntry(String username, Instant expiry) {
    }
}

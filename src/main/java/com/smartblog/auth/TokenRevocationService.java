package com.smartblog.auth;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TokenRevocationService implements RevocationService {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String token, Instant expiry) {
        revoked.put(token, expiry);
    }

    public boolean isRevoked(String token) {
        Instant exp = revoked.get(token);
        if (exp == null) return false;
        if (Instant.now().isAfter(exp)) {
            revoked.remove(token);
            return false;
        }
        return true;
    }

    // Cleanup every hour
    @Scheduled(fixedDelayString = "PT1H")
    public void cleanup() {
        Instant now = Instant.now();
        revoked.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}

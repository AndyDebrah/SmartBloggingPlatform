package com.smartblog.auth;

import java.time.Instant;

public interface RevocationService {
    void revoke(String token, Instant expiry);
    boolean isRevoked(String token);
}

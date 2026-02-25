package com.smartblog.auth;

public interface RefreshTokenService {
    String createToken(String username);
    /**
     * Validate and consume the refresh token. Returns the associated username or null if invalid.
     */
    String validateAndConsume(String token);
    void revoke(String token);
}

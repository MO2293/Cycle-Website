package com.cyclehaven.dto.auth;

import com.cyclehaven.dto.user.UserResponse;

/**
 * Returned by register and login. The client stores the token and sends it back
 * as {@code Authorization: Bearer <token>} on subsequent requests.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {

    public static AuthResponse of(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}

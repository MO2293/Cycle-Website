package com.cyclehaven.security;

import com.cyclehaven.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the signed tokens that authenticate API calls.
 *
 * <p>A token is a signed statement of "who you are" that the server can verify
 * without a lookup, which is what lets the API stay stateless — there is no
 * server-side session to keep, so any instance can serve any request.
 *
 * <p>The signature is what makes this safe: the payload is only base64-encoded
 * (readable by anyone), but altering it invalidates the HMAC, so a client cannot
 * promote itself to ADMIN by editing its own token.
 */
@Service
public class JwtService {

    private final String configuredSecret;
    private final long expirationMinutes;
    private SecretKey key;

    public JwtService(
            @Value("${cyclehaven.jwt.secret}") String configuredSecret,
            @Value("${cyclehaven.jwt.expiration-minutes}") long expirationMinutes) {
        this.configuredSecret = configuredSecret;
        this.expirationMinutes = expirationMinutes;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes;
        try {
            // Secrets are normally supplied base64-encoded (openssl rand -base64 48).
            keyBytes = Decoders.BASE64.decode(configuredSecret);
        } catch (IllegalArgumentException ex) {
            // ...but accept a plain string too, so a hand-typed secret still works.
            keyBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "cyclehaven.jwt.secret must decode to at least 32 bytes for HS256. "
                            + "Generate one with: openssl rand -base64 48");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                // The subject is the user id, not the email: emails can change,
                // ids cannot.
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiry, returning the parsed claims.
     *
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or
     *                                      its signature does not match.
     */
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getPayload().getSubject());
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}

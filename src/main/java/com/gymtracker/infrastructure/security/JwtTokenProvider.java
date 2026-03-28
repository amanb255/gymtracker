package com.gymtracker.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration = 15 * 60; // 15 minutes in seconds
    private final long refreshTokenExpiration = 30 * 24 * 60 * 60; // 30 days in seconds

    public JwtTokenProvider(@Value("${jwt.secret:}") String jwtSecret) {
        // Use environment variable or application property for secret
        // For development, fallback to a default (should use environment variable in
        // production)
        String secret = jwtSecret != null && !jwtSecret.isBlank() ? jwtSecret : System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            // Fallback for development only - NEVER use in production
            secret = "your-secret-key-min-32-chars-for-hs512-algorithm-production-use-env";
        }

        // Ensure minimum 32 bytes for HS512
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters for HS512");
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate an access token (short-lived, 15 minutes)
     */
    public String generateAccessToken(UUID userId, String email) {
        return generateToken(userId, email, accessTokenExpiration);
    }

    /**
     * Generate a refresh token (long-lived, 30 days)
     */
    public String generateRefreshToken(UUID userId, String email) {
        return generateToken(userId, email, refreshTokenExpiration);
    }

    /**
     * Generate a JWT token with specified expiration
     */
    private String generateToken(UUID userId, String email, long expirationSeconds) {
        Instant now = Instant.now();
        Instant expiryTime = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryTime))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract userId from token
     */
    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Validate token signature and expiration
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false; // Token expired
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            return false; // Invalid token
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            extractAllClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get access token expiration time in seconds
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration;
    }

    /**
     * Get refresh token expiration time in seconds
     */
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration;
    }
}

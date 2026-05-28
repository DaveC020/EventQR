package com.thedavelopers.eventqr.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.thedavelopers.eventqr.shared.constants.AccountRole;
import com.thedavelopers.eventqr.shared.exceptions.UnauthorizedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final Duration expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMillis(expirationMs);
    }

    public String createToken(UUID userId, String email, AccountRole role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public UUID extractUserIdFromBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing session token");
        }
        Claims claims = extractClaims(authorizationHeader);
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.isBlank()) {
            userId = claims.getSubject();
        }
        return UUID.fromString(userId);
    }

    public AccountRole extractRoleFromBearer(String authorizationHeader) {
        Claims claims = extractClaims(authorizationHeader);
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new UnauthorizedException("Invalid or expired session");
        }
        try {
            return AccountRole.valueOf(role);
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException("Invalid or expired session");
        }
    }

    public String extractEmailFromBearer(String authorizationHeader) {
        Claims claims = extractClaims(authorizationHeader);
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Invalid or expired session");
        }
        return email;
    }

    private Claims extractClaims(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing session token");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new UnauthorizedException("Missing session token");
        }
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new UnauthorizedException("Invalid or expired session");
        }
    }
}


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
}
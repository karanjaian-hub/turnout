package com.turnout.common.security;

import com.turnout.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

// Centralised JWT operations shared by authservice (issues tokens)
// and api-gateway (validates tokens on every request)
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_ROLE = "role";
// jti = JWT ID — a unique ID per token used for blacklisting on logout
    private static final String CLAIM_JTI  = "jti";

    private final JwtProperties jwtProperties;

// Token generation
    public String generateAccessToken(UUID userId, UserRole role, long expiryMs) {
        return buildToken(userId, role, expiryMs);
    }

    public String generateRefreshToken(UUID userId, long expiryMs) {
// Refresh tokens carry no role — they only identify the user
        return buildToken(userId, null, expiryMs);
    }

    private String buildToken(UUID userId, UserRole role, long expiryMs) {
        var now = new Date();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_JTI, UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(getSigningKey());

        if (role != null) {
            builder.claim(CLAIM_ROLE, role.name());
        }

        return builder.compact();
    }

// Token validation
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
// Any parse or signature exception means token is invalid
            return false;
        }
    }

// Claim extraction
    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    public UserRole extractRole(String token) {
        String role = extractAllClaims(token).get(CLAIM_ROLE, String.class);
        return role != null ? UserRole.valueOf(role) : null;
    }

    public java.util.Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public String extractJti(String token) {
        return extractAllClaims(token).get(CLAIM_JTI, String.class);
    }

// Private helpers
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }
}

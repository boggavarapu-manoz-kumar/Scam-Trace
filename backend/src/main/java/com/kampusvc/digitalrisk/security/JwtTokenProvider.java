package com.kampusvc.digitalrisk.security;

import io.jsonwebtoken.*;
import com.kampusvc.digitalrisk.identity.model.OrganizationRole;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secretHex,
            @Value("${app.jwt.expiration-ms:3600000}") String expirationStr) {
        String cleanSecret = (secretHex == null || secretHex.isBlank())
                ? "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
                : secretHex.trim();
        byte[] keyBytes = Decoders.BASE64.decode(
                cleanSecret.length() % 4 == 0 ? cleanSecret : java.util.Base64.getEncoder().encodeToString(cleanSecret.getBytes())
        );
        this.key = Keys.hmacShaKeyFor(keyBytes);
        long parsedExp = 3600000L;
        if (expirationStr != null && !expirationStr.isBlank()) {
            try {
                parsedExp = Long.parseLong(expirationStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        this.expirationMs = parsedExp;
    }

    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(principal.getId().toString())
                .claim("email", principal.getUsername())
                .claim("fullName", principal.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key);

        if (principal.getActiveOrganizationId() != null) {
            builder.claim("orgId", principal.getActiveOrganizationId().toString());
        }
        if (principal.getActiveRole() != null) {
            builder.claim("role", principal.getActiveRole().name());
        }

        return builder.compact();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public OrganizationRole getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        String roleStr = claims.get("role", String.class);
        return roleStr != null ? OrganizationRole.valueOf(roleStr) : null;
    }

    public UUID getOrganizationIdFromToken(String token) {
        Claims claims = parseClaims(token);
        String orgIdStr = claims.get("orgId", String.class);
        return orgIdStr != null ? UUID.fromString(orgIdStr) : null;
    }

    public Instant getExpirationFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().toInstant();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

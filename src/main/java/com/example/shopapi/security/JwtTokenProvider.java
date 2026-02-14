package com.example.shopapi.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        // Try to decode as Base64, if it fails, encode the plain string as Base64 first
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            // If not valid Base64, use the raw bytes of the string
            log.warn("JWT secret is not Base64 encoded, using raw bytes");
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }

        // Ensure the key is at least 256 bits (32 bytes) for HS256
        if (keyBytes.length < 32) {
            log.warn("JWT secret is too short, padding to 256 bits");
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        log.info("JWT Token Provider initialized with access token expiration: {}ms, refresh token expiration: {}ms",
                accessTokenExpiration, refreshTokenExpiration);
    }

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal.getId(), userPrincipal.getUsername(), userPrincipal.getRole());
    }

    public String generateAccessToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("username", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
    }

    public String getTokenType(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("type", String.class);
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                log.error("JWT token is null or empty");
                return false;
            }

            // Check for common issues
            if (token.startsWith("\"") || token.endsWith("\"")) {
                log.error("JWT token contains quotes - frontend may be sending it incorrectly");
                return false;
            }

            if (token.equals("undefined") || token.equals("null")) {
                log.error("JWT token is literal 'undefined' or 'null' string");
                return false;
            }

            // Log token prefix for debugging (first 20 chars only for security)
            log.debug("Validating JWT token starting with: {}...",
                    token.length() > 20 ? token.substring(0, 20) : token);

            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token (malformed): {}", ex.getMessage());
            // Log the token structure for debugging
            if (token != null) {
                String[] parts = token.split("\\.");
                log.debug("Token has {} parts (should be 3)", parts.length);
            }
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: expired at {}", ex.getClaims().getExpiration());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration();
    }
}


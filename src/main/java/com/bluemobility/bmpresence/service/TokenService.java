package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.config.JwtConfigProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private final JwtConfigProperties jwtConfig;

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            // Try to decode as Base64 first (if the key is in Base64)
            keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
            log.debug("JWT secret decoded from Base64 ({} bytes)", keyBytes.length);
        } catch (IllegalArgumentException e) {
            // If not Base64, use as plain text
            keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
            log.debug("JWT secret used as plain text ({} bytes)", keyBytes.length);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Integer userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtConfig.getExpiration().getDays(), ChronoUnit.DAYS);

        String token = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();

        log.info("Token generated successfully for user ID: {}", userId);
        return token;
    }

    public boolean isTokenValid(String token) {
        try {
            if (token == null || token.isEmpty()) {
                log.info("Token not found or empty");
                return false;
            }

            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            boolean isValid = expiration.after(new Date());

            if (!isValid) {
                log.info("Token expired. Expiration date: {}", expiration);
            }

            return isValid;
        } catch (ExpiredJwtException e) {
            log.info("Token expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Integer.parseInt(claims.getSubject());
        } catch (JwtException e) {
            log.error("Error getting user ID from token: {}", e.getMessage());
            return null;
        }
    }

    public String renewToken(String oldToken) {
        try {
            // Validate that the token is not empty
            if (oldToken == null || oldToken.isEmpty()) {
                log.warn("Attempt to renew empty or null token");
                return null;
            }

            // First check if the token is still valid
            // No point in renewing a token that has not yet expired
            Integer userId;
            Date expirationDate;
            boolean isExpired = false;

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(oldToken)
                        .getPayload();
                userId = Integer.parseInt(claims.getSubject());
                expirationDate = claims.getExpiration();

                // Calculate remaining time until expiration
                long timeUntilExpiration = expirationDate.getTime() - System.currentTimeMillis();
                long hoursUntilExpiration = timeUntilExpiration / (1000 * 60 * 60);

                // Only renew if the token expires in less than 24 hours
                if (timeUntilExpiration > 0 && hoursUntilExpiration >= 24) {
                    log.warn("Attempt to renew valid token expiring in {} hours. User ID: {}. Not necessary to renew yet.",
                            hoursUntilExpiration, userId);
                    return null;
                }

                log.info("Renewing token expiring in {} hours for user ID: {}",
                        hoursUntilExpiration, userId);

            } catch (ExpiredJwtException e) {
                // If the token is expired, we can still get the userId from the claims
                userId = Integer.parseInt(e.getClaims().getSubject());
                isExpired = true;
                log.info("Renewing expired token for user ID: {}", userId);
            }

            String newToken = generateToken(userId);
            log.info("Token renewed successfully for user ID: {} (previous token was {})",
                    userId, isExpired ? "expired" : "expiring soon");
            return newToken;
        } catch (JwtException e) {
            log.error("Error renewing token - Invalid or corrupted token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error renewing token", e);
            return null;
        }
    }

    public Date getExpirationDate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration();
        } catch (JwtException e) {
            log.error("Error getting token expiration date: {}", e.getMessage());
            return null;
        }
    }
}

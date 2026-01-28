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
            // Intentar decodificar como Base64 primero (si la clave está en Base64)
            keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
            log.debug("JWT secret decodificada desde Base64 ({} bytes)", keyBytes.length);
        } catch (IllegalArgumentException e) {
            // Si no es Base64, usar como texto plano
            keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
            log.debug("JWT secret usada como texto plano ({} bytes)", keyBytes.length);
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

        log.info("Token generado exitosamente para usuario ID: {}", userId);
        return token;
    }

    public boolean isTokenValid(String token) {
        try {
            if (token == null || token.isEmpty()) {
                log.info("Token no encontrado o vacío");
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
                log.info("Token expirado. Fecha de expiración: {}", expiration);
            }

            return isValid;
        } catch (ExpiredJwtException e) {
            log.info("Token expirado: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("Error validando token: {}", e.getMessage());
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
            log.error("Error obteniendo user ID del token: {}", e.getMessage());
            return null;
        }
    }

    public String renewToken(String oldToken) {
        try {
            Integer userId = getUserIdFromToken(oldToken);
            if (userId != null) {
                String newToken = generateToken(userId);
                log.info("Token renovado exitosamente para usuario ID: {}", userId);
                return newToken;
            }
            return null;
        } catch (Exception e) {
            log.error("Error renovando token", e);
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
            log.error("Error obteniendo fecha de expiración del token: {}", e.getMessage());
            return null;
        }
    }
}

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
            // Validar que el token no esté vacío
            if (oldToken == null || oldToken.isEmpty()) {
                log.warn("Intento de renovar token vacío o nulo");
                return null;
            }

            // Primero verificar si el token todavía es válido
            // No tiene sentido renovar un token que aún no ha expirado
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

                // Calcular tiempo restante hasta la expiración
                long timeUntilExpiration = expirationDate.getTime() - System.currentTimeMillis();
                long hoursUntilExpiration = timeUntilExpiration / (1000 * 60 * 60);

                // Solo renovar si el token expira en menos de 24 horas
                if (timeUntilExpiration > 0 && hoursUntilExpiration >= 24) {
                    log.warn("Intento de renovar token válido que expira en {} horas. Usuario ID: {}. " +
                            "No es necesario renovar aún.", hoursUntilExpiration, userId);
                    return null;
                }

                log.info("Renovando token que expira en {} horas para usuario ID: {}",
                        hoursUntilExpiration, userId);

            } catch (ExpiredJwtException e) {
                // Si el token está expirado, aún podemos obtener el userId de las claims
                userId = Integer.parseInt(e.getClaims().getSubject());
                isExpired = true;
                log.info("Renovando token expirado para usuario ID: {}", userId);
            }

            String newToken = generateToken(userId);
            log.info("Token renovado exitosamente para usuario ID: {} (token anterior estaba {})",
                    userId, isExpired ? "expirado" : "próximo a expirar");
            return newToken;
        } catch (JwtException e) {
            log.error("Error renovando token - Token inválido o corrupto: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error inesperado renovando token", e);
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

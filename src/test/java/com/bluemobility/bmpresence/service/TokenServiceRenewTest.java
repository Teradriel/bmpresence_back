package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.config.JwtConfigProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenServiceRenewTest {

    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key-1234";
    private final JwtConfigProperties config = new JwtConfigProperties();
    private final TokenService service;

    TokenServiceRenewTest() {
        config.setSecret(SECRET);
        service = new TokenService(config);
    }

    private String tokenExpiringIn(long hours) {
        Instant now = Instant.now();
        return Jwts.builder().subject("5").issuedAt(Date.from(now.minus(1, ChronoUnit.DAYS)))
                .expiration(Date.from(now.plus(hours, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test
    void defaultsAreSevenDays() {
        assertEquals(7, config.getExpiration().getDays());
        assertEquals(7, config.getRenewGraceDays());
    }

    @Test
    void renewsOnlyInLastDayOrWithinGraceWindow() {
        assertNull(service.renewToken(tokenExpiringIn(48)), "ancora lontano dalla scadenza");
        assertNotNull(service.renewToken(tokenExpiringIn(12)), "ultime 24 ore");
        assertNotNull(service.renewToken(tokenExpiringIn(-24 * 3)), "scaduto da 3 giorni");
        assertNull(service.renewToken(tokenExpiringIn(-24 * 8)), "scaduto da oltre 7 giorni");
        assertEquals(5, service.getUserIdFromToken(service.renewToken(tokenExpiringIn(12))));
    }
}

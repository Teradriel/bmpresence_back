package com.bluemobility.bmpresence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfigProperties {

    /**
     * Clave secreta para firmar los tokens JWT.
     * Debe ser al menos de 256 bits para HMAC-SHA256.
     */
    private String secret;

    /**
     * Configuración de expiración del token
     */
    private Expiration expiration = new Expiration();

    @Data
    public static class Expiration {
        /**
         * Giorni di validità del token (default 7)
         */
        private int days = 7;
    }

    /**
     * Giorni dopo la scadenza in cui un token scaduto si può ancora rinnovare (default 7).
     */
    private int renewGraceDays = 7;
}

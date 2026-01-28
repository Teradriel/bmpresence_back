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
         * Días hasta que expire el token (por defecto 30)
         */
        private int days = 30;
    }
}

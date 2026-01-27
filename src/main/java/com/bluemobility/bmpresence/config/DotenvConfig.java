package com.bluemobility.bmpresence.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(System.getProperty("user.dir"))
                    .ignoreIfMissing()
                    .load();

            Map<String, Object> envMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                envMap.put(entry.getKey(), entry.getValue());
                System.out.println("✓ Cargada variable: " + entry.getKey() + " = "
                        + (entry.getKey().contains("PASSWORD") ? "***" : entry.getValue()));
            });

            environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", envMap));
            System.out.println("✓ Variables .env cargadas ANTES de inicializar Spring");

        } catch (Exception e) {
            System.err.println("✗ Error cargando .env: " + e.getMessage());
        }
    }
}

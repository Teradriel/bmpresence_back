package com.bluemobility.bmpresence.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers("/actuator/health/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/auth/login",
                                                                "/api/auth/register", "/api/auth/restore-session",
                                                                "/api/auth/renew-token")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/auth/validate-token")
                                                .permitAll()
                                                .requestMatchers("/api/auth/admin-reset-password", "/api/auth/admin-create-user")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/email/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/resources/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/resources/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/resources/**")
                                                .hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(
                                "https://bm-presence-web.vercel.app"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}

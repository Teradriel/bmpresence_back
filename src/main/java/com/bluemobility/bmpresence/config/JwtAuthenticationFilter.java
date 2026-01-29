package com.bluemobility.bmpresence.config;

import com.bluemobility.bmpresence.service.TokenService;
import com.bluemobility.bmpresence.service.UserService;
import com.bluemobility.bmpresence.model.User;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {

        // Obtener el header Authorization
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No se encontró token JWT en la petición a: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extraer el token (quitar "Bearer ")
            String token = authHeader.substring(7);

            // Validar el token
            if (!tokenService.isTokenValid(token)) {
                log.warn("Token JWT inválido o expirado");
                filterChain.doFilter(request, response);
                return;
            }

            // Obtener el userId del token
            Integer userId = tokenService.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("No se pudo extraer el userId del token");
                filterChain.doFilter(request, response);
                return;
            }

            // Verificar si ya está autenticado
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Obtener el usuario de la base de datos
            User user = userService.findById(userId);

            if (user == null || !user.getActive()) {
                log.warn("Usuario no encontrado o inactivo: {}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            // Crear las authorities basadas en el rol
            SimpleGrantedAuthority authority = user.getIsAdmin()
                    ? new SimpleGrantedAuthority("ROLE_ADMIN")
                    : new SimpleGrantedAuthority("ROLE_USER");

            // Crear el objeto de autenticación
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    Collections.singletonList(authority));

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Establecer la autenticación en el contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Usuario autenticado exitosamente: {} (ID: {})", user.getUsername(), userId);

        } catch (Exception e) {
            log.error("Error procesando el token JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

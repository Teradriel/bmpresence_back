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

        // Get the Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found in request to: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extraer el token (quitar "Bearer ")
            String token = authHeader.substring(7);

            // Validar el token
            if (!tokenService.isTokenValid(token)) {
                log.warn("Invalid or expired JWT token");
                filterChain.doFilter(request, response);
                return;
            }

            // Get the userId from the token
            Integer userId = tokenService.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("Could not extract userId from token");
                filterChain.doFilter(request, response);
                return;
            }

            // Check if already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Get the user from the database
            User user = userService.findById(userId);

            if (user == null || !user.getActive()) {
                log.warn("User not found or inactive: {}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            // Create authorities based on role
            SimpleGrantedAuthority authority = user.getIsAdmin()
                    ? new SimpleGrantedAuthority("ROLE_ADMIN")
                    : new SimpleGrantedAuthority("ROLE_USER");

            // Create the authentication object
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    Collections.singletonList(authority));

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("User authenticated successfully: {} (ID: {})", user.getUsername(), userId);

        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

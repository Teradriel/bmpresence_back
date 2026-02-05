package com.bluemobility.bmpresence.controller;

import com.bluemobility.bmpresence.dto.UserDTO;
import com.bluemobility.bmpresence.model.User;
import com.bluemobility.bmpresence.service.AuthenticationService;
import com.bluemobility.bmpresence.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        AuthenticationService.AuthenticationResponse response = authenticationService.login(request.getUsername(),
                request.getPassword());

        if (response.isSuccess()) {
            return ResponseEntity.ok(new LoginResponse(
                    true,
                    response.getMessage(),
                    UserDTO.fromUser(response.getUser()),
                    response.getToken()));
        } else {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    false,
                    response.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        AuthenticationService.AuthenticationResponse response = authenticationService.registerUser(
                request.getName(),
                request.getLastName(),
                request.getEmail(),
                request.getUsername(),
                request.getPassword(),
                request.getIsAdmin());

        if (response.isSuccess()) {
            return ResponseEntity.ok(new MessageResponse(
                    true,
                    response.getMessage()));
        } else {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    false,
                    response.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        authenticationService.logout();
        return ResponseEntity.ok(new MessageResponse(
                true,
                "Logout effettuato con successo"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        boolean success = authenticationService.changePassword(
                request.getCurrentPassword(),
                request.getNewPassword());

        if (success) {
            return ResponseEntity.ok(new MessageResponse(
                    true,
                    "Password cambiata con successo"));
        } else {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    false,
                    "Errore durante il cambio della password"));
        }
    }

    @PostMapping("/admin-reset-password")
    public ResponseEntity<?> adminResetPassword(@RequestBody AdminResetPasswordRequest request) {
        AuthenticationService.AuthenticationResponse response = authenticationService.adminResetPassword(
                request.getUserId(),
                request.getNewPassword(),
                request.getForceChangeOnNextLogin());

        if (response.isSuccess()) {
            return ResponseEntity.ok(new MessageResponse(
                    true,
                    response.getMessage()));
        } else {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    false,
                    response.getMessage()));
        }
    }

    @PostMapping("/restore-session")
    public ResponseEntity<?> restoreSession(@RequestBody RestoreSessionRequest request) {
        boolean success = authenticationService.restoreSession(request.getToken());

        if (success) {
            User currentUser = authenticationService.getCurrentUser();
            return ResponseEntity.ok(new SessionResponse(
                    true,
                    "Sessione restaurata con successo",
                    UserDTO.fromUser(currentUser)));
        } else {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    false,
                    "Impossibile restaurare la sessione"));
        }
    }

    @PostMapping("/renew-token")
    public ResponseEntity<?> renewToken(@RequestBody RenewTokenRequest request, HttpServletRequest httpRequest) {
        // Log de la petición para rastrear llamadas múltiples
        String userAgent = httpRequest.getHeader("User-Agent");
        String referer = httpRequest.getHeader("Referer");
        log.warn("⚠️ Llamada a /renew-token - User-Agent: {}, Referer: {}", userAgent, referer);

        // Validar que se envió un token
        if (request.getToken() == null || request.getToken().isEmpty()) {
            log.warn("Intento de renovar sin proporcionar token");
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    false,
                    "Token non fornito"));
        }

        String newToken = tokenService.renewToken(request.getToken());

        if (newToken != null) {
            return ResponseEntity.ok(new TokenResponse(
                    true,
                    "Token rinnovato con successo",
                    newToken));
        } else {
            // Retornar 401 para que el frontend sepa que debe hacer login nuevamente
            return ResponseEntity.status(401).body(new ErrorResponse(
                    false,
                    "Impossibile rinnovare il token - token invalido o corrotto"));
        }
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    false,
                    "Token non fornito"));
        }

        String token = authHeader.substring(7);
        boolean isValid = tokenService.isTokenValid(token);

        if (isValid) {
            return ResponseEntity.ok(new ValidationResponse(true));
        } else {
            // Retornar 401 si el token no es válido
            return ResponseEntity.status(401).body(new ValidationResponse(false));
        }
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser() {
        User currentUser = authenticationService.getCurrentUser();

        if (currentUser != null) {
            return ResponseEntity.ok(UserDTO.fromUser(currentUser));
        } else {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    false,
                    "Nessun utente autenticato"));
        }
    }

    // DTOs
    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    static class RegisterRequest {
        private String name;
        private String lastName;
        private String email;
        private String username;
        private String password;
        private Boolean isAdmin;
    }

    @Data
    static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }

    @Data
    static class AdminResetPasswordRequest {
        private Integer userId;
        private String newPassword;
        private Boolean forceChangeOnNextLogin;
    }

    @Data
    static class RestoreSessionRequest {
        private String token;
    }

    @Data
    static class RenewTokenRequest {
        private String token;
    }

    @Data
    static class LoginResponse {
        private final boolean success;
        private final String message;
        private final UserDTO user;
        private final String token;
    }

    @Data
    static class MessageResponse {
        private final boolean success;
        private final String message;
    }

    @Data
    static class ErrorResponse {
        private final boolean success;
        private final String message;
    }

    @Data
    static class SessionResponse {
        private final boolean success;
        private final String message;
        private final UserDTO user;
    }

    @Data
    static class TokenResponse {
        private final boolean success;
        private final String message;
        private final String token;
    }

    @Data
    static class ValidationResponse {
        private final boolean valid;
    }
}

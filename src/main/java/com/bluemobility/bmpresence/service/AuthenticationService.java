package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.model.User;
import com.bluemobility.bmpresence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    @Transactional
    public boolean changePassword(String currentPassword, String newPassword) {
        try {
            if (currentUser == null) {
                return false;
            }

            boolean isCurrentPasswordValid = verifyPassword(
                    currentPassword,
                    currentUser.getHashedPassword(),
                    currentUser.getSalt());

            if (!isCurrentPasswordValid) {
                log.warn("Cambio de contraseña fallido: Contraseña actual inválida para usuario {}",
                        currentUser.getUsername());
                return false;
            }

            String newSalt = generateSalt();
            String newHashedPassword = hashPassword(newPassword, newSalt);

            currentUser.setSalt(newSalt);
            currentUser.setHashedPassword(newHashedPassword);

            userRepository.save(currentUser);

            log.info("Contraseña cambiada exitosamente para usuario: {}", currentUser.getUsername());
            return true;
        } catch (Exception e) {
            log.error("Error durante el cambio de contraseña para usuario: {}",
                    currentUser != null ? currentUser.getUsername() : "unknown", e);
            return false;
        }
    }

    @Transactional
    public AuthenticationResponse login(String username, String password) {
        try {
            if (username == null || username.trim().isEmpty() ||
                    password == null || password.isEmpty()) {
                return new AuthenticationResponse(
                        false,
                        "El nombre de usuario y la contraseña son obligatorios",
                        null,
                        null);
            }

            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) {
                log.warn("Intento de inicio de sesión fallido: Usuario no encontrado - {}", username);
                return new AuthenticationResponse(
                        false,
                        "Nome utente o password non corretti",
                        null,
                        null);
            }

            if (!user.getActive()) {
                log.warn("Intento de inicio de sesión fallido: Usuario inactivo - {}", username);
                return new AuthenticationResponse(
                        false,
                        "L'account utente è disattivato",
                        null,
                        null);
            }

            boolean isPasswordValid = verifyPassword(
                    password,
                    user.getHashedPassword(),
                    user.getSalt());

            if (!isPasswordValid) {
                log.warn("Intento de inicio de sesión fallido: Contraseña inválida - {}", username);
                return new AuthenticationResponse(
                        false,
                        "Nome utente o password non corretti",
                        null,
                        null);
            }

            updateLastActive(user);

            currentUser = user;

            String token = tokenService.generateToken(user.getId());

            log.info("Usuario inició sesión exitosamente: {}", username);

            return new AuthenticationResponse(
                    true,
                    "Login effettuato con successo",
                    user,
                    token);
        } catch (Exception e) {
            log.error("Error durante el inicio de sesión para usuario: {}", username, e);
            return new AuthenticationResponse(
                    false,
                    "Errore durante l'accesso. Si prega di riprovare.",
                    null,
                    null);
        }
    }

    public void logout() {
        if (currentUser != null) {
            log.info("Usuario cerró sesión: {}", currentUser.getUsername());
            currentUser = null;
        }
    }

    @Transactional
    public boolean restoreSession(String token) {
        try {
            boolean isTokenValid = tokenService.isTokenValid(token);
            if (!isTokenValid) {
                log.info("No se encontró un token válido o el token ha expirado");
                return false;
            }

            Integer userId = tokenService.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("Token encontrado pero no se encontró ID de usuario");
                return false;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("Usuario ID {} no encontrado en la base de datos", userId);
                return false;
            }

            if (!user.getActive()) {
                log.warn("Usuario {} está inactivo", user.getUsername());
                return false;
            }

            currentUser = user;
            updateLastActive(user);

            log.info("Sesión restaurada exitosamente para usuario: {}", user.getUsername());
            return true;
        } catch (Exception e) {
            log.error("Error restaurando sesión", e);
            return false;
        }
    }

    @Transactional
    public AuthenticationResponse registerUser(
            String name,
            String lastName,
            String email,
            String username,
            String password,
            Boolean isAdmin) {
        try {
            if (username == null || username.trim().isEmpty() ||
                    password == null || password.isEmpty()) {
                return new AuthenticationResponse(
                        false,
                        "Il nome utente e la password sono obbligatori",
                        null,
                        null);
            }

            if (email == null || email.trim().isEmpty()) {
                return new AuthenticationResponse(
                        false,
                        "L'email è obbligatoria",
                        null,
                        null);
            }

            // Verificar si el usuario ya existe
            if (userRepository.findByUsername(username).isPresent()) {
                return new AuthenticationResponse(
                        false,
                        "Il nome utente è già in uso",
                        null,
                        null);
            }

            if (userRepository.findByEmail(email).isPresent()) {
                return new AuthenticationResponse(
                        false,
                        "L'email è già registrata",
                        null,
                        null);
            }

            // Generar salt y hashear la contraseña
            String salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);

            User newUser = new User();
            newUser.setName(name);
            newUser.setLastName(lastName);
            newUser.setEmail(email);
            newUser.setUsername(username);
            newUser.setSalt(salt);
            newUser.setHashedPassword(hashedPassword);
            newUser.setActive(true);
            newUser.setIsAdmin(isAdmin != null ? isAdmin : false);
            newUser.setIsOnline(false);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setLastUpdated(LocalDateTime.now());

            User savedUser = userRepository.save(newUser);

            log.info("Nuevo usuario registrado: {}", username);

            return new AuthenticationResponse(
                    true,
                    "Utente registrato con successo",
                    savedUser,
                    null);
        } catch (Exception e) {
            log.error("Error durante el registro de usuario: {}", username, e);
            return new AuthenticationResponse(
                    false,
                    "Errore durante la registrazione dell'utente. Si prega di riprovare.",
                    null,
                    null);
        }
    }

    private String generateSalt() {
        byte[] saltBytes = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    private String hashPassword(String password, String salt) {
        try {
            if (salt == null || salt.isEmpty()) {
                salt = generateSalt();
            }

            byte[] saltBytes = Base64.getDecoder().decode(salt);
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] saltedPassword = new byte[saltBytes.length + passwordBytes.length];

            System.arraycopy(saltBytes, 0, saltedPassword, 0, saltBytes.length);
            System.arraycopy(passwordBytes, 0, saltedPassword, saltBytes.length, passwordBytes.length);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(saltedPassword);

            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error hasheando contraseña", e);
            throw new RuntimeException("Error hasheando contraseña", e);
        }
    }

    @Transactional
    private void updateLastActive(User user) {
        try {
            user.setLastActiveAt(LocalDateTime.now());
            user.setIsOnline(true);
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Error actualizando last active para usuario: {}", user.getUsername(), e);
        }
    }

    private boolean verifyPassword(String password, String storedHash, String salt) {
        if (storedHash == null || storedHash.isEmpty() || salt == null || salt.isEmpty()) {
            return false;
        }

        String computedHash = hashPassword(password, salt);
        return computedHash.equals(storedHash);
    }

    // Clase interna para la respuesta de autenticación
    public static class AuthenticationResponse {
        private final boolean success;
        private final String message;
        private final User user;
        private final String token;

        public AuthenticationResponse(boolean success, String message, User user, String token) {
            this.success = success;
            this.message = message;
            this.user = user;
            this.token = token;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }

        public String getToken() {
            return token;
        }
    }
}

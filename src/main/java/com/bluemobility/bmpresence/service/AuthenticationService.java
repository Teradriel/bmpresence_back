package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.dto.UserDTO;
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
                log.warn("Password change failed: Invalid current password for user {}",
                        currentUser.getUsername());
                return false;
            }

            String newSalt = generateSalt();
            String newHashedPassword = hashPassword(newPassword, newSalt);

            currentUser.setSalt(newSalt);
            currentUser.setHashedPassword(newHashedPassword);
            currentUser.setMustChangePassword(false);

            userRepository.save(currentUser);

            log.info("Password changed successfully for user: {}", currentUser.getUsername());
            return true;
        } catch (Exception e) {
            log.error("Error during password change for user: {}",
                    currentUser != null ? currentUser.getUsername() : "unknown", e);
            return false;
        }
    }

    @Transactional
    public AuthenticationResponse adminResetPassword(Integer userId, String newPassword,
            Boolean forceChangeOnNextLogin) {
        try {
            if (currentUser == null || !currentUser.getIsAdmin()) {
                log.warn("Password reset attempt without admin permissions");
                return new AuthenticationResponse(
                        false,
                        "Accesso negato. Solo gli amministratori possono resettare le password",
                        null,
                        null);
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                return new AuthenticationResponse(
                        false,
                        "La nuova password non può essere vuota",
                        null,
                        null);
            }

            User userToReset = userRepository.findById(userId).orElse(null);
            if (userToReset == null) {
                log.warn("Reset attempt for user ID {} not found", userId);
                return new AuthenticationResponse(
                        false,
                        "Utente non trovato",
                        null,
                        null);
            }

            String newSalt = generateSalt();
            String newHashedPassword = hashPassword(newPassword, newSalt);

            userToReset.setSalt(newSalt);
            userToReset.setHashedPassword(newHashedPassword);
            userToReset.setMustChangePassword(forceChangeOnNextLogin != null ? forceChangeOnNextLogin : true);

            userRepository.save(userToReset);

            log.info("Admin {} reset password for user: {}",
                    currentUser.getUsername(), userToReset.getUsername());

            return new AuthenticationResponse(
                    true,
                    "Password resettata con successo",
                    null,
                    null);
        } catch (Exception e) {
            log.error("Error during admin password reset", e);
            return new AuthenticationResponse(
                    false,
                    "Errore durante il reset della password",
                    null,
                    null);
        }
    }

    @Transactional
    public AuthenticationResponse login(String username, String password) {
        try {
            if (username == null || username.trim().isEmpty() ||
                    password == null || password.isEmpty()) {
                return new AuthenticationResponse(
                        false,
                        "Il nome utente e la password sono obbligatori",
                        null,
                        null);
            }

            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) {
                log.warn("Failed login attempt: User not found - {}", username);
                return new AuthenticationResponse(
                        false,
                        "Nome utente o password non corretti",
                        null,
                        null);
            }

            if (!user.getActive()) {
                log.warn("Failed login attempt: Inactive user - {}", username);
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
                log.warn("Failed login attempt: Invalid password - {}", username);
                return new AuthenticationResponse(
                        false,
                        "Nome utente o password non corretti",
                        null,
                        null);
            }

            updateLastActive(user);

            currentUser = user;

            String token = tokenService.generateToken(user.getId());

            log.info("User logged in successfully: {}", username);

            String message = "Login effettuato con successo";
            if (user.getMustChangePassword() != null && user.getMustChangePassword()) {
                message = "Login effettuato. È necessario cambiare la password";
            }

            return new AuthenticationResponse(
                    true,
                    message,
                    UserDTO.fromUser(user),
                    token);
        } catch (Exception e) {
            log.error("Error during login for user: {}", username, e);
            return new AuthenticationResponse(
                    false,
                    "Errore durante l'accesso. Si prega di riprovare.",
                    null,
                    null);
        }
    }

    public void logout() {
        if (currentUser != null) {
            log.info("User logged out: {}", currentUser.getUsername());
            currentUser = null;
        }
    }

    @Transactional
    public boolean restoreSession(String token) {
        try {
            boolean isTokenValid = tokenService.isTokenValid(token);
            if (!isTokenValid) {
                log.info("No valid token found or token has expired");
                return false;
            }

            Integer userId = tokenService.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("Token found but no user ID found");
                return false;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User ID {} not found in database", userId);
                return false;
            }

            if (!user.getActive()) {
                log.warn("User {} is inactive", user.getUsername());
                return false;
            }

            currentUser = user;
            updateLastActive(user);

            log.info("Session restored successfully for user: {}", user.getUsername());
            return true;
        } catch (Exception e) {
            log.error("Error restoring session", e);
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

            // Check if user already exists
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

            // Generate salt and hash the password
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

            log.info("New user registered: {}", username);

            return new AuthenticationResponse(
                    true,
                    "Utente registrato con successo",
                    UserDTO.fromUser(savedUser),
                    null);
        } catch (Exception e) {
            log.error("Error during user registration: {}", username, e);
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
            log.error("Error hashing password", e);
            throw new RuntimeException("Error hashing password", e);
        }
    }

    @Transactional
    private void updateLastActive(User user) {
        try {
            user.setLastActiveAt(LocalDateTime.now());
            user.setIsOnline(true);
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Error updating last active for user: {}", user.getUsername(), e);
        }
    }

    private boolean verifyPassword(String password, String storedHash, String salt) {
        if (storedHash == null || storedHash.isEmpty() || salt == null || salt.isEmpty()) {
            return false;
        }

        String computedHash = hashPassword(password, salt);
        return computedHash.equals(storedHash);
    }

    // Inner class for authentication response
    public static class AuthenticationResponse {
        private final boolean success;
        private final String message;
        private final UserDTO user;
        private final String token;

        public AuthenticationResponse(boolean success, String message, UserDTO user, String token) {
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

        public UserDTO getUser() {
            return user;
        }

        public String getToken() {
            return token;
        }
    }
}

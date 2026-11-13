package com.bluemobility.bmpresence.controller;

import com.bluemobility.bmpresence.dto.UserDTO;
import com.bluemobility.bmpresence.model.User;
import com.bluemobility.bmpresence.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll().stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList()));
    }

    @GetMapping("/active")
    public ResponseEntity<List<UserDTO>> getActiveUsers() {
        return ResponseEntity.ok(userService.findAllActive().stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList()));
    }

    /** Colleghi attivi per i selettori: solo i campi che servono, visibili a ogni utente collegato. */
    @GetMapping("/colleagues")
    public ResponseEntity<List<Colleague>> getColleagues() {
        return ResponseEntity.ok(userService.findAllActive().stream()
                .map(u -> new Colleague(u.getId(), u.getName(), u.getLastName(), u.getIsOnline()))
                .collect(Collectors.toList()));
    }

    public record Colleague(Integer id, String name, String lastName, Boolean isOnline) {
    }

    @GetMapping("/online")
    public ResponseEntity<List<UserDTO>> getOnlineUsers() {
        return ResponseEntity.ok(userService.findOnlineUsers().stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Integer id, @AuthenticationPrincipal User me) {
        if (!selfOrAdmin(me, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(UserDTO.fromUser(userService.findById(id)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(UserDTO.fromUser(userService.findByEmail(email)));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody User user) {
        User createdUser = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDTO.fromUser(createdUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Integer id, @RequestBody User user,
            @AuthenticationPrincipal User me) {
        if (!selfOrAdmin(me, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // ruolo e stato attivo li cambia solo un amministratore
        User updatedUser = userService.update(id, user, Boolean.TRUE.equals(me.getIsAdmin()));
        return ResponseEntity.ok(UserDTO.fromUser(updatedUser));
    }

    @PutMapping("/{id}/online-status")
    public ResponseEntity<UserDTO> updateOnlineStatus(@PathVariable Integer id, @RequestParam Boolean isOnline,
            @AuthenticationPrincipal User me) {
        if (!selfOrAdmin(me, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        User updatedUser = userService.updateOnlineStatus(id, isOnline);
        return ResponseEntity.ok(UserDTO.fromUser(updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable Integer id) {
        userService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }

    private static boolean selfOrAdmin(User me, Integer id) {
        return me != null && (Boolean.TRUE.equals(me.getIsAdmin()) || me.getId().equals(id));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}

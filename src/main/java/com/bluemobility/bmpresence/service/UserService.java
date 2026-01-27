package com.bluemobility.bmpresence.service;

import com.bluemobility.bmpresence.model.User;
import com.bluemobility.bmpresence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findAllActive() {
        return userRepository.findByActiveTrue();
    }

    public User findById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
    }

    public List<User> findOnlineUsers() {
        return userRepository.findByIsOnlineTrue();
    }

    @Transactional
    public User create(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User update(Integer id, User userDetails) {
        User user = findById(id);

        user.setName(userDetails.getName());
        user.setLastName(userDetails.getLastName());
        user.setEmail(userDetails.getEmail());
        user.setUsername(userDetails.getUsername());
        user.setAvatarUrl(userDetails.getAvatarUrl());
        user.setStatus(userDetails.getStatus());
        user.setStatusEmoji(userDetails.getStatusEmoji());
        user.setStatusText(userDetails.getStatusText());
        user.setIsOnline(userDetails.getIsOnline());
        user.setIsAdmin(userDetails.getIsAdmin());
        user.setActive(userDetails.getActive());

        if (userDetails.getIsOnline() != null && userDetails.getIsOnline()) {
            user.setLastActiveAt(LocalDateTime.now());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Integer id) {
        User user = findById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void hardDelete(Integer id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public User updateOnlineStatus(Integer id, Boolean isOnline) {
        User user = findById(id);
        user.setIsOnline(isOnline);
        if (isOnline) {
            user.setLastActiveAt(LocalDateTime.now());
        }
        return userRepository.save(user);
    }
}

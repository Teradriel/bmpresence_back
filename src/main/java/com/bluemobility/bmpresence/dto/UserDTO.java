package com.bluemobility.bmpresence.dto;

import com.bluemobility.bmpresence.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Integer id;
    private String username;
    private String email;
    private String name;
    private String lastName;
    private Boolean isAdmin;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;

    // Constructor estático para convertir User a UserDTO
    public static UserDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getLastName(),
                user.getIsAdmin(),
                user.getActive(),
                user.getCreatedAt(),
                user.getLastActiveAt());
    }
}

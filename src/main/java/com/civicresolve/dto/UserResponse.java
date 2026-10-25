package com.civicresolve.dto;

import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String department;
    private String badgeNumber;
    private boolean enabled;
    private LocalDateTime createdAt;

    public UserResponse(User user) {
        if (user != null) {
            this.id = user.getId();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.fullName = user.getFullName();
            this.email = user.getEmail();
            this.phone = user.getPhone();
            this.role = user.getRole();
            this.department = user.getDepartment();
            this.badgeNumber = user.getBadgeNumber();
            this.enabled = user.isEnabled();
            this.createdAt = user.getCreatedAt();
        }
    }
}

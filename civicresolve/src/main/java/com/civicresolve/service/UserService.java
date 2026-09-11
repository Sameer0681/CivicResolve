package com.civicresolve.service;

import com.civicresolve.dto.RegisterRequest;
import com.civicresolve.entity.User;

import java.util.Optional;

public interface UserService {
    User registerNewUser(RegisterRequest request);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    String createPasswordResetToken(String email);
    boolean validatePasswordResetToken(String token);
    boolean resetPassword(String token, String newPassword);
}

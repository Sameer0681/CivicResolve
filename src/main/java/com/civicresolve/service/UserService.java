package com.civicresolve.service;

import com.civicresolve.dto.RegisterRequest;
import com.civicresolve.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User registerNewUser(RegisterRequest request);

    User updateCitizenPhone(Long citizenId, String rawPhone);

    User createOfficer(com.civicresolve.dto.OfficerCreateRequest request);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    List<User> findOfficers();

    List<User> findOfficersByDepartment(String department);

    boolean existsByEmail(String email);

    String createPasswordResetToken(String email);

    boolean validatePasswordResetToken(String token);

    boolean resetPassword(String token, String newPassword);
}

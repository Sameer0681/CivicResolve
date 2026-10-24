package com.civicresolve.service;

import com.civicresolve.dto.RegisterRequest;
import com.civicresolve.entity.PasswordResetToken;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.repository.PasswordResetTokenRepository;
import com.civicresolve.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerNewUser(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new com.civicresolve.exception.ConflictException("An account with email " + email + " already exists.");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new com.civicresolve.exception.BadRequestException("Passwords do not match.");
        }

        // Point 4b: If phone is provided at registration, validate and normalize; if blank, allow null
        String validatedPhone = null;
        if (request.getPhone() != null && !request.getPhone().trim().isBlank()) {
            validatedPhone = OtpServiceImpl.normalizeIndianPhone(request.getPhone().trim());
        }

        // Point 2: Registration always assigns ROLE_CITIZEN
        Role assignedRole = Role.ROLE_CITIZEN;

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .phone(validatedPhone)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .department(request.getDepartment())
                .badgeNumber(request.getBadgeNumber())
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    @Override
    public User updateCitizenPhone(Long citizenId, String rawPhone) {
        User user = userRepository.findById(citizenId)
                .orElseThrow(() -> new com.civicresolve.exception.ResourceNotFoundException("Citizen not found with ID: " + citizenId));

        if (user.getRole() != Role.ROLE_CITIZEN) {
            throw new com.civicresolve.exception.BadRequestException("Phone update via this endpoint is only permitted for citizens");
        }

        String normalized = OtpServiceImpl.normalizeIndianPhone(rawPhone);
        user.setPhone(normalized);
        return userRepository.save(user);
    }

    @Override
    public User createOfficer(com.civicresolve.dto.OfficerCreateRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new com.civicresolve.exception.ConflictException("User with email " + email + " already exists.");
        }

        String name = request.getName().trim();
        String firstName = name;
        String lastName = "";
        int spaceIndex = name.indexOf(' ');
        if (spaceIndex != -1) {
            firstName = name.substring(0, spaceIndex).trim();
            lastName = name.substring(spaceIndex + 1).trim();
        }
        if (lastName.isBlank()) {
            lastName = "Officer";
        }

        String badge = "OFF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        User officer = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_OFFICER)
                .department(request.getDepartment().trim())
                .badgeNumber(badge)
                .enabled(true)
                .build();

        return userRepository.save(officer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findOfficers() {
        return userRepository.findByRole(Role.ROLE_OFFICER);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findOfficersByDepartment(String department) {
        return userRepository.findByDepartmentAndRole(department, Role.ROLE_OFFICER);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    public String createPasswordResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return null;
        }

        User user = userOpt.get();
        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();

        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        return tokenOpt.isPresent() && !tokenOpt.get().isExpired() && tokenOpt.get().getUser().isEnabled();
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        if (!user.isEnabled()) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        return true;
    }
}

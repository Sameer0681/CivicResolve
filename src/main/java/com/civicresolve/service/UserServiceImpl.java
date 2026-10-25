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
import com.civicresolve.entity.EmailOtpVerification;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.exception.ConflictException;
import com.civicresolve.exception.RateLimitException;
import com.civicresolve.repository.EmailOtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpVerificationRepository emailOtpVerificationRepository;
    private final EmailService emailService;
    private final boolean registrationOtpRequired;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           EmailOtpVerificationRepository emailOtpVerificationRepository,
                           EmailService emailService,
                           @Value("${app.registration.email-otp.required:false}") boolean registrationOtpRequired) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailOtpVerificationRepository = emailOtpVerificationRepository;
        this.emailService = emailService;
        this.registrationOtpRequired = registrationOtpRequired;
    }

    public UserServiceImpl(UserRepository userRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder) {
        this(userRepository, passwordResetTokenRepository, passwordEncoder, null, null, false);
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

        // Validate Email OTP if provided or required
        if (request.getOtp() != null && !request.getOtp().trim().isBlank()) {
            if (emailOtpVerificationRepository != null) {
                Optional<EmailOtpVerification> otpOpt = emailOtpVerificationRepository
                        .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, "REGISTRATION");
                if (otpOpt.isEmpty() || otpOpt.get().isExpired() || !otpOpt.get().getOtp().equals(request.getOtp().trim())) {
                    throw new com.civicresolve.exception.BadRequestException("Invalid or expired email verification code. Please request a new code.");
                }
                EmailOtpVerification v = otpOpt.get();
                v.setUsed(true);
                v.setVerified(true);
                emailOtpVerificationRepository.save(v);
            }
        } else if (this.registrationOtpRequired) {
            throw new com.civicresolve.exception.BadRequestException("Email verification code is required to complete registration.");
        }

        // If phone is provided at registration, validate and normalize; if blank, allow null
        String validatedPhone = null;
        if (request.getPhone() != null && !request.getPhone().trim().isBlank()) {
            validatedPhone = OtpServiceImpl.normalizeIndianPhone(request.getPhone().trim());
        }

        // Role assignment: Citizen, Admin, or Officer
        Role assignedRole = Role.ROLE_CITIZEN;
        if (request.getRole() != null) {
            String roleUpper = request.getRole().trim().toUpperCase();
            if (roleUpper.equals("ADMIN") || roleUpper.equals("ROLE_ADMIN")) {
                assignedRole = Role.ROLE_ADMIN;
            } else if (roleUpper.equals("OFFICER") || roleUpper.equals("ROLE_OFFICER")) {
                assignedRole = Role.ROLE_OFFICER;
            }
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .phone(validatedPhone)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .department(request.getDepartment() != null ? request.getDepartment().trim() : (assignedRole == Role.ROLE_ADMIN ? "Administration" : null))
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

    @Override
    public void sendRegistrationEmailOtp(String email, String firstName) {
        if (email == null || email.trim().isBlank()) {
            throw new BadRequestException("Email address is required to send verification code.");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with email " + normalizedEmail + " already exists. Please sign in instead.");
        }

        if (emailOtpVerificationRepository != null) {
            LocalDateTime oneMinuteAgo = LocalDateTime.now().minusSeconds(60);
            long recentCount = emailOtpVerificationRepository.countByEmailAndCreatedAtAfter(normalizedEmail, oneMinuteAgo);
            if (recentCount > 0) {
                throw new RateLimitException("Please wait 60 seconds before requesting another verification code.");
            }

            String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
            EmailOtpVerification verification = EmailOtpVerification.builder()
                    .email(normalizedEmail)
                    .otp(otp)
                    .purpose("REGISTRATION")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .verified(false)
                    .used(false)
                    .build();
            emailOtpVerificationRepository.save(verification);

            if (emailService != null) {
                emailService.sendRegistrationOtp(normalizedEmail, otp, firstName);
            }
        }
    }

    @Override
    public boolean verifyRegistrationEmailOtp(String email, String otp) {
        if (email == null || otp == null || emailOtpVerificationRepository == null) return false;
        String normalizedEmail = email.trim().toLowerCase();
        Optional<EmailOtpVerification> opt = emailOtpVerificationRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(normalizedEmail, "REGISTRATION");
        if (opt.isEmpty() || opt.get().isExpired()) {
            return false;
        }
        if (opt.get().getOtp().equals(otp.trim())) {
            EmailOtpVerification v = opt.get();
            v.setVerified(true);
            emailOtpVerificationRepository.save(v);
            return true;
        }
        return false;
    }

    @Override
    public void sendForgotPasswordEmailOtp(String email) {
        if (email == null || email.trim().isBlank()) {
            throw new BadRequestException("Please enter a valid email address.");
        }
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            log.info("Password reset requested for non-existent or disabled email: {}", normalizedEmail);
            return;
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

        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        if (emailOtpVerificationRepository != null) {
            EmailOtpVerification verification = EmailOtpVerification.builder()
                    .email(normalizedEmail)
                    .otp(otp)
                    .purpose("FORGOT_PASSWORD")
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .verified(false)
                    .used(false)
                    .build();
            emailOtpVerificationRepository.save(verification);
        }

        String resetLink = "http://localhost:8080/reset-password?email=" + normalizedEmail + "&token=" + token;
        if (emailService != null) {
            emailService.sendPasswordResetOtp(normalizedEmail, otp, resetLink);
        }
    }

    @Override
    public boolean resetPasswordWithOtpOrToken(String email, String otpOrToken, String newPassword) {
        if (otpOrToken == null || otpOrToken.trim().isBlank() || newPassword == null || newPassword.length() < 8) {
            return false;
        }

        String code = otpOrToken.trim();

        // 1. Try OTP match
        if (email != null && !email.trim().isBlank() && emailOtpVerificationRepository != null) {
            String normalizedEmail = email.trim().toLowerCase();
            Optional<EmailOtpVerification> opt = emailOtpVerificationRepository
                    .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(normalizedEmail, "FORGOT_PASSWORD");
            if (opt.isPresent() && !opt.get().isExpired() && opt.get().getOtp().equals(code)) {
                Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
                if (userOpt.isPresent() && userOpt.get().isEnabled()) {
                    User user = userOpt.get();
                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(user);

                    EmailOtpVerification v = opt.get();
                    v.setUsed(true);
                    v.setVerified(true);
                    emailOtpVerificationRepository.save(v);

                    passwordResetTokenRepository.deleteByUser(user);
                    return true;
                }
            }
        }

        // 2. Try UUID Token match
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(code);
        if (tokenOpt.isPresent() && !tokenOpt.get().isExpired()) {
            PasswordResetToken prt = tokenOpt.get();
            User user = prt.getUser();
            if (user.isEnabled()) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                passwordResetTokenRepository.delete(prt);
                return true;
            }
        }

        return false;
    }
}

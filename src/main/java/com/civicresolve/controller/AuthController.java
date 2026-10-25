package com.civicresolve.controller;

import com.civicresolve.dto.ForgotPasswordRequest;
import com.civicresolve.dto.OfficerCreateRequest;
import com.civicresolve.dto.RegisterRequest;
import com.civicresolve.dto.UserResponse;
import com.civicresolve.entity.User;
import com.civicresolve.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String registered,
                        @RequestParam(required = false) String resetSuccess) {
        if (registered != null) {
            model.addAttribute("registered", true);
        }
        if (resetSuccess != null) {
            model.addAttribute("resetSuccess", true);
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model, jakarta.servlet.http.HttpSession session) {
        if (!model.containsAttribute("registerRequest")) {
            RegisterRequest req = (RegisterRequest) session.getAttribute("PENDING_REGISTER");
            if (req == null) {
                req = new RegisterRequest();
                req.setRole("CITIZEN");
            }
            model.addAttribute("registerRequest", req);
        }
        return "auth/register";
    }

    @PostMapping("/register/send-otp")
    public String registerSendOtpSubmit(@RequestParam(defaultValue = "CITIZEN") String role,
                                        @RequestParam String firstName,
                                        @RequestParam String lastName,
                                        @RequestParam String email,
                                        @RequestParam(required = false) String phone,
                                        @RequestParam String password,
                                        @RequestParam String confirmPassword,
                                        jakarta.servlet.http.HttpSession session,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {

        RegisterRequest draft = RegisterRequest.builder()
                .role(role)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .password(password)
                .confirmPassword(confirmPassword)
                .build();

        if (firstName == null || firstName.trim().isBlank() || lastName == null || lastName.trim().isBlank()) {
            model.addAttribute("errorMsg", "First name and last name are required.");
            model.addAttribute("registerRequest", draft);
            return "auth/register";
        }

        if (email == null || !email.contains("@")) {
            model.addAttribute("errorMsg", "Please enter a valid email address.");
            model.addAttribute("registerRequest", draft);
            return "auth/register";
        }

        if (password == null || password.length() < 8) {
            model.addAttribute("errorMsg", "Password must be at least 8 characters long.");
            model.addAttribute("registerRequest", draft);
            return "auth/register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            model.addAttribute("registerRequest", draft);
            return "auth/register";
        }

        try {
            userService.sendRegistrationEmailOtp(email.trim(), firstName.trim());
            session.setAttribute("PENDING_REGISTER", draft);
            redirectAttributes.addFlashAttribute("infoMsg", "We sent a 6-digit verification code to " + email.trim());
            return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (com.civicresolve.exception.ConflictException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("registerRequest", draft);
            return "auth/register";
        } catch (Exception ex) {
            model.addAttribute("errorMsg", ex.getMessage() != null ? ex.getMessage() : "Could not send verification code. Please try again.");
            model.addAttribute("registerRequest", draft);
            return "auth/register";
        }
    }

    @GetMapping("/register/verify")
    public String registerVerifyForm(@RequestParam(required = false) String email,
                                     jakarta.servlet.http.HttpSession session,
                                     Model model) {
        RegisterRequest pending = (RegisterRequest) session.getAttribute("PENDING_REGISTER");
        String targetEmail = (pending != null && pending.getEmail() != null) ? pending.getEmail() : email;
        model.addAttribute("email", targetEmail);
        model.addAttribute("role", pending != null ? pending.getRole() : "CITIZEN");
        return "auth/register-verify";
    }

    @PostMapping("/register/verify")
    public String registerVerifySubmit(@RequestParam String otp,
                                       jakarta.servlet.http.HttpSession session,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        RegisterRequest pending = (RegisterRequest) session.getAttribute("PENDING_REGISTER");
        if (pending == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Registration session expired. Please fill the form again.");
            return "redirect:/register";
        }

        if (otp == null || otp.trim().isBlank()) {
            model.addAttribute("errorMsg", "Please enter the 6-digit verification code.");
            model.addAttribute("email", pending.getEmail());
            return "auth/register-verify";
        }

        pending.setOtp(otp.trim());
        try {
            userService.registerNewUser(pending);
            session.removeAttribute("PENDING_REGISTER");
            redirectAttributes.addFlashAttribute("successMsg", "Your account has been registered successfully! Please sign in.");
            return "redirect:/login?registered=true";
        } catch (Exception ex) {
            model.addAttribute("errorMsg", ex.getMessage() != null ? ex.getMessage() : "Invalid or expired verification code.");
            model.addAttribute("email", pending.getEmail());
            return "auth/register-verify";
        }
    }

    @GetMapping("/register/resend-otp")
    public String registerResendOtp(jakarta.servlet.http.HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        RegisterRequest pending = (RegisterRequest) session.getAttribute("PENDING_REGISTER");
        if (pending == null || pending.getEmail() == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "No pending registration found. Please register again.");
            return "redirect:/register";
        }

        try {
            userService.sendRegistrationEmailOtp(pending.getEmail(), pending.getFirstName());
            redirectAttributes.addFlashAttribute("infoMsg", "A fresh verification code has been dispatched to " + pending.getEmail());
            return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(pending.getEmail(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
            return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(pending.getEmail(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMsg", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "auth/register";
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "auth/register";
        }

        try {
            userService.registerNewUser(request);
            redirectAttributes.addFlashAttribute("successMsg", "Your account has been created successfully! Please sign in.");
            return "redirect:/login?registered=true";
        } catch (com.civicresolve.exception.ConflictException | com.civicresolve.exception.BadRequestException | IllegalArgumentException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            return "auth/register";
        } catch (Exception ex) {
            model.addAttribute("errorMsg", "Registration could not be completed. Please try again.");
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(Model model) {
        if (!model.containsAttribute("forgotPasswordRequest")) {
            model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam(required = false) String email,
                                       @Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        String targetEmail = (email != null && !email.isBlank()) ? email.trim() : (request != null ? request.getEmail() : null);
        if (targetEmail == null || !targetEmail.contains("@")) {
            model.addAttribute("errorMsg", "Please enter a valid email address.");
            return "auth/forgot-password";
        }

        try {
            userService.sendForgotPasswordEmailOtp(targetEmail);
            redirectAttributes.addFlashAttribute("successMsg", "A verification code and password reset instructions have been sent to " + targetEmail + ".");
            return "redirect:/reset-password?email=" + java.net.URLEncoder.encode(targetEmail, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam(required = false) String email,
                                    @RequestParam(required = false) String token,
                                    @RequestParam(required = false) String otp,
                                    Model model) {
        model.addAttribute("email", email);
        model.addAttribute("token", token);
        model.addAttribute("otp", (otp != null && !otp.isBlank()) ? otp : token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam(required = false) String email,
                                      @RequestParam(required = false) String token,
                                      @RequestParam(required = false) String otp,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        String code = (otp != null && !otp.isBlank()) ? otp.trim() : (token != null ? token.trim() : "");
        if (code.isBlank()) {
            model.addAttribute("errorMsg", "Verification code is required.");
            model.addAttribute("email", email);
            return "auth/reset-password";
        }

        if (password == null || password.length() < 8) {
            model.addAttribute("errorMsg", "Password must be at least 8 characters long.");
            model.addAttribute("email", email);
            model.addAttribute("otp", code);
            return "auth/reset-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            model.addAttribute("email", email);
            model.addAttribute("otp", code);
            return "auth/reset-password";
        }

        boolean success = userService.resetPasswordWithOtpOrToken(email, code, password);
        if (success) {
            redirectAttributes.addFlashAttribute("successMsg", "Your password has been reset successfully! Please sign in with your new credentials.");
            return "redirect:/login?resetSuccess=true";
        } else {
            model.addAttribute("errorMsg", "Invalid or expired verification code or token. Please request a new one.");
            model.addAttribute("email", email);
            model.addAttribute("token", token);
            model.addAttribute("otp", otp);
            return "auth/reset-password";
        }
    }

    @PostMapping("/api/auth/register/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendRegisterOtp(@RequestParam String email,
                                             @RequestParam(required = false) String name) {
        try {
            userService.sendRegistrationEmailOtp(email, name);
            return ResponseEntity.ok(java.util.Map.of(
                    "status", "SUCCESS",
                    "message", "Verification code sent to " + email
            ));
        } catch (com.civicresolve.exception.ConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("error", ex.getMessage()));
        } catch (com.civicresolve.exception.RateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(java.util.Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/api/auth/register/verify-otp")
    @ResponseBody
    public ResponseEntity<?> verifyRegisterOtp(@RequestParam String email, @RequestParam String otp) {
        boolean valid = userService.verifyRegistrationEmailOtp(email, otp);
        if (valid) {
            return ResponseEntity.ok(java.util.Map.of("status", "VERIFIED", "message", "Email verified successfully!"));
        } else {
            return ResponseEntity.badRequest().body(java.util.Map.of("status", "INVALID", "error", "Invalid or expired OTP."));
        }
    }

    // REST API Endpoint for user registration (Point 2: role is always ROLE_CITIZEN)
    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<UserResponse> apiRegister(@Valid @RequestBody RegisterRequest request) {
        request.setRole("CITIZEN");
        User created = userService.registerNewUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(created));
    }

    // REST API Endpoint for Admin to create Field Officers
    @PostMapping("/api/admin/users/officer")
    @ResponseBody
    public ResponseEntity<UserResponse> apiCreateOfficer(@Valid @RequestBody OfficerCreateRequest request) {
        User created = userService.createOfficer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(created));
    }

    // REST API Endpoint for citizen to update/add phone number (Point 4c)
    @PutMapping("/api/auth/profile/phone")
    @ResponseBody
    public ResponseEntity<UserResponse> updateProfilePhone(
            @Valid @RequestBody com.civicresolve.dto.UpdatePhoneRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.civicresolve.service.CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new com.civicresolve.exception.UnauthorizedException("Authentication required");
        }
        if (currentUser.getRole() != com.civicresolve.entity.Role.ROLE_CITIZEN) {
            throw new com.civicresolve.exception.ForbiddenException("Only citizens can update their phone via this endpoint");
        }

        User updated = userService.updateCitizenPhone(currentUser.getId(), request.getPhone());
        return ResponseEntity.ok(new UserResponse(updated));
    }
}

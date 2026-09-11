package com.civicresolve.controller;

import com.civicresolve.dto.ForgotPasswordRequest;
import com.civicresolve.dto.RegisterRequest;
import com.civicresolve.service.UserService;
import jakarta.validation.Valid;
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
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            RegisterRequest req = new RegisterRequest();
            req.setRole("CITIZEN");
            model.addAttribute("registerRequest", req);
        }
        return "auth/register";
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
        } catch (IllegalArgumentException ex) {
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
    public String forgotPasswordSubmit(@Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
                                       BindingResult bindingResult,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMsg", "Please enter a valid email address.");
            return "auth/forgot-password";
        }

        String token = userService.createPasswordResetToken(request.getEmail());
        if (token != null) {
            model.addAttribute("resetToken", token);
            model.addAttribute("successMsg", "A secure password reset link has been generated for " + request.getEmail() + ".");
        } else {
            model.addAttribute("infoMsg", "If this email address is registered, a password reset link has been prepared.");
        }

        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank() || !userService.validatePasswordResetToken(token)) {
            model.addAttribute("errorMsg", "This password reset token is invalid or has expired. Please request a new one.");
            return "auth/reset-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam String token,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        if (password == null || password.length() < 8) {
            model.addAttribute("errorMsg", "Password must be at least 8 characters long.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        boolean success = userService.resetPassword(token, password);
        if (success) {
            redirectAttributes.addFlashAttribute("successMsg", "Your password has been reset successfully! Please sign in with your new credentials.");
            return "redirect:/login?resetSuccess=true";
        } else {
            model.addAttribute("errorMsg", "Unable to reset password. The link may have expired.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
    }
}

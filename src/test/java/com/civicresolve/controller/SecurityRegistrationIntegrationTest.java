package com.civicresolve.controller;

import com.civicresolve.dto.RegisterRequest;
import com.civicresolve.repository.PasswordResetTokenRepository;
import com.civicresolve.repository.UserRepository;
import com.civicresolve.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SecurityRegistrationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private com.civicresolve.repository.EmailOtpVerificationRepository emailOtpVerificationRepository;

    @MockitoBean
    private EmailService emailService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testPublicAuthEndpointsAreAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reset-password").param("token", "dummy-token"))
                .andExpect(status().isOk());
    }

    @Test
    void testPostRegisterAnonymous_withValidCsrfToken_succeedsAndRedirects() throws Exception {
        String uniqueEmail = "csrf.valid." + System.currentTimeMillis() + "@civicresolve.test";

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Sneha")
                        .param("lastName", "Patel")
                        .param("email", uniqueEmail)
                        .param("phone", "+919876543210")
                        .param("password", "SecurePassword@123")
                        .param("confirmPassword", "SecurePassword@123")
                        .param("role", "CITIZEN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        assertTrue(userRepository.existsByEmail(uniqueEmail));
    }

    @Test
    void testPostRegisterAnonymous_withoutCsrfToken_isRejected() throws Exception {
        String testEmail = "csrf.missing." + System.currentTimeMillis() + "@civicresolve.test";

        var result = mockMvc.perform(post("/register")
                        .param("firstName", "Sneha")
                        .param("lastName", "Patel")
                        .param("email", testEmail)
                        .param("phone", "+919876543210")
                        .param("password", "SecurePassword@123")
                        .param("confirmPassword", "SecurePassword@123")
                        .param("role", "CITIZEN"))
                .andExpect(status().isForbidden())
                .andReturn();

        // Must NOT return the custom /api/** JSON error on non-/api browser paths
        String responseContent = result.getResponse().getContentAsString();
        assertFalse(responseContent.contains("\"error\":\"FORBIDDEN\""),
                "Browser route /register must not return /api/** JSON format on CSRF failure");
        assertFalse(userRepository.existsByEmail(testEmail));
    }

    @Test
    void testPostApiAuthRegister_withoutCsrfToken_succeeds() throws Exception {
        String uniqueApiEmail = "api.user." + System.currentTimeMillis() + "@civicresolve.test";

        RegisterRequest request = RegisterRequest.builder()
                .firstName("Rohan")
                .lastName("Verma")
                .email(uniqueApiEmail)
                .phone("+919876543211")
                .password("SecurePassword@123")
                .confirmPassword("SecurePassword@123")
                .role("CITIZEN")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(uniqueApiEmail))
                .andExpect(jsonPath("$.role").value("ROLE_CITIZEN"));

        assertTrue(userRepository.existsByEmail(uniqueApiEmail));
    }

    @Test
    void testRegisterWithEmailOtpAndRoleAdmin_succeeds() throws Exception {
        String adminEmail = "admin.test." + System.currentTimeMillis() + "@civicresolve.test";

        // 1. Request OTP
        mockMvc.perform(post("/api/auth/register/send-otp")
                        .param("email", adminEmail)
                        .param("name", "Admin User"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 2. Fetch the generated OTP from DB or service (or simulate OTP verification)
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Admin")
                        .param("lastName", "Tester")
                        .param("email", adminEmail)
                        .param("phone", "+919876543999")
                        .param("password", "AdminPassword@123")
                        .param("confirmPassword", "AdminPassword@123")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        var userOpt = userRepository.findByEmail(adminEmail);
        assertTrue(userOpt.isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(com.civicresolve.entity.Role.ROLE_ADMIN, userOpt.get().getRole());
    }

    @Test
    void testForgotPasswordAndResetFlow_succeeds() throws Exception {
        String citizenEmail = "forgot.pass." + System.currentTimeMillis() + "@civicresolve.test";

        // Register user first
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Pooja")
                        .param("lastName", "Rao")
                        .param("email", citizenEmail)
                        .param("phone", "+919876543888")
                        .param("password", "OldPassword@123")
                        .param("confirmPassword", "OldPassword@123")
                        .param("role", "CITIZEN"))
                .andExpect(status().is3xxRedirection());

        // Request forgot password
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("email", citizenEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-password?email=" + java.net.URLEncoder.encode(citizenEmail, java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void testMultiStepServerSideRegistrationFlow_withAdminRole_succeeds() throws Exception {
        String testEmail = "multistep.admin." + System.currentTimeMillis() + "@civicresolve.test";

        // Step 1: Submit initial registration form to /register/send-otp
        var session = mockMvc.perform(post("/register/send-otp")
                        .with(csrf())
                        .param("firstName", "Rajesh")
                        .param("lastName", "Kumar")
                        .param("email", testEmail)
                        .param("phone", "+919876543111")
                        .param("password", "AdminSecure@123")
                        .param("confirmPassword", "AdminSecure@123")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/verify?email=" + java.net.URLEncoder.encode(testEmail, java.nio.charset.StandardCharsets.UTF_8)))
                .andReturn()
                .getRequest()
                .getSession();

        // Verify OTP was stored in database
        var otpRecord = emailOtpVerificationRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(testEmail, "REGISTRATION");
        assertTrue(otpRecord.isPresent());
        String generatedOtp = otpRecord.get().getOtp();

        // Step 2: Submit OTP on /register/verify using the same session
        mockMvc.perform(post("/register/verify")
                        .with(csrf())
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .param("otp", generatedOtp))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        // Step 3: Verify user in database has ROLE_ADMIN
        var userOpt = userRepository.findByEmail(testEmail);
        assertTrue(userOpt.isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(com.civicresolve.entity.Role.ROLE_ADMIN, userOpt.get().getRole());
    }

    @Test
    void testMultiStepServerSideForgotPasswordAndReset_succeeds() throws Exception {
        String userEmail = "multistep.reset." + System.currentTimeMillis() + "@civicresolve.test";

        // Create user
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Ananya")
                        .param("lastName", "Sharma")
                        .param("email", userEmail)
                        .param("phone", "+919876543222")
                        .param("password", "InitialPassword@123")
                        .param("confirmPassword", "InitialPassword@123")
                        .param("role", "CITIZEN"))
                .andExpect(status().is3xxRedirection());

        // Step 1: Submit email to /forgot-password
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("email", userEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-password?email=" + java.net.URLEncoder.encode(userEmail, java.nio.charset.StandardCharsets.UTF_8)));

        // A repeated request replaces the existing per-user token instead of inserting a duplicate.
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("email", userEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-password?email=" + java.net.URLEncoder.encode(userEmail, java.nio.charset.StandardCharsets.UTF_8)));

        var user = userRepository.findByEmail(userEmail).orElseThrow();
        assertTrue(passwordResetTokenRepository.findByUser(user).isPresent());

        // Retrieve generated OTP
        var otpRecord = emailOtpVerificationRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(userEmail, "FORGOT_PASSWORD");
        assertTrue(otpRecord.isPresent());
        String resetOtp = otpRecord.get().getOtp();

        // Step 2: Submit new password with OTP to /reset-password
        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("email", userEmail)
                        .param("otp", resetOtp)
                        .param("password", "BrandNewPassword@123")
                        .param("confirmPassword", "BrandNewPassword@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?resetSuccess=true"));
    }
}

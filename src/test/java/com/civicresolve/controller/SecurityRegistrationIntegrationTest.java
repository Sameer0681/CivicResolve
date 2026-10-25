package com.civicresolve.controller;

import com.civicresolve.dto.RegisterRequest;
import com.civicresolve.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
}

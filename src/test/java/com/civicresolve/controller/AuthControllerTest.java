package com.civicresolve.controller;

import com.civicresolve.dto.OfficerCreateRequest;
import com.civicresolve.dto.RegisterRequest;
import com.civicresolve.dto.UpdatePhoneRequest;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.service.CustomUserDetails;
import com.civicresolve.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;
    private User citizen;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        citizen = User.builder()
                .id(1L)
                .firstName("Aarav")
                .lastName("Sharma")
                .email("citizen@civicresolve.in")
                .role(Role.ROLE_CITIZEN)
                .phone("+919876543210")
                .build();

        userDetails = new CustomUserDetails(citizen);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return userDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    void testRegisterRoleIgnoredAndAlwaysForcesCitizen() throws Exception {
        // Point 2: Sending role ROLE_ADMIN in /api/auth/register request body must still create a citizen
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Sneha")
                .lastName("Kapoor")
                .email("sneha.admin.attempt@example.in")
                .password("Password@123")
                .confirmPassword("Password@123")
                .role("ROLE_ADMIN") // malicious or rogue payload trying to gain admin privileges
                .phone("+919876543222")
                .build();

        User registeredCitizen = User.builder()
                .id(15L)
                .firstName("Sneha")
                .lastName("Kapoor")
                .email("sneha.admin.attempt@example.in")
                .role(Role.ROLE_CITIZEN)
                .phone("+919876543222")
                .build();

        when(userService.registerNewUser(any(RegisterRequest.class))).thenReturn(registeredCitizen);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_CITIZEN"));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(userService).registerNewUser(captor.capture());
        assertEquals("CITIZEN", captor.getValue().getRole());
    }

    @Test
    void testAdminCreateOfficer() throws Exception {
        OfficerCreateRequest request = OfficerCreateRequest.builder()
                .name("Kavita Rao")
                .email("kavita.rao@civicresolve.gov")
                .password("Officer@123")
                .department("Roads & Transport")
                .phone("+919415025688")
                .build();

        User officer = User.builder()
                .id(16L)
                .firstName("Kavita")
                .lastName("Rao")
                .email("kavita.rao@civicresolve.gov")
                .role(Role.ROLE_OFFICER)
                .department("Roads & Transport")
                .badgeNumber("OFF-PWD-999")
                .build();

        when(userService.createOfficer(any(OfficerCreateRequest.class))).thenReturn(officer);

        mockMvc.perform(post("/api/admin/users/officer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_OFFICER"))
                .andExpect(jsonPath("$.department").value("Roads & Transport"));
    }

    @Test
    void testUpdateCitizenProfilePhone() throws Exception {
        // Point 4c: PUT /api/auth/profile/phone updates citizen phone without sending OTP
        User updated = User.builder()
                .id(1L)
                .firstName("Aarav")
                .lastName("Sharma")
                .email("citizen@civicresolve.in")
                .role(Role.ROLE_CITIZEN)
                .phone("+919876543299")
                .build();

        when(userService.updateCitizenPhone(eq(1L), eq("+91 98765 43299"))).thenReturn(updated);

        UpdatePhoneRequest request = new UpdatePhoneRequest("+91 98765 43299");

        mockMvc.perform(put("/api/auth/profile/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+919876543299"));

        verify(userService).updateCitizenPhone(eq(1L), eq("+91 98765 43299"));
    }
}

package com.civicresolve.controller;

import com.civicresolve.dto.OtpSendRequest;
import com.civicresolve.dto.OtpSendResponse;
import com.civicresolve.dto.OtpVerifyRequest;
import com.civicresolve.dto.OtpVerifyResponse;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.service.CustomUserDetails;
import com.civicresolve.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OtpControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private OtpController otpController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(otpController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    void testSendOtp() throws Exception {
        OtpSendResponse response = OtpSendResponse.builder()
                .message("Verification code dispatched successfully")
                .channel("SMS")
                .maskedPhone("+91XXXXXX3210")
                .expiresInSeconds(300)
                .cooldownSeconds(60)
                .build();

        when(otpService.sendOtp(eq(citizen), any())).thenReturn(response);

        OtpSendRequest request = OtpSendRequest.builder().channel("SMS").build();

        mockMvc.perform(post("/api/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("SMS"))
                .andExpect(jsonPath("$.maskedPhone").value("+91XXXXXX3210"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300));
    }

    @Test
    void testVerifyOtp() throws Exception {
        OtpVerifyResponse response = OtpVerifyResponse.builder()
                .verified(true)
                .message("Phone successfully verified")
                .otpToken("abc-123-xyz")
                .tokenExpiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(otpService.verifyOtp(eq(citizen), eq("123456"))).thenReturn(response);

        OtpVerifyRequest request = OtpVerifyRequest.builder().otp("123456").build();

        mockMvc.perform(post("/api/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.otpToken").value("abc-123-xyz"));
    }
}

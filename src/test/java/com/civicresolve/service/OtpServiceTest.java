package com.civicresolve.service;

import com.civicresolve.dto.OtpSendRequest;
import com.civicresolve.dto.OtpSendResponse;
import com.civicresolve.dto.OtpVerifyResponse;
import com.civicresolve.entity.OtpVerification;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.exception.ConflictException;
import com.civicresolve.exception.RateLimitException;
import com.civicresolve.exception.UnauthorizedException;
import com.civicresolve.repository.OtpVerificationRepository;
import com.civicresolve.service.otp.OtpSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private OtpSender otpSender;

    private OtpServiceImpl otpService;

    private User citizen;

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl(otpVerificationRepository, otpSender, true);

        citizen = User.builder()
                .id(1L)
                .firstName("Aarav")
                .lastName("Sharma")
                .email("citizen@civicresolve.in")
                .phone("+919876543210")
                .role(Role.ROLE_CITIZEN)
                .build();
    }

    @Test
    void testSendOtpSmsHappyPath() {
        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(otpVerificationRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(0L);

        OtpSendRequest request = OtpSendRequest.builder().channel("SMS").build();
        OtpSendResponse response = otpService.sendOtp(citizen, request);

        assertNotNull(response);
        assertEquals("SMS", response.getChannel());
        assertEquals("+91XXXXXX3210", response.getMaskedPhone());
        assertEquals(300, response.getExpiresInSeconds());

        verify(otpSender, times(1)).sendSms(eq("+919876543210"), any(String.class));
        verify(otpSender, never()).sendVoiceCall(any(), any());
        verify(otpVerificationRepository, times(1)).save(any(OtpVerification.class));
    }

    @Test
    void testSendOtpAlwaysUsesCitizenRegisteredPhone() {
        // Point 1: Even if citizen object is manipulated or body passed, strictly citizen.getPhone() is used
        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(otpVerificationRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(0L);

        OtpSendRequest request = new OtpSendRequest("SMS");
        otpService.sendOtp(citizen, request);

        // Verification must be sent strictly to citizen's registered phone
        verify(otpSender).sendSms(eq("+919876543210"), any(String.class));
    }

    @Test
    void testSendOtpWithoutRegisteredPhoneThrowsBadRequest() {
        User unphonabledCitizen = User.builder()
                .id(2L)
                .email("nophone@civicresolve.in")
                .phone(null)
                .role(Role.ROLE_CITIZEN)
                .build();

        OtpSendRequest request = OtpSendRequest.builder().channel("SMS").build();
        BadRequestException ex = assertThrows(BadRequestException.class, () -> otpService.sendOtp(unphonabledCitizen, request));
        assertTrue(ex.getMessage().contains("No mobile phone number registered"));
        verify(otpSender, never()).sendSms(any(), any());
    }

    @Test
    void testSendOtpVoiceCallHappyPath() {
        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(otpVerificationRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
        when(otpVerificationRepository.countVoiceCallsAfter(eq(1L), any(LocalDateTime.class))).thenReturn(0L);

        OtpSendRequest request = OtpSendRequest.builder().channel("CALL").build();
        OtpSendResponse response = otpService.sendOtp(citizen, request);

        assertNotNull(response);
        assertEquals("CALL", response.getChannel());

        verify(otpSender, times(1)).sendVoiceCall(eq("+919876543210"), any(String.class));
        verify(otpSender, never()).sendSms(any(), any());
    }

    @Test
    void testSendOtpCooldownEnforced() {
        OtpVerification recent = OtpVerification.builder()
                .userId(1L)
                .createdAt(LocalDateTime.now().minusSeconds(30))
                .build();

        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(recent));

        OtpSendRequest request = OtpSendRequest.builder().channel("SMS").build();
        assertThrows(RateLimitException.class, () -> otpService.sendOtp(citizen, request));
        verify(otpSender, never()).sendSms(any(), any());
    }

    @Test
    void testSendOtpHourlyLimitEnforced() {
        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(otpVerificationRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(5L);

        OtpSendRequest request = OtpSendRequest.builder().channel("SMS").build();
        assertThrows(RateLimitException.class, () -> otpService.sendOtp(citizen, request));
    }

    @Test
    void testSendOtpVoiceCallHourlyLimitEnforced() {
        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(otpVerificationRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(2L);
        when(otpVerificationRepository.countVoiceCallsAfter(eq(1L), any(LocalDateTime.class))).thenReturn(3L);

        OtpSendRequest request = OtpSendRequest.builder().channel("CALL").build();
        assertThrows(RateLimitException.class, () -> otpService.sendOtp(citizen, request));
    }

    @Test
    void testVerifyOtpWrongCodeDecrementsAttempts() {
        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        // Send OTP to get real stored hash
        otpService.sendOtp(citizen, OtpSendRequest.builder().channel("SMS").build());
        verify(otpVerificationRepository).save(captor.capture());
        OtpVerification stored = captor.getValue();

        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(stored));

        assertThrows(BadRequestException.class, () -> otpService.verifyOtp(citizen, "000000"));
        assertEquals(1, stored.getAttempts());
        assertFalse(stored.isVerified());
    }

    @Test
    void testVerifyOtpMaxAttemptsBlockedAtFive() {
        // Point 7: Max wrong attempts 5
        OtpVerification maxedOut = OtpVerification.builder()
                .userId(1L)
                .otpHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .attempts(5)
                .verified(false)
                .used(false)
                .build();

        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(maxedOut));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> otpService.verifyOtp(citizen, "123456"));
        assertTrue(ex.getMessage().contains("Maximum verification attempts exceeded"));
    }

    @Test
    void testVerifyOtpExpiredBlocked() {
        OtpVerification expired = OtpVerification.builder()
                .userId(1L)
                .otpHash("hash")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .attempts(0)
                .verified(false)
                .used(false)
                .build();

        when(otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(expired));

        assertThrows(BadRequestException.class, () -> otpService.verifyOtp(citizen, "123456"));
    }

    @Test
    void testValidateAndConsumeTokenReusedBlocked() {
        OtpVerification usedRecord = OtpVerification.builder()
                .userId(1L)
                .verified(true)
                .used(true)
                .tokenExpiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(otpVerificationRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(usedRecord));

        assertThrows(ConflictException.class, () -> otpService.validateAndConsumeToken("some-uuid-token", 1L));
    }

    @Test
    void testValidateAndConsumeTokenCrossUserBlocked() {
        OtpVerification otherUserRecord = OtpVerification.builder()
                .userId(99L)
                .verified(true)
                .used(false)
                .tokenExpiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(otpVerificationRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(otherUserRecord));

        assertThrows(UnauthorizedException.class, () -> otpService.validateAndConsumeToken("some-uuid-token", 1L));
    }

    @Test
    void testPhoneNormalization() {
        assertEquals("+919876543210", OtpServiceImpl.normalizeIndianPhone("9876543210"));
        assertEquals("+919876543210", OtpServiceImpl.normalizeIndianPhone("+919876543210"));
        assertEquals("+919876543210", OtpServiceImpl.normalizeIndianPhone("919876543210"));
        assertEquals("+919876543210", OtpServiceImpl.normalizeIndianPhone("+91 98765 43210"));
        assertEquals("+919876543210", OtpServiceImpl.normalizeIndianPhone("98765-43210"));
        assertEquals("+919876543210", OtpServiceImpl.normalizeIndianPhone("09876543210"));

        assertThrows(BadRequestException.class, () -> OtpServiceImpl.normalizeIndianPhone("12345"));
        assertThrows(BadRequestException.class, () -> OtpServiceImpl.normalizeIndianPhone("1234567890")); // starts with 1
    }
}

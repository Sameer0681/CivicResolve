package com.civicresolve.controller;

import com.civicresolve.dto.OtpSendRequest;
import com.civicresolve.dto.OtpSendResponse;
import com.civicresolve.dto.OtpVerifyRequest;
import com.civicresolve.dto.OtpVerifyResponse;
import com.civicresolve.entity.Role;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.exception.UnauthorizedException;
import com.civicresolve.service.CustomUserDetails;
import com.civicresolve.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send")
    public ResponseEntity<OtpSendResponse> sendOtp(
            @RequestBody(required = false) OtpSendRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Citizen authentication required to request OTP");
        }

        if (currentUser.getRole() != Role.ROLE_CITIZEN) {
            throw new BadRequestException("OTP phone verification is intended for Citizens");
        }

        OtpSendRequest safeRequest = request != null ? request : new OtpSendRequest();
        OtpSendResponse response = otpService.sendOtp(currentUser.getUser(), safeRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Citizen authentication required to verify OTP");
        }

        if (currentUser.getRole() != Role.ROLE_CITIZEN) {
            throw new BadRequestException("OTP phone verification is intended for Citizens");
        }

        OtpVerifyResponse response = otpService.verifyOtp(currentUser.getUser(), request.getOtp());
        return ResponseEntity.ok(response);
    }
}

package com.civicresolve.service;

import com.civicresolve.dto.OtpSendRequest;
import com.civicresolve.dto.OtpSendResponse;
import com.civicresolve.dto.OtpVerifyResponse;
import com.civicresolve.entity.OtpVerification;
import com.civicresolve.entity.User;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.exception.ConflictException;
import com.civicresolve.exception.OtpException;
import com.civicresolve.exception.OtpRequiredException;
import com.civicresolve.exception.RateLimitException;
import com.civicresolve.exception.UnauthorizedException;
import com.civicresolve.repository.OtpVerificationRepository;
import com.civicresolve.service.otp.MockOtpSender;
import com.civicresolve.service.otp.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final OtpVerificationRepository otpVerificationRepository;
    private final OtpSender otpSender;
    private final boolean otpRequired;
    private final String defaultChannel;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpServiceImpl(
            OtpVerificationRepository otpVerificationRepository,
            OtpSender otpSender,
            @Value("${app.otp.required:false}") boolean otpRequired) {
        this(otpVerificationRepository, otpSender, otpRequired, "BOTH");
    }

    @Autowired
    public OtpServiceImpl(
            OtpVerificationRepository otpVerificationRepository,
            OtpSender otpSender,
            @Value("${app.otp.required:false}") boolean otpRequired,
            @Value("${app.otp.default-channel:BOTH}") String defaultChannel) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.otpSender = otpSender;
        this.otpRequired = otpRequired;
        this.defaultChannel = normalizeChannel(defaultChannel);
    }

    private static String normalizeChannel(String ch) {
        if (ch == null || ch.isBlank()) {
            return "BOTH";
        }
        String upper = ch.trim().toUpperCase();
        return (upper.equals("SMS") || upper.equals("CALL") || upper.equals("BOTH")) ? upper : "BOTH";
    }

    @Override
    public boolean isOtpRequired() {
        return this.otpRequired;
    }

    @Override
    public OtpSendResponse sendOtp(User citizen, OtpSendRequest request) {
        if (citizen == null) {
            throw new UnauthorizedException("Citizen authentication required");
        }

        // Point 1: NEVER use request body phone. Strictly use authenticated citizen's registered phone.
        if (citizen.getPhone() == null || citizen.getPhone().isBlank()) {
            throw new BadRequestException("No mobile phone number registered on your citizen account. Please update your profile with a valid 10-digit mobile number.");
        }
        String normalizedPhone = normalizeIndianPhone(citizen.getPhone());
        String maskedPhone = MockOtpSender.maskPhone(normalizedPhone);

        String channel;
        if (request != null && request.getChannel() != null && !request.getChannel().isBlank()) {
            String ch = request.getChannel().trim().toUpperCase();
            if (!ch.equals("SMS") && !ch.equals("CALL") && !ch.equals("BOTH")) {
                throw new BadRequestException("Invalid channel: " + ch + ". Allowed: SMS, CALL, BOTH");
            }
            channel = ch;
        } else {
            channel = this.defaultChannel;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Cooldown check: 60 seconds
        Optional<OtpVerification> latestOpt = otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(citizen.getId());
        if (latestOpt.isPresent()) {
            OtpVerification latest = latestOpt.get();
            if (latest.getCreatedAt().plusSeconds(60).isAfter(now)) {
                long waitSeconds = java.time.Duration.between(now, latest.getCreatedAt().plusSeconds(60)).getSeconds();
                throw new RateLimitException("Please wait " + Math.max(1, waitSeconds) + " seconds before requesting another OTP");
            }
        }

        // 2. Hourly rate limit: max 5 OTPs per hour across channels
        LocalDateTime oneHourAgo = now.minusHours(1);
        long hourlyCount = otpVerificationRepository.countByUserIdAndCreatedAtAfter(citizen.getId(), oneHourAgo);
        if (hourlyCount >= 5) {
            throw new RateLimitException("Hourly OTP request limit exceeded (max 5 per hour). Please try again later.");
        }

        // 3. Channel-specific limit: max 3 voice calls per hour
        if (channel.equals("CALL") || channel.equals("BOTH")) {
            long voiceCallCount = otpVerificationRepository.countVoiceCallsAfter(citizen.getId(), oneHourAgo);
            if (voiceCallCount >= 3) {
                throw new RateLimitException("Voice call OTP limit exceeded (max 3 calls per hour). Please choose SMS.");
            }
        }

        // 4. Generate 6-digit OTP code
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String hashedOtp = hashWithSha256(otp + ":" + citizen.getId());

        // 5. Dispatch via OtpSender
        boolean smsSuccess = false;
        boolean callSuccess = false;
        Exception smsError = null;
        Exception callError = null;

        if (channel.equals("SMS") || channel.equals("BOTH")) {
            try {
                otpSender.sendSms(normalizedPhone, otp);
                smsSuccess = true;
            } catch (Exception e) {
                smsError = e;
                log.warn("SMS OTP dispatch failed for citizen {} (masked: {}): {}", citizen.getId(), maskedPhone, sanitize(e.getMessage(), normalizedPhone, otp));
            }
        }

        if (channel.equals("CALL") || channel.equals("BOTH")) {
            try {
                otpSender.sendVoiceCall(normalizedPhone, otp);
                callSuccess = true;
            } catch (Exception e) {
                callError = e;
                log.warn("Voice Call OTP dispatch failed for citizen {} (masked: {}): {}", citizen.getId(), maskedPhone, sanitize(e.getMessage(), normalizedPhone, otp));
            }
        }

        boolean anySuccess = smsSuccess || callSuccess;
        if (!anySuccess) {
            log.error("All OTP delivery channels failed for citizen {} (masked: {})", citizen.getId(), maskedPhone);
            String failDetail = "";
            if (smsError != null && callError != null) {
                failDetail = " (SMS: " + sanitize(smsError.getMessage(), normalizedPhone, otp) + "; Call: " + sanitize(callError.getMessage(), normalizedPhone, otp) + ")";
            } else if (smsError != null) {
                failDetail = " (SMS: " + sanitize(smsError.getMessage(), normalizedPhone, otp) + ")";
            } else if (callError != null) {
                failDetail = " (Call: " + sanitize(callError.getMessage(), normalizedPhone, otp) + ")";
            }
            throw new OtpException("OTP_DELIVERY_FAILED", "Failed to deliver verification code. Upstream provider error: " + failDetail, HttpStatus.BAD_GATEWAY);
        }

        String deliveredChannel;
        String responseMessage;
        if (smsSuccess && callSuccess) {
            deliveredChannel = "BOTH";
            responseMessage = "Verification code dispatched successfully via SMS and voice call";
        } else if (callSuccess) {
            deliveredChannel = "CALL";
            responseMessage = "Verification code dispatched via voice call (SMS delivery failed)";
        } else {
            deliveredChannel = "SMS";
            responseMessage = "Verification code dispatched via SMS (voice call delivery failed)";
        }

        // 6. Persist OtpVerification record
        OtpVerification verification = OtpVerification.builder()
                .userId(citizen.getId())
                .phone(normalizedPhone)
                .otpHash(hashedOtp)
                .channel(deliveredChannel)
                .expiresAt(now.plusMinutes(5))
                .attempts(0)
                .verified(false)
                .used(false)
                .build();

        otpVerificationRepository.save(verification);

        return OtpSendResponse.builder()
                .message(responseMessage)
                .channel(deliveredChannel)
                .maskedPhone(maskedPhone)
                .expiresInSeconds(300)
                .cooldownSeconds(60)
                .build();
    }

    @Override
    public OtpVerifyResponse verifyOtp(User citizen, String otp) {
        if (citizen == null) {
            throw new UnauthorizedException("Citizen authentication required");
        }
        if (otp == null || !otp.trim().matches("^\\d{6}$")) {
            throw new BadRequestException("OTP must be exactly 6 digits");
        }

        OtpVerification record = otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(citizen.getId())
                .orElseThrow(() -> new BadRequestException("No active OTP request found for this citizen"));

        if (record.isVerified() || record.isUsed()) {
            throw new BadRequestException("This OTP has already been verified or used");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(record.getExpiresAt())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        if (record.getAttempts() >= 5) {
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        record.setAttempts(record.getAttempts() + 1);

        String computedHash = hashWithSha256(otp.trim() + ":" + citizen.getId());
        if (!MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8),
                record.getOtpHash().getBytes(StandardCharsets.UTF_8))) {
            otpVerificationRepository.save(record);
            int remaining = 5 - record.getAttempts();
            throw new BadRequestException("Invalid OTP code. " + remaining + " attempts remaining.");
        }

        // Successfully verified: issue single-use submission token (valid 10 minutes)
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashWithSha256(rawToken);

        record.setVerified(true);
        record.setTokenHash(hashedToken);
        record.setTokenExpiresAt(now.plusMinutes(10));
        otpVerificationRepository.save(record);

        return OtpVerifyResponse.builder()
                .verified(true)
                .message("Phone successfully verified")
                .otpToken(rawToken)
                .tokenExpiresAt(record.getTokenExpiresAt())
                .build();
    }

    @Override
    public void validateAndConsumeToken(String otpToken, Long citizenId) {
        if (!otpRequired && (otpToken == null || otpToken.isBlank())) {
            // Optional in dev/demo when app.otp.required=false
            return;
        }

        if (otpToken == null || otpToken.isBlank()) {
            throw new BadRequestException("OTP verification token is required to submit a complaint");
        }

        String hashedToken = hashWithSha256(otpToken.trim());
        OtpVerification record = otpVerificationRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new BadRequestException("Invalid or unrecognized OTP verification token"));

        if (!record.isVerified()) {
            throw new BadRequestException("OTP token has not been verified");
        }

        if (record.isUsed()) {
            throw new ConflictException("OTP token has already been used to submit a complaint");
        }

        if (LocalDateTime.now().isAfter(record.getTokenExpiresAt())) {
            throw new BadRequestException("OTP token has expired. Please verify your phone again.");
        }

        if (!record.getUserId().equals(citizenId)) {
            throw new UnauthorizedException("OTP token does not belong to the submitting citizen");
        }

        record.setUsed(true);
        otpVerificationRepository.save(record);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = OtpRequiredException.class)
    public void triggerComplaintSubmissionOtp(User citizen) {
        if (citizen == null) {
            throw new UnauthorizedException("Citizen authentication required to submit complaint");
        }

        // Point 4: If citizen has no valid phone, return 400 asking to add phone via PUT /api/auth/profile/phone, send nothing
        if (citizen.getPhone() == null || citizen.getPhone().trim().isEmpty()) {
            throw new BadRequestException("No mobile phone number registered on your account. Please add a valid phone number via PUT /api/auth/profile/phone before submitting complaints.");
        }

        String normalizedPhone;
        try {
            normalizedPhone = normalizeIndianPhone(citizen.getPhone());
        } catch (BadRequestException e) {
            throw new BadRequestException("Invalid mobile phone number on your account. Please update your phone number via PUT /api/auth/profile/phone before submitting complaints.");
        }

        LocalDateTime now = LocalDateTime.now();
        String maskedPhone = MockOtpSender.maskPhone(normalizedPhone);

        // Channel configured via app.otp.default-channel (default BOTH, supports CALL via APP_OTP_DEFAULT_CHANNEL=CALL)
        String channel = this.defaultChannel;

        // Point 2: If a valid unexpired OTP was already sent within cooldown, do NOT send another; just report it.
        Optional<OtpVerification> latestOpt = otpVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(citizen.getId());
        if (latestOpt.isPresent()) {
            OtpVerification latest = latestOpt.get();
            boolean isUnexpired = latest.getExpiresAt().isAfter(now);
            boolean isWithinCooldown = latest.getCreatedAt().plusSeconds(60).isAfter(now);
            boolean isUnused = !latest.isUsed();

            if (isUnexpired && isWithinCooldown && isUnused) {
                long resendAfterSeconds = Math.max(1, java.time.Duration.between(now, latest.getCreatedAt().plusSeconds(60)).getSeconds());
                long expiresInSeconds = Math.max(0, java.time.Duration.between(now, latest.getExpiresAt()).getSeconds());

                String recentChannel = latest.getChannel() != null ? latest.getChannel() : "BOTH";
                String cooldownMessage;
                if ("CALL".equalsIgnoreCase(recentChannel)) {
                    cooldownMessage = "OTP verification is required to submit a complaint. A verification code was recently dispatched to your registered phone via voice call.";
                } else if ("SMS".equalsIgnoreCase(recentChannel)) {
                    cooldownMessage = "OTP verification is required to submit a complaint. A verification code was recently dispatched to your registered phone via SMS.";
                } else {
                    cooldownMessage = "OTP verification is required to submit a complaint. A verification code was recently dispatched to your registered phone via SMS and voice call.";
                }

                throw new OtpRequiredException(
                        cooldownMessage,
                        maskedPhone,
                        expiresInSeconds,
                        resendAfterSeconds,
                        recentChannel
                );
            }
        }

        // Rate limits: hourly count and voice calls
        LocalDateTime oneHourAgo = now.minusHours(1);
        long hourlyCount = otpVerificationRepository.countByUserIdAndCreatedAtAfter(citizen.getId(), oneHourAgo);
        if (hourlyCount >= 5) {
            throw new RateLimitException("Hourly OTP request limit exceeded (max 5 per hour). Please try again later.");
        }

        if (channel.equals("CALL") || channel.equals("BOTH")) {
            long voiceCallCount = otpVerificationRepository.countVoiceCallsAfter(citizen.getId(), oneHourAgo);
            if (voiceCallCount >= 3) {
                throw new RateLimitException("Voice call OTP limit exceeded (max 3 calls per hour). Please try again later.");
            }
        }

        // Generate 6-digit OTP code
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String hashedOtp = hashWithSha256(otp + ":" + citizen.getId());

        // Dispatch via OtpSender
        boolean smsSuccess = false;
        boolean callSuccess = false;
        Exception smsError = null;
        Exception callError = null;

        if (channel.equals("SMS") || channel.equals("BOTH")) {
            try {
                otpSender.sendSms(normalizedPhone, otp);
                smsSuccess = true;
            } catch (Exception e) {
                smsError = e;
                log.warn("SMS OTP dispatch failed for citizen {} (masked: {}): {}", citizen.getId(), maskedPhone, sanitize(e.getMessage(), normalizedPhone, otp));
            }
        }

        if (channel.equals("CALL") || channel.equals("BOTH")) {
            try {
                otpSender.sendVoiceCall(normalizedPhone, otp);
                callSuccess = true;
            } catch (Exception e) {
                callError = e;
                log.warn("Voice Call OTP dispatch failed for citizen {} (masked: {}): {}", citizen.getId(), maskedPhone, sanitize(e.getMessage(), normalizedPhone, otp));
            }
        }

        boolean anySuccess = smsSuccess || callSuccess;
        if (!anySuccess) {
            log.error("All OTP delivery channels failed for citizen {} (masked: {})", citizen.getId(), maskedPhone);
            String failDetail = "";
            if (smsError != null && callError != null) {
                failDetail = " (SMS: " + sanitize(smsError.getMessage(), normalizedPhone, otp) + "; Call: " + sanitize(callError.getMessage(), normalizedPhone, otp) + ")";
            } else if (smsError != null) {
                failDetail = " (SMS: " + sanitize(smsError.getMessage(), normalizedPhone, otp) + ")";
            } else if (callError != null) {
                failDetail = " (Call: " + sanitize(callError.getMessage(), normalizedPhone, otp) + ")";
            }
            throw new OtpException("OTP_DELIVERY_FAILED", "Failed to deliver verification code. Upstream provider error: " + failDetail, HttpStatus.BAD_GATEWAY);
        }

        // Determine delivered channel
        String deliveredChannel;
        if (smsSuccess && callSuccess) {
            deliveredChannel = "BOTH";
        } else if (callSuccess) {
            deliveredChannel = "CALL";
        } else {
            deliveredChannel = "SMS";
        }

        // Persist OtpVerification record
        OtpVerification verification = OtpVerification.builder()
                .userId(citizen.getId())
                .phone(normalizedPhone)
                .otpHash(hashedOtp)
                .channel(deliveredChannel)
                .expiresAt(now.plusMinutes(5))
                .attempts(0)
                .verified(false)
                .used(false)
                .build();
        otpVerificationRepository.save(verification);

        // 428 response message: MUST NOT claim SMS was sent if it failed!
        String responseMessage;
        if (deliveredChannel.equals("CALL")) {
            responseMessage = "OTP verification is required to submit a complaint. A verification code has been dispatched to your registered phone via voice call.";
        } else if (deliveredChannel.equals("SMS")) {
            responseMessage = "OTP verification is required to submit a complaint. A verification code has been dispatched to your registered phone via SMS.";
        } else {
            responseMessage = "OTP verification is required to submit a complaint. A verification code has been dispatched to your registered phone via SMS and voice call.";
        }

        throw new OtpRequiredException(
                responseMessage,
                maskedPhone,
                300L,
                60L,
                deliveredChannel
        );
    }

    public static String sanitize(String message, String phone, String otp) {
        if (message == null) {
            return "";
        }
        String clean = message;
        if (phone != null && !phone.isBlank()) {
            clean = clean.replace(phone, MockOtpSender.maskPhone(phone));
        }
        if (otp != null && !otp.isBlank()) {
            clean = clean.replace(otp, "[REDACTED]");
        }
        return clean;
    }

    public static String normalizeIndianPhone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Valid Indian mobile number is required");
        }
        // Remove whitespace, hyphens, brackets, dots
        String cleaned = raw.replaceAll("[\\s\\-\\(\\)\\.]", "");

        // Handle optional leading zero (e.g. "09876543210")
        if (cleaned.startsWith("0") && cleaned.length() == 11) {
            cleaned = cleaned.substring(1);
        }

        if (cleaned.startsWith("+91")) {
            String num = cleaned.substring(3);
            if (num.matches("^[6-9]\\d{9}$")) {
                return "+91" + num;
            }
        } else if (cleaned.startsWith("91") && cleaned.length() == 12) {
            String num = cleaned.substring(2);
            if (num.matches("^[6-9]\\d{9}$")) {
                return "+91" + num;
            }
        } else if (cleaned.matches("^[6-9]\\d{9}$")) {
            return "+91" + cleaned;
        }

        throw new BadRequestException("Invalid Indian mobile number: " + raw + ". Must be a 10-digit number starting with 6-9, optionally prefixed with +91 or 0.");
    }

    private String hashWithSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

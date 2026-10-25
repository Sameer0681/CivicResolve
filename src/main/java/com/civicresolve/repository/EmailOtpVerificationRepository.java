package com.civicresolve.repository;

import com.civicresolve.entity.EmailOtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailOtpVerificationRepository extends JpaRepository<EmailOtpVerification, Long> {

    Optional<EmailOtpVerification> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email, String purpose);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);
}

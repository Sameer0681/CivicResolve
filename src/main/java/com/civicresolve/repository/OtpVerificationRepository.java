package com.civicresolve.repository;

import com.civicresolve.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.userId = :userId AND o.createdAt > :after AND (o.channel = 'CALL' OR o.channel = 'BOTH')")
    long countVoiceCallsAfter(@Param("userId") Long userId, @Param("after") LocalDateTime after);

    Optional<OtpVerification> findByTokenHash(String tokenHash);
}

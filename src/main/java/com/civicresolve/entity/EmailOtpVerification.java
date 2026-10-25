package com.civicresolve.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_otp_verifications", indexes = {
    @Index(name = "idx_email_purpose", columnList = "email, purpose"),
    @Index(name = "idx_email_created", columnList = "email, createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 10)
    private String otp;

    @Column(nullable = false, length = 30)
    private String purpose; // "REGISTRATION", "FORGOT_PASSWORD"

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}

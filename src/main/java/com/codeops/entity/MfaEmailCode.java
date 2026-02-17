package com.codeops.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores a single-use, time-limited email MFA verification code for a user.
 *
 * <p>Codes are BCrypt-hashed before storage. Each code has a 10-minute TTL
 * and is marked as used after successful verification to prevent replay attacks.
 * Expired codes are periodically cleaned up by a scheduled task.</p>
 *
 * @see com.codeops.service.MfaService
 */
@Entity
@Table(name = "mfa_email_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaEmailCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}

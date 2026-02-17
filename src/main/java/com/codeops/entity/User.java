package com.codeops.entity;

import com.codeops.entity.enums.MfaMethod;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Builder.Default
    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_method", nullable = false, length = 10)
    private MfaMethod mfaMethod = MfaMethod.NONE;

    @Column(name = "mfa_secret", length = 500)
    private String mfaSecret;

    @Column(name = "mfa_recovery_codes", length = 2000)
    private String mfaRecoveryCodes;
}

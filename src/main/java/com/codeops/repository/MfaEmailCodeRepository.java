package com.codeops.repository;

import com.codeops.entity.MfaEmailCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link MfaEmailCode} entities.
 *
 * <p>Provides queries for finding unexpired/unused codes by user and for
 * cleaning up expired codes.</p>
 */
@Repository
public interface MfaEmailCodeRepository extends JpaRepository<MfaEmailCode, UUID> {

    /**
     * Finds all unexpired, unused MFA email codes for a given user.
     *
     * @param userId    the user's ID
     * @param now       the current timestamp for expiration comparison
     * @return list of valid (unexpired, unused) codes for the user
     */
    List<MfaEmailCode> findByUserIdAndUsedFalseAndExpiresAtAfter(UUID userId, Instant now);

    /**
     * Deletes all expired MFA email codes (for scheduled cleanup).
     *
     * @param now the current timestamp; codes with {@code expiresAt} before this are deleted
     */
    void deleteByExpiresAtBefore(Instant now);

    /**
     * Deletes all MFA email codes for a given user (used when disabling email MFA).
     *
     * @param userId the user's ID
     */
    void deleteByUserId(UUID userId);
}

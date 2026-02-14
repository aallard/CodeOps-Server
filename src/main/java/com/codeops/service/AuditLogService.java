package com.codeops.service;

import com.codeops.dto.response.AuditLogResponse;
import com.codeops.entity.AuditLog;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.repository.AuditLogRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Records and retrieves audit log entries for user and team activity tracking.
 *
 * <p>The {@link #log} method is annotated with {@code @Async} and executes in a separate thread
 * to avoid blocking the calling operation. Audit entries capture the acting user, team context,
 * action performed, and the target entity type and ID.</p>
 *
 * @see AuditLogRepository
 * @see AuditLog
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Asynchronously writes an audit log entry to the database.
     *
     * <p>This method runs on a separate thread via {@code @Async} and uses its own transaction.
     * If the provided {@code userId} or {@code teamId} cannot be resolved, the corresponding
     * field is set to {@code null} rather than throwing an exception.</p>
     *
     * @param userId     the UUID of the user performing the action, or {@code null} for system actions
     * @param teamId     the UUID of the team context, or {@code null} if not team-scoped
     * @param action     a short description of the action performed (e.g., "CREATE", "DELETE")
     * @param entityType the type of entity affected (e.g., "Project", "QaJob")
     * @param entityId   the UUID of the affected entity
     * @param details    additional free-text details about the action
     */
    @Async
    @Transactional
    public void log(UUID userId, UUID teamId, String action, String entityType, UUID entityId, String details) {
        AuditLog entry = new AuditLog();
        if (userId != null) {
            entry.setUser(userRepository.findById(userId).orElse(null));
        }
        if (teamId != null) {
            entry.setTeam(teamRepository.findById(teamId).orElse(null));
        }
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        entry.setCreatedAt(Instant.now());
        auditLogRepository.save(entry);
    }

    /**
     * Retrieves a paginated audit log for a specific team, ordered by creation time descending.
     *
     * @param teamId   the UUID of the team whose audit log to retrieve
     * @param pageable the pagination and sorting parameters
     * @return a page of audit log response DTOs
     * @throws AccessDeniedException if the current user is not a member of the specified team
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getTeamAuditLog(UUID teamId, Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        return auditLogRepository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Retrieves a paginated audit log for a specific user, ordered by creation time descending.
     *
     * <p>Users can only access their own audit log; requesting another user's log
     * results in an access denied error.</p>
     *
     * @param userId   the UUID of the user whose audit log to retrieve
     * @param pageable the pagination and sorting parameters
     * @return a page of audit log response DTOs
     * @throws AccessDeniedException if the current user's ID does not match the requested {@code userId}
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getUserAuditLog(UUID userId, Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new AccessDeniedException("Cannot access another user's audit log");
        }
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUser() != null ? log.getUser().getId() : null,
                null,
                log.getTeam() != null ? log.getTeam().getId() : null,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}

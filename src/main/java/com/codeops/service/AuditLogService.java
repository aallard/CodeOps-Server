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

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Async
    @Transactional
    public void log(UUID userId, UUID teamId, String action, String entityType, UUID entityId, String details) {
        AuditLog entry = new AuditLog();
        if (userId != null) {
            entry.setUser(userRepository.getReferenceById(userId));
        }
        if (teamId != null) {
            entry.setTeam(teamRepository.getReferenceById(teamId));
        }
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        entry.setCreatedAt(Instant.now());
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getTeamAuditLog(UUID teamId, Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        return auditLogRepository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable)
                .map(this::mapToResponse);
    }

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

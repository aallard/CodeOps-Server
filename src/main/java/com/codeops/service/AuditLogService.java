package com.codeops.service;

import com.codeops.dto.response.AuditLogResponse;
import com.codeops.entity.AuditLog;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.repository.AuditLogRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
        return auditLogRepository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getUserAuditLog(UUID userId, Pageable pageable) {
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

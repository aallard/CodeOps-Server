package com.codeops.service;

import com.codeops.dto.response.AuditLogResponse;
import com.codeops.entity.AuditLog;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.AuditLogRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private UUID currentUserId;
    private UUID teamId;
    private User testUser;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        teamId = UUID.randomUUID();

        testUser = User.builder()
                .email("test@codeops.dev")
                .passwordHash("hash")
                .displayName("Test User")
                .isActive(true)
                .build();
        testUser.setId(currentUserId);
        testUser.setCreatedAt(Instant.now());

        testTeam = Team.builder()
                .name("Test Team")
                .description("A test team")
                .owner(testUser)
                .build();
        testTeam.setId(teamId);
        testTeam.setCreatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- log() tests ---

    @Test
    void log_withUserIdAndTeamId_savesAuditLog() {
        UUID entityId = UUID.randomUUID();
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(testUser));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log(currentUserId, teamId, "CREATE", "PROJECT", entityId, "Created project");

        verify(userRepository).findById(currentUserId);
        verify(teamRepository).findById(teamId);
        verify(auditLogRepository).save(argThat(entry -> {
            assertEquals(testUser, entry.getUser());
            assertEquals(testTeam, entry.getTeam());
            assertEquals("CREATE", entry.getAction());
            assertEquals("PROJECT", entry.getEntityType());
            assertEquals(entityId, entry.getEntityId());
            assertEquals("Created project", entry.getDetails());
            assertNotNull(entry.getCreatedAt());
            return true;
        }));
    }

    @Test
    void log_withNullUserIdAndNullTeamId_savesWithNulls() {
        UUID entityId = UUID.randomUUID();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log(null, null, "SYSTEM_EVENT", "SYSTEM", entityId, "System event");

        verify(userRepository, never()).findById(any(UUID.class));
        verify(teamRepository, never()).findById(any(UUID.class));
        verify(auditLogRepository).save(argThat(entry -> {
            assertNull(entry.getUser());
            assertNull(entry.getTeam());
            assertEquals("SYSTEM_EVENT", entry.getAction());
            return true;
        }));
    }

    @Test
    void log_withUserNotFound_savesWithNullUser() {
        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log(currentUserId, teamId, "ACTION", "ENTITY", UUID.randomUUID(), "details");

        verify(auditLogRepository).save(argThat(entry -> {
            assertNull(entry.getUser());
            assertEquals(testTeam, entry.getTeam());
            return true;
        }));
    }

    @Test
    void log_withTeamNotFound_savesWithNullTeam() {
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(testUser));
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log(currentUserId, teamId, "ACTION", "ENTITY", UUID.randomUUID(), "details");

        verify(auditLogRepository).save(argThat(entry -> {
            assertEquals(testUser, entry.getUser());
            assertNull(entry.getTeam());
            return true;
        }));
    }

    // --- getTeamAuditLog() tests ---

    @Test
    void getTeamAuditLog_memberAccess_returnsPage() {
        setSecurityContext(currentUserId);
        Pageable pageable = PageRequest.of(0, 20);

        TeamMember membership = TeamMember.builder()
                .team(testTeam)
                .user(testUser)
                .role(TeamRole.MEMBER)
                .joinedAt(Instant.now())
                .build();
        membership.setId(UUID.randomUUID());

        AuditLog log1 = AuditLog.builder()
                .action("CREATE")
                .entityType("PROJECT")
                .entityId(UUID.randomUUID())
                .details("Created project")
                .createdAt(Instant.now())
                .user(testUser)
                .team(testTeam)
                .build();
        log1.setId(1L);

        Page<AuditLog> logPage = new PageImpl<>(List.of(log1), pageable, 1);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(membership));
        when(auditLogRepository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable))
                .thenReturn(logPage);

        Page<AuditLogResponse> result = auditLogService.getTeamAuditLog(teamId, pageable);

        assertEquals(1, result.getTotalElements());
        AuditLogResponse response = result.getContent().get(0);
        assertEquals(1L, response.id());
        assertEquals(currentUserId, response.userId());
        assertNull(response.userName());
        assertEquals(teamId, response.teamId());
        assertEquals("CREATE", response.action());
        assertEquals("PROJECT", response.entityType());
    }

    @Test
    void getTeamAuditLog_notMember_throwsAccessDenied() {
        setSecurityContext(currentUserId);
        Pageable pageable = PageRequest.of(0, 20);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> auditLogService.getTeamAuditLog(teamId, pageable));
        verify(auditLogRepository, never()).findByTeamIdOrderByCreatedAtDesc(any(), any());
    }

    // --- getUserAuditLog() tests ---

    @Test
    void getUserAuditLog_ownLog_returnsPage() {
        setSecurityContext(currentUserId);
        Pageable pageable = PageRequest.of(0, 20);

        AuditLog log1 = AuditLog.builder()
                .action("LOGIN")
                .entityType("USER")
                .entityId(currentUserId)
                .details("User logged in")
                .createdAt(Instant.now())
                .user(testUser)
                .build();
        log1.setId(2L);

        Page<AuditLog> logPage = new PageImpl<>(List.of(log1), pageable, 1);

        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable))
                .thenReturn(logPage);

        Page<AuditLogResponse> result = auditLogService.getUserAuditLog(currentUserId, pageable);

        assertEquals(1, result.getTotalElements());
        AuditLogResponse response = result.getContent().get(0);
        assertEquals(2L, response.id());
        assertEquals("LOGIN", response.action());
    }

    @Test
    void getUserAuditLog_differentUser_throwsAccessDenied() {
        setSecurityContext(currentUserId);
        UUID otherUserId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        assertThrows(AccessDeniedException.class,
                () -> auditLogService.getUserAuditLog(otherUserId, pageable));
        verify(auditLogRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
    }

    // --- mapToResponse edge cases ---

    @Test
    void getTeamAuditLog_logWithNullUser_returnsNullUserId() {
        setSecurityContext(currentUserId);
        Pageable pageable = PageRequest.of(0, 20);

        TeamMember membership = TeamMember.builder()
                .team(testTeam)
                .user(testUser)
                .role(TeamRole.MEMBER)
                .joinedAt(Instant.now())
                .build();
        membership.setId(UUID.randomUUID());

        AuditLog logNoUser = AuditLog.builder()
                .action("SYSTEM")
                .entityType("SYSTEM")
                .createdAt(Instant.now())
                .team(testTeam)
                .user(null)
                .build();
        logNoUser.setId(3L);

        Page<AuditLog> logPage = new PageImpl<>(List.of(logNoUser), pageable, 1);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(membership));
        when(auditLogRepository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable))
                .thenReturn(logPage);

        Page<AuditLogResponse> result = auditLogService.getTeamAuditLog(teamId, pageable);

        AuditLogResponse response = result.getContent().get(0);
        assertNull(response.userId());
        assertEquals(teamId, response.teamId());
    }

    @Test
    void getUserAuditLog_logWithNullTeam_returnsNullTeamId() {
        setSecurityContext(currentUserId);
        Pageable pageable = PageRequest.of(0, 20);

        AuditLog logNoTeam = AuditLog.builder()
                .action("PROFILE_UPDATE")
                .entityType("USER")
                .entityId(currentUserId)
                .createdAt(Instant.now())
                .user(testUser)
                .team(null)
                .build();
        logNoTeam.setId(4L);

        Page<AuditLog> logPage = new PageImpl<>(List.of(logNoTeam), pageable, 1);

        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable))
                .thenReturn(logPage);

        Page<AuditLogResponse> result = auditLogService.getUserAuditLog(currentUserId, pageable);

        AuditLogResponse response = result.getContent().get(0);
        assertEquals(currentUserId, response.userId());
        assertNull(response.teamId());
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

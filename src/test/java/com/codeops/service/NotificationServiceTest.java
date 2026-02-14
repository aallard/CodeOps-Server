package com.codeops.service;

import com.codeops.dto.request.UpdateNotificationPreferenceRequest;
import com.codeops.dto.response.NotificationPreferenceResponse;
import com.codeops.entity.NotificationPreference;
import com.codeops.entity.User;
import com.codeops.repository.NotificationPreferenceRepository;
import com.codeops.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder().email("test@codeops.dev").displayName("Test User").build();
        user.setId(userId);
        user.setCreatedAt(Instant.now());

        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- getPreferences ---

    @Test
    void getPreferences_success() {
        NotificationPreference pref1 = NotificationPreference.builder()
                .user(user).eventType("JOB_COMPLETED").inApp(true).email(false).build();
        pref1.setId(UUID.randomUUID());
        NotificationPreference pref2 = NotificationPreference.builder()
                .user(user).eventType("FINDING_CRITICAL").inApp(true).email(true).build();
        pref2.setId(UUID.randomUUID());

        when(preferenceRepository.findByUserId(userId)).thenReturn(List.of(pref1, pref2));

        List<NotificationPreferenceResponse> responses = notificationService.getPreferences(userId);

        assertEquals(2, responses.size());
        assertEquals("JOB_COMPLETED", responses.get(0).eventType());
        assertTrue(responses.get(0).inApp());
        assertFalse(responses.get(0).email());
        assertEquals("FINDING_CRITICAL", responses.get(1).eventType());
        assertTrue(responses.get(1).email());
    }

    @Test
    void getPreferences_emptyList() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(List.of());

        List<NotificationPreferenceResponse> responses = notificationService.getPreferences(userId);

        assertTrue(responses.isEmpty());
    }

    @Test
    void getPreferences_anotherUser_throws() {
        UUID otherUserId = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> notificationService.getPreferences(otherUserId));
    }

    // --- updatePreference ---

    @Test
    void updatePreference_existingPref_updates() {
        UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest(
                "JOB_COMPLETED", true, true);

        NotificationPreference existing = NotificationPreference.builder()
                .user(user).eventType("JOB_COMPLETED").inApp(true).email(false).build();
        existing.setId(UUID.randomUUID());

        when(preferenceRepository.findByUserIdAndEventType(userId, "JOB_COMPLETED"))
                .thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenReturn(existing);

        NotificationPreferenceResponse response = notificationService.updatePreference(userId, request);

        assertTrue(existing.getEmail());
        assertTrue(existing.getInApp());
        assertEquals("JOB_COMPLETED", response.eventType());
    }

    @Test
    void updatePreference_newPref_creates() {
        UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest(
                "HEALTH_ALERT", false, true);

        when(preferenceRepository.findByUserIdAndEventType(userId, "HEALTH_ALERT"))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(inv -> {
            NotificationPreference p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        NotificationPreferenceResponse response = notificationService.updatePreference(userId, request);

        assertNotNull(response);
        assertEquals("HEALTH_ALERT", response.eventType());
        assertFalse(response.inApp());
        assertTrue(response.email());
    }

    @Test
    void updatePreference_newPref_userNotFound_throws() {
        UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest(
                "HEALTH_ALERT", true, false);

        when(preferenceRepository.findByUserIdAndEventType(userId, "HEALTH_ALERT"))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> notificationService.updatePreference(userId, request));
    }

    @Test
    void updatePreference_anotherUser_throws() {
        UUID otherUserId = UUID.randomUUID();
        UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest(
                "JOB_COMPLETED", true, false);

        assertThrows(AccessDeniedException.class,
                () -> notificationService.updatePreference(otherUserId, request));
    }

    // --- updatePreferences (batch) ---

    @Test
    void updatePreferences_success() {
        UpdateNotificationPreferenceRequest req1 = new UpdateNotificationPreferenceRequest(
                "JOB_COMPLETED", true, true);
        UpdateNotificationPreferenceRequest req2 = new UpdateNotificationPreferenceRequest(
                "FINDING_CRITICAL", true, false);

        NotificationPreference pref1 = NotificationPreference.builder()
                .user(user).eventType("JOB_COMPLETED").inApp(true).email(false).build();
        pref1.setId(UUID.randomUUID());
        NotificationPreference pref2 = NotificationPreference.builder()
                .user(user).eventType("FINDING_CRITICAL").inApp(false).email(false).build();
        pref2.setId(UUID.randomUUID());

        when(preferenceRepository.findByUserIdAndEventType(userId, "JOB_COMPLETED"))
                .thenReturn(Optional.of(pref1));
        when(preferenceRepository.findByUserIdAndEventType(userId, "FINDING_CRITICAL"))
                .thenReturn(Optional.of(pref2));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        List<NotificationPreferenceResponse> responses =
                notificationService.updatePreferences(userId, List.of(req1, req2));

        assertEquals(2, responses.size());
    }

    // --- shouldNotify ---

    @Test
    void shouldNotify_inApp_withPreference_true() {
        NotificationPreference pref = NotificationPreference.builder()
                .user(user).eventType("JOB_COMPLETED").inApp(true).email(false).build();

        when(preferenceRepository.findByUserIdAndEventType(userId, "JOB_COMPLETED"))
                .thenReturn(Optional.of(pref));

        assertTrue(notificationService.shouldNotify(userId, "JOB_COMPLETED", "inApp"));
    }

    @Test
    void shouldNotify_email_withPreference_true() {
        NotificationPreference pref = NotificationPreference.builder()
                .user(user).eventType("JOB_COMPLETED").inApp(false).email(true).build();

        when(preferenceRepository.findByUserIdAndEventType(userId, "JOB_COMPLETED"))
                .thenReturn(Optional.of(pref));

        assertTrue(notificationService.shouldNotify(userId, "JOB_COMPLETED", "email"));
    }

    @Test
    void shouldNotify_email_disabled_false() {
        NotificationPreference pref = NotificationPreference.builder()
                .user(user).eventType("JOB_COMPLETED").inApp(true).email(false).build();

        when(preferenceRepository.findByUserIdAndEventType(userId, "JOB_COMPLETED"))
                .thenReturn(Optional.of(pref));

        assertFalse(notificationService.shouldNotify(userId, "JOB_COMPLETED", "email"));
    }

    @Test
    void shouldNotify_noPreference_inApp_defaultsTrue() {
        when(preferenceRepository.findByUserIdAndEventType(userId, "UNKNOWN_EVENT"))
                .thenReturn(Optional.empty());

        assertTrue(notificationService.shouldNotify(userId, "UNKNOWN_EVENT", "inApp"));
    }

    @Test
    void shouldNotify_noPreference_email_defaultsFalse() {
        when(preferenceRepository.findByUserIdAndEventType(userId, "UNKNOWN_EVENT"))
                .thenReturn(Optional.empty());

        assertFalse(notificationService.shouldNotify(userId, "UNKNOWN_EVENT", "email"));
    }

    @Test
    void shouldNotify_unknownChannel_returnsFalse() {
        NotificationPreference pref = NotificationPreference.builder()
                .user(user).eventType("JOB_COMPLETED").inApp(true).email(true).build();

        when(preferenceRepository.findByUserIdAndEventType(userId, "JOB_COMPLETED"))
                .thenReturn(Optional.of(pref));

        assertFalse(notificationService.shouldNotify(userId, "JOB_COMPLETED", "sms"));
    }

    @Test
    void shouldNotify_noPreference_unknownChannel_returnsFalse() {
        when(preferenceRepository.findByUserIdAndEventType(userId, "JOB_COMPLETED"))
                .thenReturn(Optional.empty());

        assertFalse(notificationService.shouldNotify(userId, "JOB_COMPLETED", "slack"));
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

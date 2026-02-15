package com.codeops.service;

import com.codeops.dto.request.UpdateNotificationPreferenceRequest;
import com.codeops.dto.response.NotificationPreferenceResponse;
import com.codeops.entity.NotificationPreference;
import com.codeops.repository.NotificationPreferenceRepository;
import com.codeops.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeops.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

/**
 * Manages user notification preferences for in-app and email channels.
 *
 * <p>Users can configure per-event-type preferences to control whether
 * notifications are delivered via in-app notifications, email, or both.
 * When no explicit preference exists for an event type, in-app notifications
 * default to enabled and email defaults to disabled.</p>
 *
 * <p>All operations enforce that the current user can only access their own
 * notification preferences.</p>
 *
 * @see NotificationPreference
 * @see UserController
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    private void verifyCurrentUserAccess(UUID userId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new AccessDeniedException("Cannot access another user's notification preferences");
        }
    }

    /**
     * Retrieves all notification preferences for a user.
     *
     * @param userId the ID of the user whose preferences to retrieve
     * @return a list of notification preference response DTOs for all configured event types
     * @throws AccessDeniedException if the current user is not the specified user
     */
    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(UUID userId) {
        log.debug("getPreferences called with userId={}", userId);
        verifyCurrentUserAccess(userId);
        return preferenceRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Creates or updates a notification preference for a specific event type.
     *
     * <p>If a preference already exists for the given user and event type, it is
     * updated in place. Otherwise, a new preference record is created.</p>
     *
     * @param userId the ID of the user whose preference to update
     * @param request the update request containing event type, in-app, and email flags
     * @return the created or updated notification preference as a response DTO
     * @throws AccessDeniedException if the current user is not the specified user
     * @throws EntityNotFoundException if the user is not found (when creating a new preference)
     */
    public NotificationPreferenceResponse updatePreference(UUID userId, UpdateNotificationPreferenceRequest request) {
        log.debug("updatePreference called with userId={}, eventType={}", userId, request.eventType());
        verifyCurrentUserAccess(userId);
        var existing = preferenceRepository.findByUserIdAndEventType(userId, request.eventType());
        NotificationPreference pref;
        if (existing.isPresent()) {
            pref = existing.get();
            pref.setInApp(request.inApp());
            pref.setEmail(request.email());
            log.info("Updated notification preference for userId={}, eventType={}, inApp={}, email={}", userId, request.eventType(), request.inApp(), request.email());
        } else {
            pref = NotificationPreference.builder()
                    .user(userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found")))
                    .eventType(request.eventType())
                    .inApp(request.inApp())
                    .email(request.email())
                    .build();
            log.info("Created notification preference for userId={}, eventType={}, inApp={}, email={}", userId, request.eventType(), request.inApp(), request.email());
        }
        pref = preferenceRepository.save(pref);
        return mapToResponse(pref);
    }

    /**
     * Batch creates or updates multiple notification preferences for a user.
     *
     * <p>Each request in the list is processed individually via
     * {@link #updatePreference(UUID, UpdateNotificationPreferenceRequest)}.</p>
     *
     * @param userId the ID of the user whose preferences to update
     * @param requests a list of update requests, each containing event type and channel flags
     * @return a list of created or updated notification preference response DTOs
     * @throws AccessDeniedException if the current user is not the specified user
     */
    public List<NotificationPreferenceResponse> updatePreferences(UUID userId, List<UpdateNotificationPreferenceRequest> requests) {
        log.debug("updatePreferences called with userId={}, count={}", userId, requests.size());
        return requests.stream()
                .map(request -> updatePreference(userId, request))
                .toList();
    }

    /**
     * Determines whether a notification should be sent to a user for a given
     * event type and delivery channel.
     *
     * <p>If no explicit preference exists for the event type, in-app notifications
     * default to {@code true} and all other channels default to {@code false}.</p>
     *
     * @param userId the ID of the user to check
     * @param eventType the event type identifier (e.g., "JOB_COMPLETED", "FINDING_CREATED")
     * @param channel the delivery channel to check ("inApp" or "email")
     * @return {@code true} if the user should receive the notification on the specified channel
     */
    @Transactional(readOnly = true)
    public boolean shouldNotify(UUID userId, String eventType, String channel) {
        log.debug("shouldNotify called with userId={}, eventType={}, channel={}", userId, eventType, channel);
        var pref = preferenceRepository.findByUserIdAndEventType(userId, eventType);
        if (pref.isEmpty()) {
            log.warn("No notification preference found for userId={}, eventType={}, defaulting for channel={}", userId, eventType, channel);
            return "inApp".equals(channel);
        }
        NotificationPreference p = pref.get();
        if ("inApp".equals(channel)) return p.getInApp();
        if ("email".equals(channel)) return p.getEmail();
        return false;
    }

    private NotificationPreferenceResponse mapToResponse(NotificationPreference pref) {
        return new NotificationPreferenceResponse(
                pref.getEventType(),
                pref.getInApp(),
                pref.getEmail()
        );
    }
}

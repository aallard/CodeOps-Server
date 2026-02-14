package com.codeops.service;

import com.codeops.dto.request.UpdateNotificationPreferenceRequest;
import com.codeops.dto.response.NotificationPreferenceResponse;
import com.codeops.entity.NotificationPreference;
import com.codeops.repository.NotificationPreferenceRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public NotificationPreferenceResponse updatePreference(UUID userId, UpdateNotificationPreferenceRequest request) {
        var existing = preferenceRepository.findByUserIdAndEventType(userId, request.eventType());
        NotificationPreference pref;
        if (existing.isPresent()) {
            pref = existing.get();
            pref.setInApp(request.inApp());
            pref.setEmail(request.email());
        } else {
            pref = NotificationPreference.builder()
                    .user(userRepository.getReferenceById(userId))
                    .eventType(request.eventType())
                    .inApp(request.inApp())
                    .email(request.email())
                    .build();
        }
        pref = preferenceRepository.save(pref);
        return mapToResponse(pref);
    }

    public List<NotificationPreferenceResponse> updatePreferences(UUID userId, List<UpdateNotificationPreferenceRequest> requests) {
        return requests.stream()
                .map(request -> updatePreference(userId, request))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean shouldNotify(UUID userId, String eventType, String channel) {
        var pref = preferenceRepository.findByUserIdAndEventType(userId, eventType);
        if (pref.isEmpty()) {
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

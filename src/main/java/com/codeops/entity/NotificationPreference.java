package com.codeops.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Builder.Default
    @Column(name = "in_app", nullable = false, columnDefinition = "boolean default true")
    private Boolean inApp = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean email = false;
}

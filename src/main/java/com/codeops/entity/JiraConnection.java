package com.codeops.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "jira_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraConnection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "instance_url", nullable = false, length = 500)
    private String instanceUrl;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "encrypted_api_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedApiToken;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}

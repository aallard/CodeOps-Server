package com.codeops.entity;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.Scope;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "personas", indexes = {
        @Index(name = "idx_persona_team_id", columnList = "team_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    private AgentType agentType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private Scope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = true)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Builder.Default
    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;
}

package com.codeops.entity;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ComplianceStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "compliance_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private QaJob job;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spec_id", nullable = true)
    private Specification spec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceStatus status;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type")
    private AgentType agentType;

    @Column(columnDefinition = "TEXT")
    private String notes;
}

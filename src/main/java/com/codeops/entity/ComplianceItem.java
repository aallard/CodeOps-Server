package com.codeops.entity;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ComplianceStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "compliance_items", indexes = {
        @Index(name = "idx_compliance_job_id", columnList = "job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private QaJob job;

    @Column(name = "requirement", nullable = false, columnDefinition = "TEXT")
    private String requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spec_id", nullable = true)
    private Specification spec;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ComplianceStatus status;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type")
    private AgentType agentType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}

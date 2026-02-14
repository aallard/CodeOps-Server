package com.codeops.entity;

import com.codeops.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "findings", indexes = {
        @Index(name = "idx_finding_job_id", columnList = "job_id"),
        @Index(name = "idx_finding_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Finding extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private QaJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    private AgentType agentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "effort_estimate")
    private Effort effortEstimate;

    @Enumerated(EnumType.STRING)
    @Column(name = "debt_category")
    private DebtCategory debtCategory;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(20) default 'OPEN'")
    private FindingStatus status = FindingStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_changed_by", nullable = true)
    private User statusChangedBy;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Version
    @Column(name = "version")
    private Long version;
}

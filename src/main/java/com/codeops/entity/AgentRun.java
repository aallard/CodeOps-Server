package com.codeops.entity;

import com.codeops.entity.enums.AgentResult;
import com.codeops.entity.enums.AgentStatus;
import com.codeops.entity.enums.AgentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "agent_runs", indexes = {
        @Index(name = "idx_agent_run_job_id", columnList = "job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private QaJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    private AgentType agentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AgentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "result")
    private AgentResult result;

    @Column(name = "report_s3_key", length = 500)
    private String reportS3Key;

    @Column(name = "score")
    private Integer score;

    @Builder.Default
    @Column(name = "findings_count")
    private Integer findingsCount = 0;

    @Builder.Default
    @Column(name = "critical_count")
    private Integer criticalCount = 0;

    @Builder.Default
    @Column(name = "high_count")
    private Integer highCount = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version")
    private Long version;
}

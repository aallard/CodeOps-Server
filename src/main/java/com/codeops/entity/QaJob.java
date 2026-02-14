package com.codeops.entity;

import com.codeops.entity.enums.JobMode;
import com.codeops.entity.enums.JobResult;
import com.codeops.entity.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "qa_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QaJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(length = 200)
    private String name;

    @Column(length = 100)
    private String branch;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "summary_md", columnDefinition = "TEXT")
    private String summaryMd;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_result")
    private JobResult overallResult;

    @Column(name = "health_score")
    private Integer healthScore;

    @Builder.Default
    @Column(name = "total_findings")
    private Integer totalFindings = 0;

    @Builder.Default
    @Column(name = "critical_count")
    private Integer criticalCount = 0;

    @Builder.Default
    @Column(name = "high_count")
    private Integer highCount = 0;

    @Builder.Default
    @Column(name = "medium_count")
    private Integer mediumCount = 0;

    @Builder.Default
    @Column(name = "low_count")
    private Integer lowCount = 0;

    @Column(name = "jira_ticket_key", length = 50)
    private String jiraTicketKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by", nullable = false)
    private User startedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}

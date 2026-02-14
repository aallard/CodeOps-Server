package com.codeops.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "health_snapshots", indexes = {
        @Index(name = "idx_snapshot_project_id", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthSnapshot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = true)
    private QaJob job;

    @Column(name = "health_score", nullable = false)
    private Integer healthScore;

    @Column(name = "findings_by_severity", columnDefinition = "TEXT")
    private String findingsBySeverity;

    @Column(name = "tech_debt_score")
    private Integer techDebtScore;

    @Column(name = "dependency_score")
    private Integer dependencyScore;

    @Column(name = "test_coverage_percent", precision = 5, scale = 2)
    private BigDecimal testCoveragePercent;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}

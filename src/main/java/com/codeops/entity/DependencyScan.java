package com.codeops.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dependency_scans", indexes = {
        @Index(name = "idx_dep_scan_project_id", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DependencyScan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = true)
    private QaJob job;

    @Column(name = "manifest_file", length = 200)
    private String manifestFile;

    @Column(name = "total_dependencies")
    private Integer totalDependencies;

    @Column(name = "outdated_count")
    private Integer outdatedCount;

    @Column(name = "vulnerable_count")
    private Integer vulnerableCount;

    @Column(name = "scan_data_json", columnDefinition = "TEXT")
    private String scanDataJson;
}

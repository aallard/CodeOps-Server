package com.codeops.entity;

import com.codeops.entity.enums.BusinessImpact;
import com.codeops.entity.enums.DebtCategory;
import com.codeops.entity.enums.DebtStatus;
import com.codeops.entity.enums.Effort;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tech_debt_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechDebtItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebtCategory category;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "effort_estimate")
    private Effort effortEstimate;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_impact")
    private BusinessImpact businessImpact;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20) default 'IDENTIFIED'")
    private DebtStatus status = DebtStatus.IDENTIFIED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_detected_job_id", nullable = true)
    private QaJob firstDetectedJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_job_id", nullable = true)
    private QaJob resolvedJob;
}

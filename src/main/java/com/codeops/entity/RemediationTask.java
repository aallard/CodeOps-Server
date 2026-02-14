package com.codeops.entity;

import com.codeops.entity.enums.Priority;
import com.codeops.entity.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "remediation_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private QaJob job;

    @Column(name = "task_number", nullable = false)
    private Integer taskNumber;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "prompt_md", columnDefinition = "TEXT")
    private String promptMd;

    @Column(name = "prompt_s3_key", length = 500)
    private String promptS3Key;

    @Column(name = "finding_ids", columnDefinition = "TEXT")
    private String findingIds;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20) default 'PENDING'")
    private TaskStatus status = TaskStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = true)
    private User assignedTo;

    @Column(name = "jira_key", length = 50)
    private String jiraKey;
}

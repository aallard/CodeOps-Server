package com.codeops.entity;

import com.codeops.entity.enums.Priority;
import com.codeops.entity.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "remediation_tasks", indexes = {
        @Index(name = "idx_task_job_id", columnList = "job_id")
})
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

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "prompt_md", columnDefinition = "TEXT")
    private String promptMd;

    @Column(name = "prompt_s3_key", length = 500)
    private String promptS3Key;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "remediation_task_findings",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "finding_id")
    )
    @Builder.Default
    private List<Finding> findings = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(20) default 'PENDING'")
    private TaskStatus status = TaskStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = true)
    private User assignedTo;

    @Column(name = "jira_key", length = 50)
    private String jiraKey;

    @Version
    @Column(name = "version")
    private Long version;
}

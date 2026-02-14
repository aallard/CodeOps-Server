package com.codeops.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bug_investigations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BugInvestigation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private QaJob job;

    @Column(name = "jira_key", length = 50)
    private String jiraKey;

    @Column(name = "jira_summary", columnDefinition = "TEXT")
    private String jiraSummary;

    @Column(name = "jira_description", columnDefinition = "TEXT")
    private String jiraDescription;

    @Column(name = "jira_comments_json", columnDefinition = "TEXT")
    private String jiraCommentsJson;

    @Column(name = "jira_attachments_json", columnDefinition = "TEXT")
    private String jiraAttachmentsJson;

    @Column(name = "jira_linked_issues", columnDefinition = "TEXT")
    private String jiraLinkedIssues;

    @Column(name = "additional_context", columnDefinition = "TEXT")
    private String additionalContext;

    @Column(name = "rca_md", columnDefinition = "TEXT")
    private String rcaMd;

    @Column(name = "impact_assessment_md", columnDefinition = "TEXT")
    private String impactAssessmentMd;

    @Column(name = "rca_s3_key", length = 500)
    private String rcaS3Key;

    @Builder.Default
    @Column(name = "rca_posted_to_jira")
    private Boolean rcaPostedToJira = false;

    @Builder.Default
    @Column(name = "fix_tasks_created_in_jira")
    private Boolean fixTasksCreatedInJira = false;
}

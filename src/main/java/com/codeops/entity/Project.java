package com.codeops.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "github_connection_id", nullable = true)
    private GitHubConnection githubConnection;

    @Column(name = "repo_url", length = 500)
    private String repoUrl;

    @Column(name = "repo_full_name", length = 200)
    private String repoFullName;

    @Builder.Default
    @Column(name = "default_branch", columnDefinition = "varchar(100) default 'main'")
    private String defaultBranch = "main";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_connection_id", nullable = true)
    private JiraConnection jiraConnection;

    @Column(name = "jira_project_key", length = 20)
    private String jiraProjectKey;

    @Builder.Default
    @Column(name = "jira_default_issue_type", columnDefinition = "varchar(50) default 'Task'")
    private String jiraDefaultIssueType = "Task";

    @Column(name = "jira_labels", columnDefinition = "TEXT")
    private String jiraLabels;

    @Column(name = "jira_component", length = 100)
    private String jiraComponent;

    @Column(name = "tech_stack", length = 200)
    private String techStack;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "last_audit_at")
    private Instant lastAuditAt;

    @Column(name = "settings_json", columnDefinition = "TEXT")
    private String settingsJson;

    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}

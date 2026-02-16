package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateProjectRequest;
import com.codeops.dto.request.UpdateProjectRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.ProjectResponse;
import com.codeops.entity.Project;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.*;
import com.codeops.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages project lifecycle including creation, retrieval, updating, archiving,
 * deletion, and health score tracking.
 *
 * <p>Projects are the primary organizational unit for code repositories within a team.
 * Each project can be linked to a GitHub connection and a Jira connection for
 * integration with external tools. The number of projects per team is capped at
 * {@link AppConstants#MAX_PROJECTS_PER_TEAM}.</p>
 *
 * <p>All operations enforce team membership or admin/owner role requirements.
 * Project deletion is restricted to team owners only.</p>
 *
 * @see ProjectController
 * @see Project
 * @see GitHubConnectionService
 * @see JiraConnectionService
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final GitHubConnectionRepository gitHubConnectionRepository;
    private final JiraConnectionRepository jiraConnectionRepository;
    private final ObjectMapper objectMapper;
    private final RemediationTaskRepository remediationTaskRepository;
    private final ComplianceItemRepository complianceItemRepository;
    private final SpecificationRepository specificationRepository;
    private final FindingRepository findingRepository;
    private final AgentRunRepository agentRunRepository;
    private final BugInvestigationRepository bugInvestigationRepository;
    private final TechDebtItemRepository techDebtItemRepository;
    private final DependencyVulnerabilityRepository dependencyVulnerabilityRepository;
    private final DependencyScanRepository dependencyScanRepository;
    private final HealthSnapshotRepository healthSnapshotRepository;
    private final QaJobRepository qaJobRepository;
    private final HealthScheduleRepository healthScheduleRepository;
    private final ProjectDirectiveRepository projectDirectiveRepository;
    private final DirectiveRepository directiveRepository;

    /**
     * Creates a new project within the specified team.
     *
     * <p>Validates the team has not exceeded its project limit. Sets default values
     * for branch ("main"), Jira issue type ("Task"), and health score. Optionally
     * links the project to existing GitHub and Jira connections. The current user
     * is recorded as the project creator.</p>
     *
     * @param teamId the ID of the team to create the project in
     * @param request the project creation request containing name, description,
     *                repository details, Jira configuration, tech stack, and
     *                optional connection IDs
     * @return the created project as a response DTO
     * @throws IllegalArgumentException if the team has reached the maximum project count
     * @throws EntityNotFoundException if the team, current user, or referenced connections are not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the team
     */
    public ProjectResponse createProject(UUID teamId, CreateProjectRequest request) {
        log.debug("createProject called with teamId={}, name={}", teamId, request.name());
        verifyTeamAdmin(teamId);

        long projectCount = projectRepository.countByTeamId(teamId);
        if (projectCount >= AppConstants.MAX_PROJECTS_PER_TEAM) {
            log.warn("Project creation rejected: teamId={} at max project capacity={}", teamId, projectCount);
            throw new IllegalArgumentException("Team has reached the maximum number of projects");
        }

        Project project = Project.builder()
                .team(teamRepository.findById(teamId).orElseThrow(() -> new EntityNotFoundException("Team not found")))
                .name(request.name())
                .description(request.description())
                .repoUrl(request.repoUrl())
                .repoFullName(request.repoFullName())
                .defaultBranch(request.defaultBranch() != null ? request.defaultBranch() : "main")
                .jiraProjectKey(request.jiraProjectKey())
                .jiraDefaultIssueType(request.jiraDefaultIssueType() != null ? request.jiraDefaultIssueType() : "Task")
                .jiraLabels(serializeLabels(request.jiraLabels()))
                .jiraComponent(request.jiraComponent())
                .techStack(request.techStack())
                .healthScore(AppConstants.DEFAULT_HEALTH_SCORE)
                .isArchived(false)
                .createdBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")))
                .build();

        if (request.githubConnectionId() != null) {
            project.setGithubConnection(gitHubConnectionRepository.findById(request.githubConnectionId()).orElseThrow(() -> new EntityNotFoundException("GitHub connection not found")));
        }
        if (request.jiraConnectionId() != null) {
            project.setJiraConnection(jiraConnectionRepository.findById(request.jiraConnectionId()).orElseThrow(() -> new EntityNotFoundException("Jira connection not found")));
        }

        project = projectRepository.save(project);
        log.info("Project created: projectId={}, teamId={}, name={}", project.getId(), teamId, project.getName());
        return mapToProjectResponse(project);
    }

    /**
     * Retrieves a single project by its ID.
     *
     * @param projectId the ID of the project to retrieve
     * @return the project as a response DTO
     * @throws EntityNotFoundException if no project exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        log.debug("getProject called with projectId={}", projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        return mapToProjectResponse(project);
    }

    /**
     * Retrieves all non-archived projects for a team.
     *
     * @param teamId the ID of the team whose active projects to retrieve
     * @return a list of non-archived project response DTOs
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsForTeam(UUID teamId) {
        log.debug("getProjectsForTeam called with teamId={}", teamId);
        verifyTeamMembership(teamId);
        return projectRepository.findByTeamIdAndIsArchivedFalse(teamId).stream()
                .map(this::mapToProjectResponse)
                .toList();
    }

    /**
     * Retrieves a paginated list of projects for a team, optionally including archived projects.
     *
     * @param teamId the ID of the team whose projects to retrieve
     * @param includeArchived {@code true} to include archived projects, {@code false} to exclude them
     * @param pageable pagination and sorting parameters
     * @return a paginated response of project DTOs
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getAllProjectsForTeam(UUID teamId, boolean includeArchived, Pageable pageable) {
        log.debug("getAllProjectsForTeam called with teamId={}, includeArchived={}", teamId, includeArchived);
        verifyTeamMembership(teamId);
        Page<Project> page = includeArchived
                ? projectRepository.findByTeamId(teamId, pageable)
                : projectRepository.findByTeamIdAndIsArchivedFalse(teamId, pageable);
        List<ProjectResponse> content = page.getContent().stream()
                .map(this::mapToProjectResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Updates an existing project's mutable fields.
     *
     * <p>Only non-null fields in the request are applied. Connection references
     * (GitHub, Jira) are resolved and validated if provided. Jira labels are
     * serialized to JSON for storage.</p>
     *
     * @param projectId the ID of the project to update
     * @param request the update request containing optional field values to apply
     * @return the updated project as a response DTO
     * @throws EntityNotFoundException if the project or referenced connections are not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the project's team
     */
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        log.debug("updateProject called with projectId={}", projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());

        if (request.name() != null) project.setName(request.name());
        if (request.description() != null) project.setDescription(request.description());
        if (request.githubConnectionId() != null) {
            project.setGithubConnection(gitHubConnectionRepository.findById(request.githubConnectionId()).orElseThrow(() -> new EntityNotFoundException("GitHub connection not found")));
        }
        if (request.repoUrl() != null) project.setRepoUrl(request.repoUrl());
        if (request.repoFullName() != null) project.setRepoFullName(request.repoFullName());
        if (request.defaultBranch() != null) project.setDefaultBranch(request.defaultBranch());
        if (request.jiraConnectionId() != null) {
            project.setJiraConnection(jiraConnectionRepository.findById(request.jiraConnectionId()).orElseThrow(() -> new EntityNotFoundException("Jira connection not found")));
        }
        if (request.jiraProjectKey() != null) project.setJiraProjectKey(request.jiraProjectKey());
        if (request.jiraDefaultIssueType() != null) project.setJiraDefaultIssueType(request.jiraDefaultIssueType());
        if (request.jiraLabels() != null) project.setJiraLabels(serializeLabels(request.jiraLabels()));
        if (request.jiraComponent() != null) project.setJiraComponent(request.jiraComponent());
        if (request.techStack() != null) project.setTechStack(request.techStack());
        if (request.isArchived() != null) project.setIsArchived(request.isArchived());

        project = projectRepository.save(project);
        log.info("Project updated: projectId={}, name={}", project.getId(), project.getName());
        return mapToProjectResponse(project);
    }

    /**
     * Archives a project by setting its archived flag to {@code true}.
     *
     * <p>Archived projects are excluded from default project listings but
     * remain in the database and can be unarchived.</p>
     *
     * @param projectId the ID of the project to archive
     * @throws EntityNotFoundException if no project exists with the given ID
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the project's team
     */
    public void archiveProject(UUID projectId) {
        log.debug("archiveProject called with projectId={}", projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());
        project.setIsArchived(true);
        projectRepository.save(project);
        log.info("Project archived: projectId={}, name={}", projectId, project.getName());
    }

    /**
     * Restores an archived project by setting its archived flag to {@code false}.
     *
     * @param projectId the ID of the project to unarchive
     * @throws EntityNotFoundException if no project exists with the given ID
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the project's team
     */
    public void unarchiveProject(UUID projectId) {
        log.debug("unarchiveProject called with projectId={}", projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());
        project.setIsArchived(false);
        projectRepository.save(project);
        log.info("Project unarchived: projectId={}, name={}", projectId, project.getName());
    }

    /**
     * Permanently deletes a project and all associated data.
     *
     * <p>Deletion is restricted to team owners only. This is a hard delete
     * that cannot be undone.</p>
     *
     * @param projectId the ID of the project to delete
     * @throws EntityNotFoundException if no project exists with the given ID
     * @throws AccessDeniedException if the current user is not a team member or does not have OWNER role
     */
    public void deleteProject(UUID projectId) {
        log.debug("deleteProject called with projectId={}", projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(project.getTeam().getId(), currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        if (member.getRole() != TeamRole.OWNER) {
            throw new AccessDeniedException("Only the team owner can delete projects");
        }

        // Delete all child records in FK-safe order before deleting the project
        remediationTaskRepository.deleteJoinTableByProjectId(projectId);
        remediationTaskRepository.deleteAllByProjectId(projectId);
        complianceItemRepository.deleteAllByProjectId(projectId);
        specificationRepository.deleteAllByProjectId(projectId);
        findingRepository.deleteAllByProjectId(projectId);
        agentRunRepository.deleteAllByProjectId(projectId);
        bugInvestigationRepository.deleteAllByProjectId(projectId);
        techDebtItemRepository.deleteAllByProjectId(projectId);
        dependencyVulnerabilityRepository.deleteAllByProjectId(projectId);
        dependencyScanRepository.deleteAllByProjectId(projectId);
        healthSnapshotRepository.deleteAllByProjectId(projectId);
        qaJobRepository.deleteAllByProjectId(projectId);
        healthScheduleRepository.deleteAllByProjectId(projectId);
        projectDirectiveRepository.deleteAllByProjectId(projectId);
        directiveRepository.deleteAllByProjectId(projectId);
        projectRepository.delete(project);
        log.info("Project deleted: projectId={}, name={}, deletedBy={}", projectId, project.getName(), currentUserId);
    }

    /**
     * Updates a project's health score and records the current timestamp as the last audit time.
     *
     * <p>Typically called internally when a QA job completes with a computed health score.
     * This is a side effect of {@link QaJobService#updateJob(UUID, UpdateJobRequest)}.</p>
     *
     * @param projectId the ID of the project whose health score to update
     * @param score the new health score value (0-100)
     * @throws EntityNotFoundException if no project exists with the given ID
     */
    public void updateHealthScore(UUID projectId, int score) {
        log.debug("updateHealthScore called with projectId={}, score={}", projectId, score);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        project.setHealthScore(score);
        project.setLastAuditAt(Instant.now());
        projectRepository.save(project);
        log.info("Health score updated: projectId={}, score={}", projectId, score);
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTeam().getId(),
                project.getName(),
                project.getDescription(),
                project.getGithubConnection() != null ? project.getGithubConnection().getId() : null,
                project.getRepoUrl(),
                project.getRepoFullName(),
                project.getDefaultBranch(),
                project.getJiraConnection() != null ? project.getJiraConnection().getId() : null,
                project.getJiraProjectKey(),
                project.getJiraDefaultIssueType(),
                deserializeLabels(project.getJiraLabels()),
                project.getJiraComponent(),
                project.getTechStack(),
                project.getHealthScore(),
                project.getLastAuditAt(),
                project.getIsArchived(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private String serializeLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(labels);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize labels: {}", labels, e);
            throw new RuntimeException("Failed to serialize labels", e);
        }
    }

    private List<String> deserializeLabels(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }

    private void verifyTeamAdmin(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        if (member.getRole() != TeamRole.OWNER && member.getRole() != TeamRole.ADMIN) {
            throw new AccessDeniedException("Requires OWNER or ADMIN role");
        }
    }
}

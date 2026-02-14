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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final GitHubConnectionRepository gitHubConnectionRepository;
    private final JiraConnectionRepository jiraConnectionRepository;
    private final ObjectMapper objectMapper;

    public ProjectResponse createProject(UUID teamId, CreateProjectRequest request) {
        verifyTeamAdmin(teamId);

        long projectCount = projectRepository.countByTeamId(teamId);
        if (projectCount >= AppConstants.MAX_PROJECTS_PER_TEAM) {
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
        return mapToProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        return mapToProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsForTeam(UUID teamId) {
        verifyTeamMembership(teamId);
        return projectRepository.findByTeamIdAndIsArchivedFalse(teamId).stream()
                .map(this::mapToProjectResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getAllProjectsForTeam(UUID teamId, boolean includeArchived, Pageable pageable) {
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

    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
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
        return mapToProjectResponse(project);
    }

    public void archiveProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());
        project.setIsArchived(true);
        projectRepository.save(project);
    }

    public void unarchiveProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());
        project.setIsArchived(false);
        projectRepository.save(project);
    }

    public void deleteProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(project.getTeam().getId(), currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        if (member.getRole() != TeamRole.OWNER) {
            throw new AccessDeniedException("Only the team owner can delete projects");
        }

        projectRepository.delete(project);
    }

    public void updateHealthScore(UUID projectId, int score) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        project.setHealthScore(score);
        project.setLastAuditAt(Instant.now());
        projectRepository.save(project);
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

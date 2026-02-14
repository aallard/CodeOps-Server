package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.AssignDirectiveRequest;
import com.codeops.dto.request.CreateDirectiveRequest;
import com.codeops.dto.request.UpdateDirectiveRequest;
import com.codeops.dto.response.DirectiveResponse;
import com.codeops.dto.response.ProjectDirectiveResponse;
import com.codeops.entity.Directive;
import com.codeops.entity.ProjectDirective;
import com.codeops.entity.ProjectDirectiveId;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.DirectiveScope;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.*;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DirectiveService {

    private final DirectiveRepository directiveRepository;
    private final ProjectDirectiveRepository projectDirectiveRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public DirectiveResponse createDirective(CreateDirectiveRequest request) {
        if (request.scope() == DirectiveScope.TEAM && request.teamId() == null) {
            throw new IllegalArgumentException("teamId is required for TEAM scope directives");
        }
        if (request.scope() == DirectiveScope.PROJECT && request.projectId() == null) {
            throw new IllegalArgumentException("projectId is required for PROJECT scope directives");
        }

        UUID teamId = request.teamId();
        if (request.scope() == DirectiveScope.TEAM && teamId != null) {
            verifyTeamAdmin(teamId);
        }
        if (request.scope() == DirectiveScope.PROJECT && request.projectId() != null) {
            var project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            teamId = project.getTeam().getId();
            verifyTeamAdmin(teamId);
        }

        Directive directive = Directive.builder()
                .name(request.name())
                .description(request.description())
                .contentMd(request.contentMd())
                .category(request.category())
                .scope(request.scope())
                .team(request.teamId() != null ? teamRepository.findById(request.teamId()).orElseThrow(() -> new EntityNotFoundException("Team not found")) : null)
                .project(request.projectId() != null ? projectRepository.findById(request.projectId()).orElseThrow(() -> new EntityNotFoundException("Project not found")) : null)
                .createdBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")))
                .version(1)
                .build();

        directive = directiveRepository.save(directive);
        return mapToResponse(directive);
    }

    @Transactional(readOnly = true)
    public DirectiveResponse getDirective(UUID directiveId) {
        Directive directive = directiveRepository.findById(directiveId)
                .orElseThrow(() -> new EntityNotFoundException("Directive not found"));
        UUID teamId = directive.getTeam() != null ? directive.getTeam().getId()
                : (directive.getProject() != null ? directive.getProject().getTeam().getId() : null);
        if (teamId != null) {
            verifyTeamMembership(teamId);
        }
        return mapToResponse(directive);
    }

    @Transactional(readOnly = true)
    public List<DirectiveResponse> getDirectivesForTeam(UUID teamId) {
        verifyTeamMembership(teamId);
        return directiveRepository.findByTeamId(teamId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DirectiveResponse> getDirectivesForProject(UUID projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        return directiveRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DirectiveResponse> getDirectivesByCategory(UUID teamId, DirectiveScope scope) {
        verifyTeamMembership(teamId);
        return directiveRepository.findByTeamIdAndScope(teamId, scope).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DirectiveResponse updateDirective(UUID directiveId, UpdateDirectiveRequest request) {
        Directive directive = directiveRepository.findById(directiveId)
                .orElseThrow(() -> new EntityNotFoundException("Directive not found"));
        verifyCreatorOrTeamAdmin(directive);

        if (request.name() != null) directive.setName(request.name());
        if (request.description() != null) directive.setDescription(request.description());
        if (request.contentMd() != null) {
            directive.setContentMd(request.contentMd());
            directive.setVersion(directive.getVersion() + 1);
        }
        if (request.category() != null) directive.setCategory(request.category());

        directive = directiveRepository.save(directive);
        return mapToResponse(directive);
    }

    public void deleteDirective(UUID directiveId) {
        Directive directive = directiveRepository.findById(directiveId)
                .orElseThrow(() -> new EntityNotFoundException("Directive not found"));
        verifyCreatorOrTeamAdmin(directive);
        projectDirectiveRepository.deleteAll(projectDirectiveRepository.findByDirectiveId(directiveId));
        directiveRepository.delete(directive);
    }

    public ProjectDirectiveResponse assignToProject(AssignDirectiveRequest request) {
        Directive directive = directiveRepository.findById(request.directiveId())
                .orElseThrow(() -> new EntityNotFoundException("Directive not found"));
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());

        int count = projectDirectiveRepository.findByProjectId(request.projectId()).size();
        if (count >= AppConstants.MAX_DIRECTIVES_PER_PROJECT) {
            throw new IllegalArgumentException("Project has reached the maximum number of directives");
        }

        ProjectDirective pd = ProjectDirective.builder()
                .id(new ProjectDirectiveId(request.projectId(), request.directiveId()))
                .project(project)
                .directive(directive)
                .enabled(request.enabled())
                .build();
        pd = projectDirectiveRepository.save(pd);
        return mapToProjectDirectiveResponse(pd, directive);
    }

    public void removeFromProject(UUID projectId, UUID directiveId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());
        projectDirectiveRepository.deleteByProjectIdAndDirectiveId(projectId, directiveId);
    }

    @Transactional(readOnly = true)
    public List<ProjectDirectiveResponse> getProjectDirectives(UUID projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        return projectDirectiveRepository.findByProjectId(projectId).stream()
                .map(pd -> mapToProjectDirectiveResponse(pd, pd.getDirective()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DirectiveResponse> getEnabledDirectivesForProject(UUID projectId) {
        return projectDirectiveRepository.findByProjectIdAndEnabledTrue(projectId).stream()
                .map(pd -> mapToResponse(pd.getDirective()))
                .toList();
    }

    public ProjectDirectiveResponse toggleProjectDirective(UUID projectId, UUID directiveId, boolean enabled) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());
        ProjectDirectiveId pdId = new ProjectDirectiveId(projectId, directiveId);
        ProjectDirective pd = projectDirectiveRepository.findById(pdId)
                .orElseThrow(() -> new EntityNotFoundException("Project directive assignment not found"));
        pd.setEnabled(enabled);
        pd = projectDirectiveRepository.save(pd);
        return mapToProjectDirectiveResponse(pd, pd.getDirective());
    }

    private DirectiveResponse mapToResponse(Directive directive) {
        return new DirectiveResponse(
                directive.getId(),
                directive.getName(),
                directive.getDescription(),
                directive.getContentMd(),
                directive.getCategory(),
                directive.getScope(),
                directive.getTeam() != null ? directive.getTeam().getId() : null,
                directive.getProject() != null ? directive.getProject().getId() : null,
                directive.getCreatedBy().getId(),
                directive.getCreatedBy().getDisplayName(),
                directive.getVersion(),
                directive.getCreatedAt(),
                directive.getUpdatedAt()
        );
    }

    private ProjectDirectiveResponse mapToProjectDirectiveResponse(ProjectDirective pd, Directive directive) {
        return new ProjectDirectiveResponse(
                pd.getId().getProjectId(),
                pd.getId().getDirectiveId(),
                directive.getName(),
                directive.getCategory(),
                pd.getEnabled()
        );
    }

    private void verifyCreatorOrTeamAdmin(Directive directive) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (directive.getCreatedBy().getId().equals(currentUserId)) return;
        UUID teamId = directive.getTeam() != null ? directive.getTeam().getId()
                : (directive.getProject() != null ? directive.getProject().getTeam().getId() : null);
        if (teamId != null) {
            var member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId);
            if (member.isPresent() && (member.get().getRole() == TeamRole.OWNER || member.get().getRole() == TeamRole.ADMIN)) {
                return;
            }
        }
        throw new AccessDeniedException("Not authorized to modify this directive");
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

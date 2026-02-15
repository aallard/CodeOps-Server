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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages directives (coding standards, guidelines, policies) and their assignments to projects.
 *
 * <p>Directives can be scoped to a team or project level via {@link DirectiveScope}. They are
 * versioned: updating the content markdown increments the version number. Directives are
 * assigned to projects through the {@code project_directives} join table and can be individually
 * enabled or disabled per project. A maximum of {@link AppConstants#MAX_DIRECTIVES_PER_PROJECT}
 * directives can be assigned to a single project.</p>
 *
 * @see DirectiveController
 * @see DirectiveRepository
 * @see ProjectDirectiveRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DirectiveService {

    private static final Logger log = LoggerFactory.getLogger(DirectiveService.class);

    private final DirectiveRepository directiveRepository;
    private final ProjectDirectiveRepository projectDirectiveRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    /**
     * Creates a new directive with the specified scope, category, and markdown content.
     *
     * <p>For TEAM-scoped directives, a {@code teamId} is required and the caller must be a team admin.
     * For PROJECT-scoped directives, a {@code projectId} is required and the caller must be an admin
     * of the project's team. The directive is initialized at version 1 with the current user as creator.</p>
     *
     * @param request the creation request containing name, description, content, category, scope, and optional team/project IDs
     * @return the newly created directive as a response DTO
     * @throws IllegalArgumentException if required IDs are missing for the given scope
     * @throws EntityNotFoundException if the referenced team, project, or current user does not exist
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role in the team
     */
    public DirectiveResponse createDirective(CreateDirectiveRequest request) {
        log.debug("createDirective called with name={}, scope={}, category={}", request.name(), request.scope(), request.category());
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
        log.info("Directive created: directiveId={}, name={}, scope={}", directive.getId(), directive.getName(), directive.getScope());
        return mapToResponse(directive);
    }

    /**
     * Retrieves a directive by its unique identifier.
     *
     * <p>If the directive is associated with a team or project, team membership is verified.</p>
     *
     * @param directiveId the UUID of the directive to retrieve
     * @return the directive as a response DTO
     * @throws EntityNotFoundException if no directive exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    @Transactional(readOnly = true)
    public DirectiveResponse getDirective(UUID directiveId) {
        log.debug("getDirective called with directiveId={}", directiveId);
        Directive directive = directiveRepository.findById(directiveId)
                .orElseThrow(() -> new EntityNotFoundException("Directive not found"));
        UUID teamId = directive.getTeam() != null ? directive.getTeam().getId()
                : (directive.getProject() != null ? directive.getProject().getTeam().getId() : null);
        if (teamId != null) {
            verifyTeamMembership(teamId);
        }
        return mapToResponse(directive);
    }

    /**
     * Retrieves all directives directly associated with a team.
     *
     * @param teamId the UUID of the team to retrieve directives for
     * @return a list of directive response DTOs belonging to the team
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public List<DirectiveResponse> getDirectivesForTeam(UUID teamId) {
        log.debug("getDirectivesForTeam called with teamId={}", teamId);
        verifyTeamMembership(teamId);
        return directiveRepository.findByTeamId(teamId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves all directives directly associated with a project (not assigned via project_directives).
     *
     * @param projectId the UUID of the project to retrieve directives for
     * @return a list of directive response DTOs scoped to the project
     * @throws EntityNotFoundException if the referenced project does not exist
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public List<DirectiveResponse> getDirectivesForProject(UUID projectId) {
        log.debug("getDirectivesForProject called with projectId={}", projectId);
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        return directiveRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves directives for a team filtered by directive scope.
     *
     * @param teamId the UUID of the team to retrieve directives for
     * @param scope  the directive scope to filter by (e.g., TEAM, PROJECT)
     * @return a list of directive response DTOs matching the given scope
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public List<DirectiveResponse> getDirectivesByCategory(UUID teamId, DirectiveScope scope) {
        log.debug("getDirectivesByCategory called with teamId={}, scope={}", teamId, scope);
        verifyTeamMembership(teamId);
        return directiveRepository.findByTeamIdAndScope(teamId, scope).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Partially updates a directive with the non-null fields from the request.
     *
     * <p>If the {@code contentMd} field is updated, the directive's version number is incremented.
     * Only the original creator or a team admin/owner can perform this operation.</p>
     *
     * @param directiveId the UUID of the directive to update
     * @param request     the update request containing fields to modify (null fields are skipped)
     * @return the updated directive as a response DTO
     * @throws EntityNotFoundException if no directive exists with the given ID
     * @throws AccessDeniedException if the current user is neither the creator nor a team admin/owner
     */
    public DirectiveResponse updateDirective(UUID directiveId, UpdateDirectiveRequest request) {
        log.debug("updateDirective called with directiveId={}", directiveId);
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
        log.info("Directive updated: directiveId={}, name={}, version={}", directive.getId(), directive.getName(), directive.getVersion());
        return mapToResponse(directive);
    }

    /**
     * Deletes a directive and removes all its project assignments.
     *
     * <p>First deletes all rows in the {@code project_directives} join table referencing this
     * directive, then deletes the directive itself. Only the original creator or a team admin/owner
     * can perform this operation.</p>
     *
     * @param directiveId the UUID of the directive to delete
     * @throws EntityNotFoundException if no directive exists with the given ID
     * @throws AccessDeniedException if the current user is neither the creator nor a team admin/owner
     */
    public void deleteDirective(UUID directiveId) {
        log.debug("deleteDirective called with directiveId={}", directiveId);
        Directive directive = directiveRepository.findById(directiveId)
                .orElseThrow(() -> new EntityNotFoundException("Directive not found"));
        verifyCreatorOrTeamAdmin(directive);
        projectDirectiveRepository.deleteAll(projectDirectiveRepository.findByDirectiveId(directiveId));
        directiveRepository.delete(directive);
        log.info("Directive deleted: directiveId={}, name={}", directiveId, directive.getName());
    }

    /**
     * Assigns a directive to a project, creating an entry in the project_directives join table.
     *
     * <p>Enforces the maximum number of directives per project defined by
     * {@link AppConstants#MAX_DIRECTIVES_PER_PROJECT}. Requires OWNER or ADMIN role
     * in the project's team.</p>
     *
     * @param request the assignment request containing directive ID, project ID, and enabled flag
     * @return the project directive assignment as a response DTO
     * @throws EntityNotFoundException if the referenced directive or project does not exist
     * @throws IllegalArgumentException if the project has reached the maximum directive limit
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role in the team
     */
    public ProjectDirectiveResponse assignToProject(AssignDirectiveRequest request) {
        log.debug("assignToProject called with directiveId={}, projectId={}", request.directiveId(), request.projectId());
        Directive directive = directiveRepository.findById(request.directiveId())
                .orElseThrow(() -> new EntityNotFoundException("Directive not found"));
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());

        int count = projectDirectiveRepository.findByProjectId(request.projectId()).size();
        if (count >= AppConstants.MAX_DIRECTIVES_PER_PROJECT) {
            log.warn("Directive assignment rejected: projectId={} at max directive capacity={}", request.projectId(), count);
            throw new IllegalArgumentException("Project has reached the maximum number of directives");
        }

        ProjectDirective pd = ProjectDirective.builder()
                .id(new ProjectDirectiveId(request.projectId(), request.directiveId()))
                .project(project)
                .directive(directive)
                .enabled(request.enabled())
                .build();
        pd = projectDirectiveRepository.save(pd);
        log.info("Directive assigned to project: directiveId={}, projectId={}, enabled={}", request.directiveId(), request.projectId(), request.enabled());
        return mapToProjectDirectiveResponse(pd, directive);
    }

    /**
     * Removes a directive assignment from a project by deleting the project_directives join table entry.
     *
     * @param projectId   the UUID of the project to remove the directive from
     * @param directiveId the UUID of the directive to unassign
     * @throws EntityNotFoundException if the referenced project does not exist
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role in the team
     */
    public void removeFromProject(UUID projectId, UUID directiveId) {
        log.debug("removeFromProject called with projectId={}, directiveId={}", projectId, directiveId);
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());
        projectDirectiveRepository.deleteByProjectIdAndDirectiveId(projectId, directiveId);
        log.info("Directive unassigned from project: directiveId={}, projectId={}", directiveId, projectId);
    }

    /**
     * Retrieves all directive assignments for a project, including both enabled and disabled directives.
     *
     * @param projectId the UUID of the project to retrieve directive assignments for
     * @return a list of project directive response DTOs
     * @throws EntityNotFoundException if the referenced project does not exist
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public List<ProjectDirectiveResponse> getProjectDirectives(UUID projectId) {
        log.debug("getProjectDirectives called with projectId={}", projectId);
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        return projectDirectiveRepository.findByProjectId(projectId).stream()
                .map(pd -> mapToProjectDirectiveResponse(pd, pd.getDirective()))
                .toList();
    }

    /**
     * Retrieves only the enabled directives assigned to a project.
     *
     * <p>This is typically used when running QA jobs to determine which directives
     * should be enforced for the project.</p>
     *
     * @param projectId the UUID of the project to retrieve enabled directives for
     * @return a list of directive response DTOs for enabled assignments only
     */
    @Transactional(readOnly = true)
    public List<DirectiveResponse> getEnabledDirectivesForProject(UUID projectId) {
        log.debug("getEnabledDirectivesForProject called with projectId={}", projectId);
        return projectDirectiveRepository.findByProjectIdAndEnabledTrue(projectId).stream()
                .map(pd -> mapToResponse(pd.getDirective()))
                .toList();
    }

    /**
     * Toggles the enabled/disabled status of a directive assignment for a project.
     *
     * @param projectId   the UUID of the project
     * @param directiveId the UUID of the directive
     * @param enabled     {@code true} to enable the directive, {@code false} to disable it
     * @return the updated project directive assignment as a response DTO
     * @throws EntityNotFoundException if the project or the directive assignment does not exist
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role in the team
     */
    public ProjectDirectiveResponse toggleProjectDirective(UUID projectId, UUID directiveId, boolean enabled) {
        log.debug("toggleProjectDirective called with projectId={}, directiveId={}, enabled={}", projectId, directiveId, enabled);
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());
        ProjectDirectiveId pdId = new ProjectDirectiveId(projectId, directiveId);
        ProjectDirective pd = projectDirectiveRepository.findById(pdId)
                .orElseThrow(() -> new EntityNotFoundException("Project directive assignment not found"));
        pd.setEnabled(enabled);
        pd = projectDirectiveRepository.save(pd);
        log.info("Project directive toggled: projectId={}, directiveId={}, enabled={}", projectId, directiveId, enabled);
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

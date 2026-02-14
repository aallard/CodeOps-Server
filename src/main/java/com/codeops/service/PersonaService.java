package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreatePersonaRequest;
import com.codeops.dto.request.UpdatePersonaRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.PersonaResponse;
import com.codeops.entity.Persona;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.Scope;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.PersonaRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages AI agent persona definitions including creation, retrieval, updating,
 * deletion, and default persona assignment.
 *
 * <p>Personas define the behavior and instructions for AI agents. They are scoped
 * as SYSTEM (read-only, built-in), TEAM (shared within a team), or USER (personal).
 * Each team can have one default persona per agent type, which is automatically
 * used when no specific persona is selected.</p>
 *
 * <p>SYSTEM personas cannot be created, modified, or deleted through this service.
 * TEAM-scoped personas require admin/owner role for creation and can be modified
 * by their creator or a team admin/owner. The number of personas per team is
 * capped at {@link AppConstants#MAX_PERSONAS_PER_TEAM}.</p>
 *
 * @see PersonaController
 * @see Persona
 * @see AgentType
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    /**
     * Creates a new persona with the specified scope and configuration.
     *
     * <p>Validates that SYSTEM scope cannot be used, that TEAM scope requires a team ID,
     * and that the team has not exceeded its persona limit. If the persona is marked
     * as default and has both an agent type and team, the existing default for that
     * combination is cleared. The persona version starts at 1.</p>
     *
     * @param request the persona creation request containing name, agent type, description,
     *                content markdown, scope, optional team ID, and default flag
     * @return the created persona as a response DTO
     * @throws IllegalArgumentException if scope is SYSTEM, if TEAM scope lacks a team ID,
     *                                  or if the team has reached the maximum persona count
     * @throws EntityNotFoundException if the team or current user is not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role for TEAM-scoped personas
     */
    public PersonaResponse createPersona(CreatePersonaRequest request) {
        if (request.scope() == Scope.SYSTEM) {
            throw new IllegalArgumentException("Cannot create SYSTEM personas");
        }
        if (request.scope() == Scope.TEAM && request.teamId() == null) {
            throw new IllegalArgumentException("teamId is required for TEAM scope personas");
        }
        if (request.scope() == Scope.TEAM) {
            verifyTeamAdmin(request.teamId());
        }
        if (request.teamId() != null) {
            long count = personaRepository.findByTeamId(request.teamId()).size();
            if (count >= AppConstants.MAX_PERSONAS_PER_TEAM) {
                throw new IllegalArgumentException("Team has reached the maximum number of personas");
            }
        }

        Persona persona = Persona.builder()
                .name(request.name())
                .agentType(request.agentType())
                .description(request.description())
                .contentMd(request.contentMd())
                .scope(request.scope())
                .team(request.teamId() != null ? teamRepository.findById(request.teamId()).orElseThrow(() -> new EntityNotFoundException("Team not found")) : null)
                .createdBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")))
                .isDefault(request.isDefault() != null ? request.isDefault() : false)
                .version(1)
                .build();

        if (Boolean.TRUE.equals(persona.getIsDefault()) && persona.getAgentType() != null && request.teamId() != null) {
            clearExistingDefault(request.teamId(), persona.getAgentType());
        }

        persona = personaRepository.save(persona);
        return mapToResponse(persona);
    }

    /**
     * Retrieves a single persona by its ID.
     *
     * <p>For team-scoped personas, verifies the current user is a member of the team.</p>
     *
     * @param personaId the ID of the persona to retrieve
     * @return the persona as a response DTO
     * @throws EntityNotFoundException if no persona exists with the given ID
     * @throws AccessDeniedException if the persona is team-scoped and the current user is not a team member
     */
    @Transactional(readOnly = true)
    public PersonaResponse getPersona(UUID personaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new EntityNotFoundException("Persona not found"));
        if (persona.getTeam() != null) {
            verifyTeamMembership(persona.getTeam().getId());
        }
        return mapToResponse(persona);
    }

    /**
     * Retrieves a paginated list of personas belonging to a specific team.
     *
     * @param teamId the ID of the team whose personas to retrieve
     * @param pageable pagination and sorting parameters
     * @return a paginated response of persona DTOs
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public PageResponse<PersonaResponse> getPersonasForTeam(UUID teamId, Pageable pageable) {
        verifyTeamMembership(teamId);
        Page<Persona> page = personaRepository.findByTeamId(teamId, pageable);
        List<PersonaResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Retrieves all personas for a team filtered by agent type.
     *
     * @param teamId the ID of the team whose personas to retrieve
     * @param agentType the agent type to filter by
     * @return a list of persona response DTOs matching the specified agent type
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public List<PersonaResponse> getPersonasByAgentType(UUID teamId, AgentType agentType) {
        verifyTeamMembership(teamId);
        return personaRepository.findByTeamIdAndAgentType(teamId, agentType).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves the default persona for a given team and agent type combination.
     *
     * @param teamId the ID of the team whose default persona to retrieve
     * @param agentType the agent type whose default persona to retrieve
     * @return the default persona as a response DTO
     * @throws EntityNotFoundException if no default persona is configured for the specified agent type
     */
    @Transactional(readOnly = true)
    public PersonaResponse getDefaultPersona(UUID teamId, AgentType agentType) {
        return personaRepository.findByTeamIdAndAgentTypeAndIsDefaultTrue(teamId, agentType)
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("No default persona found for agent type: " + agentType));
    }

    /**
     * Retrieves all personas created by a specific user.
     *
     * @param userId the ID of the user whose personas to retrieve
     * @return a list of persona response DTOs created by the specified user
     */
    @Transactional(readOnly = true)
    public List<PersonaResponse> getPersonasByUser(UUID userId) {
        return personaRepository.findByCreatedById(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves all built-in system personas.
     *
     * <p>System personas are read-only and available to all users regardless
     * of team membership.</p>
     *
     * @return a list of system-scoped persona response DTOs
     */
    @Transactional(readOnly = true)
    public List<PersonaResponse> getSystemPersonas() {
        return personaRepository.findByScope(Scope.SYSTEM).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Updates an existing persona's mutable fields.
     *
     * <p>Only non-null fields in the request are applied. Updating the content
     * markdown increments the persona version. Setting isDefault to {@code true}
     * clears the existing default for the same team and agent type combination.
     * SYSTEM personas cannot be modified.</p>
     *
     * @param personaId the ID of the persona to update
     * @param request the update request containing optional name, description,
     *                content markdown, and default flag
     * @return the updated persona as a response DTO
     * @throws EntityNotFoundException if no persona exists with the given ID
     * @throws IllegalArgumentException if the persona has SYSTEM scope
     * @throws AccessDeniedException if the current user is neither the creator nor a team admin/owner
     */
    public PersonaResponse updatePersona(UUID personaId, UpdatePersonaRequest request) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new EntityNotFoundException("Persona not found"));
        verifyCreatorOrTeamAdmin(persona);
        if (persona.getScope() == Scope.SYSTEM) {
            throw new IllegalArgumentException("Cannot modify SYSTEM personas");
        }

        if (request.name() != null) persona.setName(request.name());
        if (request.description() != null) persona.setDescription(request.description());
        if (request.contentMd() != null) {
            persona.setContentMd(request.contentMd());
            persona.setVersion(persona.getVersion() + 1);
        }
        if (request.isDefault() != null) {
            persona.setIsDefault(request.isDefault());
            if (Boolean.TRUE.equals(request.isDefault()) && persona.getAgentType() != null && persona.getTeam() != null) {
                clearExistingDefault(persona.getTeam().getId(), persona.getAgentType());
            }
        }

        persona = personaRepository.save(persona);
        return mapToResponse(persona);
    }

    /**
     * Permanently deletes a persona.
     *
     * <p>SYSTEM personas cannot be deleted.</p>
     *
     * @param personaId the ID of the persona to delete
     * @throws EntityNotFoundException if no persona exists with the given ID
     * @throws IllegalArgumentException if the persona has SYSTEM scope
     * @throws AccessDeniedException if the current user is neither the creator nor a team admin/owner
     */
    public void deletePersona(UUID personaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new EntityNotFoundException("Persona not found"));
        verifyCreatorOrTeamAdmin(persona);
        if (persona.getScope() == Scope.SYSTEM) {
            throw new IllegalArgumentException("Cannot delete SYSTEM personas");
        }
        personaRepository.delete(persona);
    }

    /**
     * Designates a persona as the default for its agent type within its team.
     *
     * <p>Clears any existing default persona for the same team and agent type
     * combination before setting the new default. The persona must have both
     * an agent type and a team assigned.</p>
     *
     * @param personaId the ID of the persona to set as default
     * @return the updated persona as a response DTO with isDefault set to {@code true}
     * @throws EntityNotFoundException if no persona exists with the given ID
     * @throws IllegalArgumentException if the persona lacks an agent type or team
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the persona's team
     */
    public PersonaResponse setAsDefault(UUID personaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new EntityNotFoundException("Persona not found"));
        if (persona.getAgentType() == null || persona.getTeam() == null) {
            throw new IllegalArgumentException("Persona must have agentType and team to be set as default");
        }
        verifyTeamAdmin(persona.getTeam().getId());
        clearExistingDefault(persona.getTeam().getId(), persona.getAgentType());
        persona.setIsDefault(true);
        persona = personaRepository.save(persona);
        return mapToResponse(persona);
    }

    /**
     * Removes the default designation from a persona.
     *
     * @param personaId the ID of the persona to remove default status from
     * @return the updated persona as a response DTO with isDefault set to {@code false}
     * @throws EntityNotFoundException if no persona exists with the given ID
     * @throws AccessDeniedException if the persona is team-scoped and the current user does not have OWNER or ADMIN role
     */
    public PersonaResponse removeDefault(UUID personaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new EntityNotFoundException("Persona not found"));
        if (persona.getTeam() != null) {
            verifyTeamAdmin(persona.getTeam().getId());
        }
        persona.setIsDefault(false);
        persona = personaRepository.save(persona);
        return mapToResponse(persona);
    }

    private void clearExistingDefault(UUID teamId, AgentType agentType) {
        personaRepository.findByTeamIdAndAgentTypeAndIsDefaultTrue(teamId, agentType)
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    personaRepository.save(existing);
                });
    }

    private PersonaResponse mapToResponse(Persona persona) {
        return new PersonaResponse(
                persona.getId(),
                persona.getName(),
                persona.getAgentType(),
                persona.getDescription(),
                persona.getContentMd(),
                persona.getScope(),
                persona.getTeam() != null ? persona.getTeam().getId() : null,
                persona.getCreatedBy().getId(),
                persona.getCreatedBy().getDisplayName(),
                persona.getIsDefault(),
                persona.getVersion(),
                persona.getCreatedAt(),
                persona.getUpdatedAt()
        );
    }

    private void verifyCreatorOrTeamAdmin(Persona persona) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (persona.getCreatedBy().getId().equals(currentUserId)) return;
        if (persona.getTeam() != null) {
            var member = teamMemberRepository.findByTeamIdAndUserId(persona.getTeam().getId(), currentUserId);
            if (member.isPresent() && (member.get().getRole() == TeamRole.OWNER || member.get().getRole() == TeamRole.ADMIN)) {
                return;
            }
        }
        throw new AccessDeniedException("Not authorized to modify this persona");
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

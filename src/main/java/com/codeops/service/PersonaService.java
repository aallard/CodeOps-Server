package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreatePersonaRequest;
import com.codeops.dto.request.UpdatePersonaRequest;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

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
                .team(request.teamId() != null ? teamRepository.getReferenceById(request.teamId()) : null)
                .createdBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()))
                .isDefault(request.isDefault() != null ? request.isDefault() : false)
                .version(1)
                .build();

        if (Boolean.TRUE.equals(persona.getIsDefault()) && persona.getAgentType() != null && request.teamId() != null) {
            clearExistingDefault(request.teamId(), persona.getAgentType());
        }

        persona = personaRepository.save(persona);
        return mapToResponse(persona);
    }

    @Transactional(readOnly = true)
    public PersonaResponse getPersona(UUID personaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new EntityNotFoundException("Persona not found"));
        if (persona.getTeam() != null) {
            verifyTeamMembership(persona.getTeam().getId());
        }
        return mapToResponse(persona);
    }

    @Transactional(readOnly = true)
    public List<PersonaResponse> getPersonasForTeam(UUID teamId) {
        verifyTeamMembership(teamId);
        return personaRepository.findByTeamId(teamId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PersonaResponse> getPersonasByAgentType(UUID teamId, AgentType agentType) {
        verifyTeamMembership(teamId);
        return personaRepository.findByTeamIdAndAgentType(teamId, agentType).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonaResponse getDefaultPersona(UUID teamId, AgentType agentType) {
        return personaRepository.findByTeamIdAndAgentTypeAndIsDefaultTrue(teamId, agentType)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PersonaResponse> getPersonasByUser(UUID userId) {
        return personaRepository.findByCreatedById(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PersonaResponse> getSystemPersonas() {
        return personaRepository.findByScope(Scope.SYSTEM).stream()
                .map(this::mapToResponse)
                .toList();
    }

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

    public void deletePersona(UUID personaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new EntityNotFoundException("Persona not found"));
        verifyCreatorOrTeamAdmin(persona);
        if (persona.getScope() == Scope.SYSTEM) {
            throw new IllegalArgumentException("Cannot delete SYSTEM personas");
        }
        personaRepository.delete(persona);
    }

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

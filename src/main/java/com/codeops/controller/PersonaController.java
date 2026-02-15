package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreatePersonaRequest;
import com.codeops.dto.request.UpdatePersonaRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.PersonaResponse;
import com.codeops.entity.enums.AgentType;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.PersonaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for persona management operations.
 *
 * <p>Personas define the behavior and tone for AI agents. They can be system-level
 * (read-only built-in), team-level (shared), or user-level (personal). All endpoints
 * require authentication. Authorization is enforced at the service layer based on
 * team membership and persona ownership.</p>
 *
 * <p>Mutating operations (create, update, delete, set/remove default) record an
 * audit log entry via {@link AuditLogService}.</p>
 *
 * @see PersonaService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
@Tag(name = "Personas")
public class PersonaController {

    private static final Logger log = LoggerFactory.getLogger(PersonaController.class);

    private final PersonaService personaService;
    private final AuditLogService auditLogService;

    /**
     * Creates a new persona.
     *
     * <p>POST /api/v1/personas</p>
     *
     * <p>Requires authentication. Logs a PERSONA_CREATED audit event.</p>
     *
     * @param request the persona creation payload including name, agent type, and configuration
     * @return the created persona with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> createPersona(@Valid @RequestBody CreatePersonaRequest request) {
        log.debug("createPersona called");
        PersonaResponse response = personaService.createPersona(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PERSONA_CREATED", "PERSONA", response.id(), "");
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves a persona by its identifier.
     *
     * <p>GET /api/v1/personas/{personaId}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param personaId the UUID of the persona to retrieve
     * @return the persona details
     */
    @GetMapping("/{personaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> getPersona(@PathVariable UUID personaId) {
        log.debug("getPersona called with personaId={}", personaId);
        return ResponseEntity.ok(personaService.getPersona(personaId));
    }

    /**
     * Retrieves a paginated list of personas belonging to a specific team.
     *
     * <p>GET /api/v1/personas/team/{teamId}?page={page}&amp;size={size}</p>
     *
     * <p>Requires authentication. Results are sorted by creation date descending.
     * Page size is capped at {@link AppConstants#MAX_PAGE_SIZE}.</p>
     *
     * @param teamId the UUID of the team whose personas to list
     * @param page   zero-based page index (defaults to 0)
     * @param size   number of items per page (defaults to 20, capped at MAX_PAGE_SIZE)
     * @return a paginated list of personas for the team
     */
    @GetMapping("/team/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<PersonaResponse>> getPersonasForTeam(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("getPersonasForTeam called with teamId={}, page={}, size={}", teamId, page, size);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(personaService.getPersonasForTeam(teamId, pageable));
    }

    /**
     * Retrieves all personas for a team filtered by agent type.
     *
     * <p>GET /api/v1/personas/team/{teamId}/agent/{agentType}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param teamId    the UUID of the team
     * @param agentType the agent type to filter by (e.g., CODE_REVIEW, SECURITY)
     * @return a list of personas matching the specified agent type within the team
     */
    @GetMapping("/team/{teamId}/agent/{agentType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PersonaResponse>> getPersonasByAgentType(@PathVariable UUID teamId,
                                                                        @PathVariable AgentType agentType) {
        log.debug("getPersonasByAgentType called with teamId={}, agentType={}", teamId, agentType);
        return ResponseEntity.ok(personaService.getPersonasByAgentType(teamId, agentType));
    }

    /**
     * Retrieves the default persona for a given team and agent type.
     *
     * <p>GET /api/v1/personas/team/{teamId}/default/{agentType}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param teamId    the UUID of the team
     * @param agentType the agent type whose default persona to retrieve
     * @return the default persona for the specified team and agent type
     */
    @GetMapping("/team/{teamId}/default/{agentType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> getDefaultPersona(@PathVariable UUID teamId,
                                                              @PathVariable AgentType agentType) {
        log.debug("getDefaultPersona called with teamId={}, agentType={}", teamId, agentType);
        return ResponseEntity.ok(personaService.getDefaultPersona(teamId, agentType));
    }

    /**
     * Retrieves all personas owned by the currently authenticated user.
     *
     * <p>GET /api/v1/personas/mine</p>
     *
     * <p>Requires authentication.</p>
     *
     * @return a list of personas belonging to the current user
     */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PersonaResponse>> getMyPersonas() {
        log.debug("getMyPersonas called");
        return ResponseEntity.ok(personaService.getPersonasByUser(SecurityUtils.getCurrentUserId()));
    }

    /**
     * Retrieves all system-level built-in personas.
     *
     * <p>GET /api/v1/personas/system</p>
     *
     * <p>Requires authentication. System personas are read-only and shared
     * across all teams.</p>
     *
     * @return a list of system personas
     */
    @GetMapping("/system")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PersonaResponse>> getSystemPersonas() {
        log.debug("getSystemPersonas called");
        return ResponseEntity.ok(personaService.getSystemPersonas());
    }

    /**
     * Updates an existing persona.
     *
     * <p>PUT /api/v1/personas/{personaId}</p>
     *
     * <p>Requires authentication. Logs a PERSONA_UPDATED audit event.</p>
     *
     * @param personaId the UUID of the persona to update
     * @param request   the update payload with fields to modify
     * @return the updated persona
     */
    @PutMapping("/{personaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> updatePersona(@PathVariable UUID personaId,
                                                          @Valid @RequestBody UpdatePersonaRequest request) {
        log.debug("updatePersona called with personaId={}", personaId);
        PersonaResponse response = personaService.updatePersona(personaId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PERSONA_UPDATED", "PERSONA", personaId, "");
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a persona by its identifier.
     *
     * <p>DELETE /api/v1/personas/{personaId}</p>
     *
     * <p>Requires authentication. Logs a PERSONA_DELETED audit event.
     * Returns HTTP 204 No Content on success.</p>
     *
     * @param personaId the UUID of the persona to delete
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{personaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePersona(@PathVariable UUID personaId) {
        log.debug("deletePersona called with personaId={}", personaId);
        PersonaResponse persona = personaService.getPersona(personaId);
        personaService.deletePersona(personaId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), persona.teamId(), "PERSONA_DELETED", "PERSONA", personaId, "");
        return ResponseEntity.noContent().build();
    }

    /**
     * Sets a persona as the default for its team and agent type.
     *
     * <p>PUT /api/v1/personas/{personaId}/set-default</p>
     *
     * <p>Requires authentication. Any previously default persona for the same
     * team and agent type combination will be unset. Logs a PERSONA_SET_DEFAULT
     * audit event.</p>
     *
     * @param personaId the UUID of the persona to set as default
     * @return the updated persona with its default flag set to true
     */
    @PutMapping("/{personaId}/set-default")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> setAsDefault(@PathVariable UUID personaId) {
        log.debug("setAsDefault called with personaId={}", personaId);
        PersonaResponse response = personaService.setAsDefault(personaId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PERSONA_SET_DEFAULT", "PERSONA", personaId, "");
        return ResponseEntity.ok(response);
    }

    /**
     * Removes the default designation from a persona.
     *
     * <p>PUT /api/v1/personas/{personaId}/remove-default</p>
     *
     * <p>Requires authentication. Logs a PERSONA_REMOVED_DEFAULT audit event.</p>
     *
     * @param personaId the UUID of the persona to remove the default flag from
     * @return the updated persona with its default flag set to false
     */
    @PutMapping("/{personaId}/remove-default")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> removeDefault(@PathVariable UUID personaId) {
        log.debug("removeDefault called with personaId={}", personaId);
        PersonaResponse response = personaService.removeDefault(personaId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PERSONA_REMOVED_DEFAULT", "PERSONA", personaId, "");
        return ResponseEntity.ok(response);
    }
}

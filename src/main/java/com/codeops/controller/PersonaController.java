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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
@Tag(name = "Personas")
public class PersonaController {

    private final PersonaService personaService;
    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> createPersona(@Valid @RequestBody CreatePersonaRequest request) {
        PersonaResponse response = personaService.createPersona(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PERSONA_CREATED", "PERSONA", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{personaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> getPersona(@PathVariable UUID personaId) {
        return ResponseEntity.ok(personaService.getPersona(personaId));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<PersonaResponse>> getPersonasForTeam(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(personaService.getPersonasForTeam(teamId, pageable));
    }

    @GetMapping("/team/{teamId}/agent/{agentType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PersonaResponse>> getPersonasByAgentType(@PathVariable UUID teamId,
                                                                        @PathVariable AgentType agentType) {
        return ResponseEntity.ok(personaService.getPersonasByAgentType(teamId, agentType));
    }

    @GetMapping("/team/{teamId}/default/{agentType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> getDefaultPersona(@PathVariable UUID teamId,
                                                              @PathVariable AgentType agentType) {
        PersonaResponse response = personaService.getDefaultPersona(teamId, agentType);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PersonaResponse>> getMyPersonas() {
        return ResponseEntity.ok(personaService.getPersonasByUser(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/system")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PersonaResponse>> getSystemPersonas() {
        return ResponseEntity.ok(personaService.getSystemPersonas());
    }

    @PutMapping("/{personaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> updatePersona(@PathVariable UUID personaId,
                                                          @Valid @RequestBody UpdatePersonaRequest request) {
        PersonaResponse response = personaService.updatePersona(personaId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PERSONA_UPDATED", "PERSONA", personaId, null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{personaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePersona(@PathVariable UUID personaId) {
        PersonaResponse persona = personaService.getPersona(personaId);
        personaService.deletePersona(personaId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), persona.teamId(), "PERSONA_DELETED", "PERSONA", personaId, null);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{personaId}/set-default")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> setAsDefault(@PathVariable UUID personaId) {
        PersonaResponse response = personaService.setAsDefault(personaId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PERSONA_SET_DEFAULT", "PERSONA", personaId, null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{personaId}/remove-default")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonaResponse> removeDefault(@PathVariable UUID personaId) {
        PersonaResponse response = personaService.removeDefault(personaId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PERSONA_REMOVED_DEFAULT", "PERSONA", personaId, null);
        return ResponseEntity.ok(response);
    }
}

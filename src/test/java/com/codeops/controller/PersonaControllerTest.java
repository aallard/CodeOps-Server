package com.codeops.controller;

import com.codeops.dto.request.CreatePersonaRequest;
import com.codeops.dto.request.UpdatePersonaRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.PersonaResponse;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.Scope;
import com.codeops.service.AuditLogService;
import com.codeops.service.PersonaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonaControllerTest {

    @Mock
    private PersonaService personaService;

    @Mock
    private AuditLogService auditLogService;

    private PersonaController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID personaId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new PersonaController(personaService, auditLogService);
        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PersonaResponse buildPersonaResponse(UUID id) {
        return new PersonaResponse(id, "Security Reviewer", AgentType.SECURITY,
                "A security-focused persona", "## Instructions\nReview for vulnerabilities",
                Scope.TEAM, teamId, userId, "Adam", true, 1, Instant.now(), Instant.now());
    }

    @Test
    void createPersona_returnsCreatedWithBody() {
        CreatePersonaRequest request = new CreatePersonaRequest("Security Reviewer", AgentType.SECURITY,
                "A security-focused persona", "## Instructions\nReview for vulnerabilities",
                Scope.TEAM, teamId, true);
        PersonaResponse response = buildPersonaResponse(personaId);
        when(personaService.createPersona(request)).thenReturn(response);

        ResponseEntity<PersonaResponse> result = controller.createPersona(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(personaService).createPersona(request);
        verify(auditLogService).log(userId, teamId, "PERSONA_CREATED", "PERSONA", personaId, "");
    }

    @Test
    void getPersona_returnsOkWithBody() {
        PersonaResponse response = buildPersonaResponse(personaId);
        when(personaService.getPersona(personaId)).thenReturn(response);

        ResponseEntity<PersonaResponse> result = controller.getPersona(personaId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(personaService).getPersona(personaId);
    }

    @Test
    void getPersonasForTeam_returnsOkWithPage() {
        PageResponse<PersonaResponse> page = new PageResponse<>(
                List.of(buildPersonaResponse(personaId)), 0, 20, 1, 1, true);
        when(personaService.getPersonasForTeam(eq(teamId), any())).thenReturn(page);

        ResponseEntity<PageResponse<PersonaResponse>> result = controller.getPersonasForTeam(teamId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(personaService).getPersonasForTeam(eq(teamId), any());
    }

    @Test
    void getPersonasByAgentType_returnsOkWithList() {
        List<PersonaResponse> responses = List.of(buildPersonaResponse(personaId));
        when(personaService.getPersonasByAgentType(teamId, AgentType.SECURITY)).thenReturn(responses);

        ResponseEntity<List<PersonaResponse>> result = controller.getPersonasByAgentType(teamId, AgentType.SECURITY);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(1);
        verify(personaService).getPersonasByAgentType(teamId, AgentType.SECURITY);
    }

    @Test
    void getDefaultPersona_returnsOkWithBody() {
        PersonaResponse response = buildPersonaResponse(personaId);
        when(personaService.getDefaultPersona(teamId, AgentType.SECURITY)).thenReturn(response);

        ResponseEntity<PersonaResponse> result = controller.getDefaultPersona(teamId, AgentType.SECURITY);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(personaService).getDefaultPersona(teamId, AgentType.SECURITY);
    }

    @Test
    void getMyPersonas_returnsOkWithList() {
        List<PersonaResponse> responses = List.of(buildPersonaResponse(personaId));
        when(personaService.getPersonasByUser(userId)).thenReturn(responses);

        ResponseEntity<List<PersonaResponse>> result = controller.getMyPersonas();

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(1);
        verify(personaService).getPersonasByUser(userId);
    }

    @Test
    void getSystemPersonas_returnsOkWithList() {
        List<PersonaResponse> responses = List.of(buildPersonaResponse(personaId));
        when(personaService.getSystemPersonas()).thenReturn(responses);

        ResponseEntity<List<PersonaResponse>> result = controller.getSystemPersonas();

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(1);
        verify(personaService).getSystemPersonas();
    }

    @Test
    void updatePersona_returnsOkWithBody() {
        UpdatePersonaRequest request = new UpdatePersonaRequest("Updated Name", "New desc", "## Updated", false);
        PersonaResponse response = buildPersonaResponse(personaId);
        when(personaService.updatePersona(personaId, request)).thenReturn(response);

        ResponseEntity<PersonaResponse> result = controller.updatePersona(personaId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(personaService).updatePersona(personaId, request);
        verify(auditLogService).log(userId, teamId, "PERSONA_UPDATED", "PERSONA", personaId, "");
    }

    @Test
    void deletePersona_returnsNoContent() {
        PersonaResponse personaBeforeDelete = buildPersonaResponse(personaId);
        when(personaService.getPersona(personaId)).thenReturn(personaBeforeDelete);

        ResponseEntity<Void> result = controller.deletePersona(personaId);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        assertThat(result.getBody()).isNull();
        verify(personaService).getPersona(personaId);
        verify(personaService).deletePersona(personaId);
        verify(auditLogService).log(userId, teamId, "PERSONA_DELETED", "PERSONA", personaId, "");
    }

    @Test
    void setAsDefault_returnsOkWithBody() {
        PersonaResponse response = buildPersonaResponse(personaId);
        when(personaService.setAsDefault(personaId)).thenReturn(response);

        ResponseEntity<PersonaResponse> result = controller.setAsDefault(personaId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(personaService).setAsDefault(personaId);
        verify(auditLogService).log(userId, teamId, "PERSONA_SET_DEFAULT", "PERSONA", personaId, "");
    }

    @Test
    void removeDefault_returnsOkWithBody() {
        PersonaResponse response = buildPersonaResponse(personaId);
        when(personaService.removeDefault(personaId)).thenReturn(response);

        ResponseEntity<PersonaResponse> result = controller.removeDefault(personaId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(personaService).removeDefault(personaId);
        verify(auditLogService).log(userId, teamId, "PERSONA_REMOVED_DEFAULT", "PERSONA", personaId, "");
    }
}

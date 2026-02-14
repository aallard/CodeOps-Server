package com.codeops.controller;

import com.codeops.dto.request.*;
import com.codeops.dto.response.*;
import com.codeops.entity.enums.*;
import com.codeops.service.AgentRunService;
import com.codeops.service.AuditLogService;
import com.codeops.service.BugInvestigationService;
import com.codeops.service.QaJobService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private QaJobService qaJobService;

    @Mock
    private AgentRunService agentRunService;

    @Mock
    private BugInvestigationService bugInvestigationService;

    @Mock
    private AuditLogService auditLogService;

    private JobController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new JobController(qaJobService, agentRunService, bugInvestigationService, auditLogService);
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

    private JobResponse buildJobResponse() {
        return new JobResponse(jobId, projectId, "TestProject", JobMode.AUDIT, JobStatus.COMPLETED,
                "Audit Run", "main", null, "## Summary", JobResult.PASS, 85,
                10, 1, 2, 3, 4, null, userId, "Adam", Instant.now(), Instant.now(), Instant.now());
    }

    private JobSummaryResponse buildJobSummaryResponse() {
        return new JobSummaryResponse(jobId, "TestProject", JobMode.AUDIT, JobStatus.COMPLETED,
                "Audit Run", JobResult.PASS, 85, 10, 1, Instant.now(), Instant.now());
    }

    private AgentRunResponse buildAgentRunResponse(UUID id) {
        return new AgentRunResponse(id, jobId, AgentType.SECURITY, AgentStatus.COMPLETED,
                AgentResult.PASS, "reports/key.md", 90, 5, 1, 2, Instant.now(), Instant.now());
    }

    private BugInvestigationResponse buildBugInvestigationResponse(UUID id) {
        return new BugInvestigationResponse(id, jobId, "PROJ-123", "Bug summary",
                "Bug description", "context", "# RCA", "# Impact", "rca/key.md",
                false, false, Instant.now());
    }

    @Test
    void createJob_returnsCreatedWithBody() {
        CreateJobRequest request = new CreateJobRequest(projectId, JobMode.AUDIT, "Audit Run", "main", null, null);
        JobResponse response = buildJobResponse();
        when(qaJobService.createJob(request)).thenReturn(response);

        ResponseEntity<JobResponse> result = controller.createJob(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(qaJobService).createJob(request);
        verify(auditLogService).log(userId, null, "JOB_CREATED", "JOB", jobId, null);
    }

    @Test
    void getJob_returnsOkWithBody() {
        JobResponse response = buildJobResponse();
        when(qaJobService.getJob(jobId)).thenReturn(response);

        ResponseEntity<JobResponse> result = controller.getJob(jobId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(qaJobService).getJob(jobId);
    }

    @Test
    void getJobsForProject_returnsOkWithPage() {
        PageResponse<JobSummaryResponse> page = new PageResponse<>(
                List.of(buildJobSummaryResponse()), 0, 20, 1, 1, true);
        when(qaJobService.getJobsForProject(eq(projectId), any())).thenReturn(page);

        ResponseEntity<PageResponse<JobSummaryResponse>> result = controller.getJobsForProject(projectId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(qaJobService).getJobsForProject(eq(projectId), any());
    }

    @Test
    void getMyJobs_returnsOkWithPage() {
        PageResponse<JobSummaryResponse> page = new PageResponse<>(
                List.of(buildJobSummaryResponse()), 0, 20, 1, 1, true);
        when(qaJobService.getJobsByUser(eq(userId), any())).thenReturn(page);

        ResponseEntity<PageResponse<JobSummaryResponse>> result = controller.getMyJobs(0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(qaJobService).getJobsByUser(eq(userId), any());
    }

    @Test
    void updateJob_returnsOkWithBody() {
        UpdateJobRequest request = new UpdateJobRequest(JobStatus.COMPLETED, "## Summary",
                JobResult.PASS, 85, 10, 1, 2, 3, 4, Instant.now(), Instant.now());
        JobResponse response = buildJobResponse();
        when(qaJobService.updateJob(jobId, request)).thenReturn(response);

        ResponseEntity<JobResponse> result = controller.updateJob(jobId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(qaJobService).updateJob(jobId, request);
        verify(auditLogService).log(userId, null, "JOB_UPDATED", "JOB", jobId, null);
    }

    @Test
    void deleteJob_returnsNoContent() {
        ResponseEntity<Void> result = controller.deleteJob(jobId);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        assertThat(result.getBody()).isNull();
        verify(qaJobService).deleteJob(jobId);
        verify(auditLogService).log(userId, null, "JOB_DELETED", "JOB", jobId, null);
    }

    @Test
    void createAgentRun_returnsCreatedWithBody() {
        UUID agentRunId = UUID.randomUUID();
        CreateAgentRunRequest request = new CreateAgentRunRequest(jobId, AgentType.SECURITY);
        AgentRunResponse response = buildAgentRunResponse(agentRunId);
        when(agentRunService.createAgentRun(request)).thenReturn(response);

        ResponseEntity<AgentRunResponse> result = controller.createAgentRun(jobId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(agentRunService).createAgentRun(request);
        verify(auditLogService).log(userId, null, "AGENT_RUN_CREATED", "AGENT_RUN", agentRunId, null);
    }

    @Test
    void createAgentRunsBatch_returnsCreatedWithList() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<AgentType> agentTypes = List.of(AgentType.SECURITY, AgentType.CODE_QUALITY);
        List<AgentRunResponse> responses = List.of(buildAgentRunResponse(id1), buildAgentRunResponse(id2));
        when(agentRunService.createAgentRuns(jobId, agentTypes)).thenReturn(responses);

        ResponseEntity<List<AgentRunResponse>> result = controller.createAgentRunsBatch(jobId, agentTypes);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).hasSize(2);
        verify(agentRunService).createAgentRuns(jobId, agentTypes);
        verify(auditLogService, times(2)).log(eq(userId), isNull(), eq("AGENT_RUN_CREATED"),
                eq("AGENT_RUN"), any(UUID.class), isNull());
    }

    @Test
    void getAgentRuns_returnsOkWithList() {
        UUID agentRunId = UUID.randomUUID();
        List<AgentRunResponse> responses = List.of(buildAgentRunResponse(agentRunId));
        when(agentRunService.getAgentRuns(jobId)).thenReturn(responses);

        ResponseEntity<List<AgentRunResponse>> result = controller.getAgentRuns(jobId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(1);
        verify(agentRunService).getAgentRuns(jobId);
    }

    @Test
    void updateAgentRun_returnsOkWithBody() {
        UUID agentRunId = UUID.randomUUID();
        UpdateAgentRunRequest request = new UpdateAgentRunRequest(AgentStatus.COMPLETED, AgentResult.PASS,
                "reports/key.md", 90, 5, 1, 2, Instant.now(), Instant.now());
        AgentRunResponse response = buildAgentRunResponse(agentRunId);
        when(agentRunService.updateAgentRun(agentRunId, request)).thenReturn(response);

        ResponseEntity<AgentRunResponse> result = controller.updateAgentRun(agentRunId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(agentRunService).updateAgentRun(agentRunId, request);
        verify(auditLogService).log(userId, null, "AGENT_RUN_UPDATED", "AGENT_RUN", agentRunId, null);
    }

    @Test
    void getInvestigation_returnsOkWithBody() {
        UUID investigationId = UUID.randomUUID();
        BugInvestigationResponse response = buildBugInvestigationResponse(investigationId);
        when(bugInvestigationService.getInvestigationByJob(jobId)).thenReturn(response);

        ResponseEntity<BugInvestigationResponse> result = controller.getInvestigation(jobId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(bugInvestigationService).getInvestigationByJob(jobId);
    }

    @Test
    void createInvestigation_returnsCreatedWithBody() {
        UUID investigationId = UUID.randomUUID();
        CreateBugInvestigationRequest request = new CreateBugInvestigationRequest(jobId, "PROJ-123",
                "Bug summary", "Bug description", null, null, null, "context");
        BugInvestigationResponse response = buildBugInvestigationResponse(investigationId);
        when(bugInvestigationService.createInvestigation(request)).thenReturn(response);

        ResponseEntity<BugInvestigationResponse> result = controller.createInvestigation(jobId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(bugInvestigationService).createInvestigation(request);
        verify(auditLogService).log(userId, null, "INVESTIGATION_CREATED", "BUG_INVESTIGATION", investigationId, null);
    }

    @Test
    void updateInvestigation_returnsOkWithBody() {
        UUID investigationId = UUID.randomUUID();
        UpdateBugInvestigationRequest request = new UpdateBugInvestigationRequest("# RCA", "# Impact",
                "rca/key.md", true, false);
        BugInvestigationResponse response = buildBugInvestigationResponse(investigationId);
        when(bugInvestigationService.updateInvestigation(investigationId, request)).thenReturn(response);

        ResponseEntity<BugInvestigationResponse> result = controller.updateInvestigation(investigationId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(bugInvestigationService).updateInvestigation(investigationId, request);
        verify(auditLogService).log(userId, null, "INVESTIGATION_UPDATED", "BUG_INVESTIGATION", investigationId, null);
    }
}

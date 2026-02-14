package com.codeops.service;

import com.codeops.dto.request.CreateAgentRunRequest;
import com.codeops.dto.request.UpdateAgentRunRequest;
import com.codeops.dto.response.AgentRunResponse;
import com.codeops.entity.AgentRun;
import com.codeops.entity.enums.AgentStatus;
import com.codeops.entity.enums.AgentType;
import com.codeops.repository.AgentRunRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages the lifecycle of agent runs within QA jobs, including creation, retrieval, and status updates.
 *
 * <p>An agent run represents a single execution of a specific {@link AgentType} analysis agent
 * against a QA job. All operations verify that the current user is a member of the team
 * that owns the associated project.</p>
 *
 * @see AgentRunRepository
 * @see AgentRun
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AgentRunService {

    private final AgentRunRepository agentRunRepository;
    private final QaJobRepository qaJobRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Creates a single agent run for a QA job with initial status {@link AgentStatus#PENDING}
     * and all finding counters set to zero.
     *
     * @param request the creation request containing the job ID and agent type
     * @return the newly created agent run as a response DTO
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public AgentRunResponse createAgentRun(CreateAgentRunRequest request) {
        var job = qaJobRepository.findById(request.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        AgentRun run = AgentRun.builder()
                .job(job)
                .agentType(request.agentType())
                .status(AgentStatus.PENDING)
                .findingsCount(0)
                .criticalCount(0)
                .highCount(0)
                .build();

        run = agentRunRepository.save(run);
        return mapToResponse(run);
    }

    /**
     * Creates multiple agent runs for a QA job in a single batch, one per specified agent type.
     * Each run is initialized with {@link AgentStatus#PENDING} status and zero finding counters.
     *
     * @param jobId      the UUID of the QA job to associate the agent runs with
     * @param agentTypes the list of agent types to create runs for
     * @return a list of the newly created agent runs as response DTOs
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public List<AgentRunResponse> createAgentRuns(UUID jobId, List<AgentType> agentTypes) {
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        List<AgentRun> runs = agentTypes.stream()
                .map(agentType -> AgentRun.builder()
                        .job(job)
                        .agentType(agentType)
                        .status(AgentStatus.PENDING)
                        .findingsCount(0)
                        .criticalCount(0)
                        .highCount(0)
                        .build())
                .toList();

        runs = agentRunRepository.saveAll(runs);
        return runs.stream().map(this::mapToResponse).toList();
    }

    /**
     * Retrieves all agent runs associated with a given QA job.
     *
     * @param jobId the UUID of the QA job to retrieve runs for
     * @return a list of agent run response DTOs for the specified job
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public List<AgentRunResponse> getAgentRuns(UUID jobId) {
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        return agentRunRepository.findByJobId(jobId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves a single agent run by its unique identifier.
     *
     * @param agentRunId the UUID of the agent run to retrieve
     * @return the agent run as a response DTO
     * @throws EntityNotFoundException if no agent run exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    @Transactional(readOnly = true)
    public AgentRunResponse getAgentRun(UUID agentRunId) {
        AgentRun run = agentRunRepository.findById(agentRunId)
                .orElseThrow(() -> new EntityNotFoundException("Agent run not found"));
        verifyTeamMembership(run.getJob().getProject().getTeam().getId());
        return mapToResponse(run);
    }

    /**
     * Partially updates an agent run with the non-null fields from the request.
     *
     * <p>Supports updating status, result, report S3 key, score, finding counts,
     * and completion timestamp. The {@code startedAt} field is only set if it has not
     * been previously assigned (write-once semantics).</p>
     *
     * @param agentRunId the UUID of the agent run to update
     * @param request    the update request containing fields to modify (null fields are skipped)
     * @return the updated agent run as a response DTO
     * @throws EntityNotFoundException if no agent run exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    public AgentRunResponse updateAgentRun(UUID agentRunId, UpdateAgentRunRequest request) {
        AgentRun run = agentRunRepository.findById(agentRunId)
                .orElseThrow(() -> new EntityNotFoundException("Agent run not found"));
        verifyTeamMembership(run.getJob().getProject().getTeam().getId());

        if (request.status() != null) run.setStatus(request.status());
        if (request.result() != null) run.setResult(request.result());
        if (request.reportS3Key() != null) run.setReportS3Key(request.reportS3Key());
        if (request.score() != null) run.setScore(request.score());
        if (request.findingsCount() != null) run.setFindingsCount(request.findingsCount());
        if (request.criticalCount() != null) run.setCriticalCount(request.criticalCount());
        if (request.highCount() != null) run.setHighCount(request.highCount());
        if (request.completedAt() != null) run.setCompletedAt(request.completedAt());
        if (request.startedAt() != null && run.getStartedAt() == null) {
            run.setStartedAt(request.startedAt());
        }

        run = agentRunRepository.save(run);
        return mapToResponse(run);
    }

    private AgentRunResponse mapToResponse(AgentRun run) {
        return new AgentRunResponse(
                run.getId(),
                run.getJob().getId(),
                run.getAgentType(),
                run.getStatus(),
                run.getResult(),
                run.getReportS3Key(),
                run.getScore(),
                run.getFindingsCount(),
                run.getCriticalCount(),
                run.getHighCount(),
                run.getStartedAt(),
                run.getCompletedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }
}

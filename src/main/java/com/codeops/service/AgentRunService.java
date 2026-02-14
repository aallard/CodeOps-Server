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

@Service
@RequiredArgsConstructor
@Transactional
public class AgentRunService {

    private final AgentRunRepository agentRunRepository;
    private final QaJobRepository qaJobRepository;
    private final TeamMemberRepository teamMemberRepository;

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

    @Transactional(readOnly = true)
    public List<AgentRunResponse> getAgentRuns(UUID jobId) {
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        return agentRunRepository.findByJobId(jobId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentRunResponse getAgentRun(UUID agentRunId) {
        AgentRun run = agentRunRepository.findById(agentRunId)
                .orElseThrow(() -> new EntityNotFoundException("Agent run not found"));
        verifyTeamMembership(run.getJob().getProject().getTeam().getId());
        return mapToResponse(run);
    }

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

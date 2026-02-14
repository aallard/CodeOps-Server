package com.codeops.repository;

import com.codeops.entity.AgentRun;
import com.codeops.entity.enums.AgentStatus;
import com.codeops.entity.enums.AgentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    List<AgentRun> findByJobId(UUID jobId);

    List<AgentRun> findByJobIdAndStatus(UUID jobId, AgentStatus status);

    Optional<AgentRun> findByJobIdAndAgentType(UUID jobId, AgentType agentType);
}

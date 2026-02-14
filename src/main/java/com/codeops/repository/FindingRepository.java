package com.codeops.repository;

import com.codeops.entity.Finding;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.FindingStatus;
import com.codeops.entity.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FindingRepository extends JpaRepository<Finding, UUID> {

    List<Finding> findByJobId(UUID jobId);

    List<Finding> findByJobIdAndAgentType(UUID jobId, AgentType agentType);

    List<Finding> findByJobIdAndSeverity(UUID jobId, Severity severity);

    List<Finding> findByJobIdAndStatus(UUID jobId, FindingStatus status);

    Page<Finding> findByJobId(UUID jobId, Pageable pageable);

    Page<Finding> findByJobIdAndSeverity(UUID jobId, Severity severity, Pageable pageable);

    Page<Finding> findByJobIdAndAgentType(UUID jobId, AgentType agentType, Pageable pageable);

    Page<Finding> findByJobIdAndStatus(UUID jobId, FindingStatus status, Pageable pageable);

    long countByJobIdAndSeverity(UUID jobId, Severity severity);

    long countByJobIdAndSeverityAndStatus(UUID jobId, Severity severity, FindingStatus status);
}

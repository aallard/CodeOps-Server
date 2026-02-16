package com.codeops.repository;

import com.codeops.entity.AgentRun;
import com.codeops.entity.enums.AgentStatus;
import com.codeops.entity.enums.AgentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    List<AgentRun> findByJobId(UUID jobId);

    List<AgentRun> findByJobIdAndStatus(UUID jobId, AgentStatus status);

    Optional<AgentRun> findByJobIdAndAgentType(UUID jobId, AgentType agentType);

    /**
     * Bulk-deletes all agent runs for jobs belonging to the given project.
     *
     * @param projectId the project whose agent runs to remove
     */
    @Modifying
    @Query("DELETE FROM AgentRun a WHERE a.job.id IN "
            + "(SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

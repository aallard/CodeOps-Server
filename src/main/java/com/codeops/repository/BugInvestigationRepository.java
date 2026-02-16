package com.codeops.repository;

import com.codeops.entity.BugInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BugInvestigationRepository extends JpaRepository<BugInvestigation, UUID> {

    Optional<BugInvestigation> findByJobId(UUID jobId);

    Optional<BugInvestigation> findByJiraKey(String jiraKey);

    /**
     * Bulk-deletes all bug investigations for jobs belonging to the given project.
     *
     * @param projectId the project whose bug investigations to remove
     */
    @Modifying
    @Query("DELETE FROM BugInvestigation b WHERE b.job.id IN "
            + "(SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

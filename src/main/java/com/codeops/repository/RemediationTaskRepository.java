package com.codeops.repository;

import com.codeops.entity.RemediationTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RemediationTaskRepository extends JpaRepository<RemediationTask, UUID> {

    List<RemediationTask> findByJobIdOrderByTaskNumberAsc(UUID jobId);

    Page<RemediationTask> findByJobId(UUID jobId, Pageable pageable);

    List<RemediationTask> findByAssignedToId(UUID userId);

    Page<RemediationTask> findByAssignedToId(UUID userId, Pageable pageable);

    /**
     * Deletes all rows from the {@code remediation_task_findings} join table
     * for tasks belonging to jobs in the given project.
     *
     * @param projectId the project whose join-table rows to remove
     */
    @Modifying
    @Query(value = "DELETE FROM remediation_task_findings WHERE task_id IN "
            + "(SELECT id FROM remediation_tasks WHERE job_id IN "
            + "(SELECT id FROM qa_jobs WHERE project_id = :projectId))",
            nativeQuery = true)
    void deleteJoinTableByProjectId(@Param("projectId") UUID projectId);

    /**
     * Bulk-deletes all remediation tasks for jobs belonging to the given project.
     *
     * @param projectId the project whose remediation tasks to remove
     */
    @Modifying
    @Query("DELETE FROM RemediationTask t WHERE t.job.id IN "
            + "(SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

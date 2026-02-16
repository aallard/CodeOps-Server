package com.codeops.repository;

import com.codeops.entity.QaJob;
import com.codeops.entity.enums.JobMode;
import com.codeops.entity.enums.JobStatus;
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
public interface QaJobRepository extends JpaRepository<QaJob, UUID> {

    List<QaJob> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<QaJob> findByProjectIdAndMode(UUID projectId, JobMode mode);

    List<QaJob> findByStartedById(UUID userId);

    Page<QaJob> findByStartedById(UUID userId, Pageable pageable);

    Page<QaJob> findByProjectId(UUID projectId, Pageable pageable);

    long countByProjectIdAndStatus(UUID projectId, JobStatus status);

    /**
     * Bulk-deletes all QA jobs for the given project.
     *
     * @param projectId the project whose jobs to remove
     */
    @Modifying
    @Query("DELETE FROM QaJob j WHERE j.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

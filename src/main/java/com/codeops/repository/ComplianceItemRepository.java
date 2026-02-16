package com.codeops.repository;

import com.codeops.entity.ComplianceItem;
import com.codeops.entity.enums.ComplianceStatus;
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
public interface ComplianceItemRepository extends JpaRepository<ComplianceItem, UUID> {

    List<ComplianceItem> findByJobId(UUID jobId);

    Page<ComplianceItem> findByJobId(UUID jobId, Pageable pageable);

    List<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status);

    Page<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status, Pageable pageable);

    /**
     * Bulk-deletes all compliance items for jobs belonging to the given project.
     *
     * @param projectId the project whose compliance items to remove
     */
    @Modifying
    @Query("DELETE FROM ComplianceItem c WHERE c.job.id IN "
            + "(SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

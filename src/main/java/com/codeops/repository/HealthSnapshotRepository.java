package com.codeops.repository;

import com.codeops.entity.HealthSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthSnapshotRepository extends JpaRepository<HealthSnapshot, UUID> {

    List<HealthSnapshot> findByProjectIdOrderByCapturedAtDesc(UUID projectId);

    Page<HealthSnapshot> findByProjectId(UUID projectId, Pageable pageable);

    Optional<HealthSnapshot> findFirstByProjectIdOrderByCapturedAtDesc(UUID projectId);

    /**
     * Bulk-deletes all health snapshots for the given project.
     *
     * @param projectId the project whose snapshots to remove
     */
    @Modifying
    @Query("DELETE FROM HealthSnapshot h WHERE h.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

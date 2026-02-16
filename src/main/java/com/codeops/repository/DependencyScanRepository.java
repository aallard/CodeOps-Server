package com.codeops.repository;

import com.codeops.entity.DependencyScan;
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
public interface DependencyScanRepository extends JpaRepository<DependencyScan, UUID> {

    List<DependencyScan> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Page<DependencyScan> findByProjectId(UUID projectId, Pageable pageable);

    Optional<DependencyScan> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId);

    /**
     * Bulk-deletes all dependency scans for the given project.
     *
     * @param projectId the project whose scans to remove
     */
    @Modifying
    @Query("DELETE FROM DependencyScan d WHERE d.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

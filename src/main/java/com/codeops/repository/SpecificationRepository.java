package com.codeops.repository;

import com.codeops.entity.Specification;
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
public interface SpecificationRepository extends JpaRepository<Specification, UUID> {

    List<Specification> findByJobId(UUID jobId);

    Page<Specification> findByJobId(UUID jobId, Pageable pageable);

    /**
     * Bulk-deletes all specifications for jobs belonging to the given project.
     *
     * @param projectId the project whose specifications to remove
     */
    @Modifying
    @Query("DELETE FROM Specification s WHERE s.job.id IN "
            + "(SELECT j.id FROM QaJob j WHERE j.project.id = :projectId)")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

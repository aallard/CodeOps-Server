package com.codeops.repository;

import com.codeops.entity.TechDebtItem;
import com.codeops.entity.enums.DebtCategory;
import com.codeops.entity.enums.DebtStatus;
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
public interface TechDebtItemRepository extends JpaRepository<TechDebtItem, UUID> {

    List<TechDebtItem> findByProjectId(UUID projectId);

    Page<TechDebtItem> findByProjectId(UUID projectId, Pageable pageable);

    List<TechDebtItem> findByProjectIdAndStatus(UUID projectId, DebtStatus status);

    Page<TechDebtItem> findByProjectIdAndStatus(UUID projectId, DebtStatus status, Pageable pageable);

    List<TechDebtItem> findByProjectIdAndCategory(UUID projectId, DebtCategory category);

    Page<TechDebtItem> findByProjectIdAndCategory(UUID projectId, DebtCategory category, Pageable pageable);

    long countByProjectIdAndStatus(UUID projectId, DebtStatus status);

    /**
     * Bulk-deletes all tech debt items for the given project.
     *
     * @param projectId the project whose tech debt items to remove
     */
    @Modifying
    @Query("DELETE FROM TechDebtItem t WHERE t.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

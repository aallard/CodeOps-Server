package com.codeops.repository;

import com.codeops.entity.TechDebtItem;
import com.codeops.entity.enums.DebtCategory;
import com.codeops.entity.enums.DebtStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

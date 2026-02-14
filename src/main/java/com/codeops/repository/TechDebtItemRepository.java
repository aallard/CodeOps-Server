package com.codeops.repository;

import com.codeops.entity.TechDebtItem;
import com.codeops.entity.enums.DebtCategory;
import com.codeops.entity.enums.DebtStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TechDebtItemRepository extends JpaRepository<TechDebtItem, UUID> {

    List<TechDebtItem> findByProjectId(UUID projectId);

    List<TechDebtItem> findByProjectIdAndStatus(UUID projectId, DebtStatus status);

    List<TechDebtItem> findByProjectIdAndCategory(UUID projectId, DebtCategory category);

    long countByProjectIdAndStatus(UUID projectId, DebtStatus status);
}

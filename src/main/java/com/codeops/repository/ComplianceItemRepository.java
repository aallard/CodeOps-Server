package com.codeops.repository;

import com.codeops.entity.ComplianceItem;
import com.codeops.entity.enums.ComplianceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplianceItemRepository extends JpaRepository<ComplianceItem, UUID> {

    List<ComplianceItem> findByJobId(UUID jobId);

    Page<ComplianceItem> findByJobId(UUID jobId, Pageable pageable);

    List<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status);

    Page<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status, Pageable pageable);
}

package com.codeops.repository;

import com.codeops.entity.ComplianceItem;
import com.codeops.entity.enums.ComplianceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplianceItemRepository extends JpaRepository<ComplianceItem, UUID> {

    List<ComplianceItem> findByJobId(UUID jobId);

    List<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status);
}

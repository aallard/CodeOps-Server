package com.codeops.repository;

import com.codeops.entity.HealthSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthSnapshotRepository extends JpaRepository<HealthSnapshot, UUID> {

    List<HealthSnapshot> findByProjectIdOrderByCapturedAtDesc(UUID projectId);

    Page<HealthSnapshot> findByProjectId(UUID projectId, Pageable pageable);

    Optional<HealthSnapshot> findFirstByProjectIdOrderByCapturedAtDesc(UUID projectId);
}

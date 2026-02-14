package com.codeops.repository;

import com.codeops.entity.DependencyScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DependencyScanRepository extends JpaRepository<DependencyScan, UUID> {

    List<DependencyScan> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<DependencyScan> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId);
}

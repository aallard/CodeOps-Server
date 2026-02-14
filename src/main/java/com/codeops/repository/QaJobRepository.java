package com.codeops.repository;

import com.codeops.entity.QaJob;
import com.codeops.entity.enums.JobMode;
import com.codeops.entity.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QaJobRepository extends JpaRepository<QaJob, UUID> {

    List<QaJob> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<QaJob> findByProjectIdAndMode(UUID projectId, JobMode mode);

    List<QaJob> findByStartedById(UUID userId);

    Page<QaJob> findByProjectId(UUID projectId, Pageable pageable);

    long countByProjectIdAndStatus(UUID projectId, JobStatus status);
}

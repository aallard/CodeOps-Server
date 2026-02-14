package com.codeops.repository;

import com.codeops.entity.RemediationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RemediationTaskRepository extends JpaRepository<RemediationTask, UUID> {

    List<RemediationTask> findByJobIdOrderByTaskNumberAsc(UUID jobId);

    List<RemediationTask> findByAssignedToId(UUID userId);
}

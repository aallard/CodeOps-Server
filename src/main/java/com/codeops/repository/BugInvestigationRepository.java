package com.codeops.repository;

import com.codeops.entity.BugInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BugInvestigationRepository extends JpaRepository<BugInvestigation, UUID> {

    Optional<BugInvestigation> findByJobId(UUID jobId);

    Optional<BugInvestigation> findByJiraKey(String jiraKey);
}

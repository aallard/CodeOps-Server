package com.codeops.repository;

import com.codeops.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByTeamIdAndIsArchivedFalse(UUID teamId);

    List<Project> findByTeamId(UUID teamId);

    Optional<Project> findByTeamIdAndRepoFullName(UUID teamId, String repoFullName);

    long countByTeamId(UUID teamId);
}

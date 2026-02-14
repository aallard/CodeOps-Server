package com.codeops.repository;

import com.codeops.entity.ProjectDirective;
import com.codeops.entity.ProjectDirectiveId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectDirectiveRepository extends JpaRepository<ProjectDirective, ProjectDirectiveId> {

    List<ProjectDirective> findByProjectId(UUID projectId);

    List<ProjectDirective> findByProjectIdAndEnabledTrue(UUID projectId);

    List<ProjectDirective> findByDirectiveId(UUID directiveId);

    void deleteByProjectIdAndDirectiveId(UUID projectId, UUID directiveId);
}

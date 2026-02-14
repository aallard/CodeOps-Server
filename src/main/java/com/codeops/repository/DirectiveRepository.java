package com.codeops.repository;

import com.codeops.entity.Directive;
import com.codeops.entity.enums.DirectiveScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DirectiveRepository extends JpaRepository<Directive, UUID> {

    List<Directive> findByTeamId(UUID teamId);

    List<Directive> findByProjectId(UUID projectId);

    List<Directive> findByTeamIdAndScope(UUID teamId, DirectiveScope scope);
}

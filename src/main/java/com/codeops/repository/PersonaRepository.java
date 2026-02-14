package com.codeops.repository;

import com.codeops.entity.Persona;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.Scope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, UUID> {

    List<Persona> findByTeamId(UUID teamId);

    List<Persona> findByScope(Scope scope);

    List<Persona> findByTeamIdAndAgentType(UUID teamId, AgentType agentType);

    Optional<Persona> findByTeamIdAndAgentTypeAndIsDefaultTrue(UUID teamId, AgentType agentType);

    List<Persona> findByCreatedById(UUID userId);
}

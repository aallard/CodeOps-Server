package com.codeops.repository;

import com.codeops.entity.Invitation;
import com.codeops.entity.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByTeamIdAndStatus(UUID teamId, InvitationStatus status);

    List<Invitation> findByEmailAndStatus(String email, InvitationStatus status);
}

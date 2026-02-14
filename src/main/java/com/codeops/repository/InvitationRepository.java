package com.codeops.repository;

import com.codeops.entity.Invitation;
import com.codeops.entity.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByTeamIdAndStatus(UUID teamId, InvitationStatus status);

    List<Invitation> findByEmailAndStatus(String email, InvitationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invitation i WHERE i.team.id = :teamId AND i.email = :email AND i.status = :status")
    List<Invitation> findByTeamIdAndEmailAndStatusForUpdate(UUID teamId, String email, InvitationStatus status);
}

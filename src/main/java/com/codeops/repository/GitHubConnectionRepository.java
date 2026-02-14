package com.codeops.repository;

import com.codeops.entity.GitHubConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GitHubConnectionRepository extends JpaRepository<GitHubConnection, UUID> {

    List<GitHubConnection> findByTeamIdAndIsActiveTrue(UUID teamId);
}

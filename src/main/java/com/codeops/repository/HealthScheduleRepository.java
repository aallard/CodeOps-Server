package com.codeops.repository;

import com.codeops.entity.HealthSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HealthScheduleRepository extends JpaRepository<HealthSchedule, UUID> {

    List<HealthSchedule> findByProjectId(UUID projectId);

    List<HealthSchedule> findByIsActiveTrue();
}

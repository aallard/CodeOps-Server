package com.codeops.repository;

import com.codeops.entity.HealthSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HealthScheduleRepository extends JpaRepository<HealthSchedule, UUID> {

    List<HealthSchedule> findByProjectId(UUID projectId);

    List<HealthSchedule> findByIsActiveTrue();

    /**
     * Bulk-deletes all health schedules for the given project.
     *
     * @param projectId the project whose schedules to remove
     */
    @Modifying
    @Query("DELETE FROM HealthSchedule h WHERE h.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") UUID projectId);
}

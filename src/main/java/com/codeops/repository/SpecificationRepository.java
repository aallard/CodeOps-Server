package com.codeops.repository;

import com.codeops.entity.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpecificationRepository extends JpaRepository<Specification, UUID> {

    List<Specification> findByJobId(UUID jobId);

    Page<Specification> findByJobId(UUID jobId, Pageable pageable);
}

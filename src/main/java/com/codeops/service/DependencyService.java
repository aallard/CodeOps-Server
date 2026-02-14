package com.codeops.service;

import com.codeops.dto.request.CreateDependencyScanRequest;
import com.codeops.dto.request.CreateVulnerabilityRequest;
import com.codeops.dto.response.DependencyScanResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.VulnerabilityResponse;
import com.codeops.entity.DependencyScan;
import com.codeops.entity.DependencyVulnerability;
import com.codeops.entity.enums.Severity;
import com.codeops.entity.enums.VulnerabilityStatus;
import com.codeops.repository.*;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DependencyService {

    private final DependencyScanRepository dependencyScanRepository;
    private final DependencyVulnerabilityRepository vulnerabilityRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final QaJobRepository qaJobRepository;

    public DependencyScanResponse createScan(CreateDependencyScanRequest request) {
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        DependencyScan scan = DependencyScan.builder()
                .project(project)
                .job(request.jobId() != null ? qaJobRepository.findById(request.jobId()).orElseThrow(() -> new EntityNotFoundException("Job not found")) : null)
                .manifestFile(request.manifestFile())
                .totalDependencies(request.totalDependencies())
                .outdatedCount(request.outdatedCount())
                .vulnerableCount(request.vulnerableCount())
                .scanDataJson(request.scanDataJson())
                .build();

        scan = dependencyScanRepository.save(scan);
        return mapScanToResponse(scan);
    }

    @Transactional(readOnly = true)
    public DependencyScanResponse getScan(UUID scanId) {
        DependencyScan scan = dependencyScanRepository.findById(scanId)
                .orElseThrow(() -> new EntityNotFoundException("Dependency scan not found"));
        verifyTeamMembership(scan.getProject().getTeam().getId());
        return mapScanToResponse(scan);
    }

    @Transactional(readOnly = true)
    public PageResponse<DependencyScanResponse> getScansForProject(UUID projectId, Pageable pageable) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        Page<DependencyScan> page = dependencyScanRepository.findByProjectId(projectId, pageable);
        List<DependencyScanResponse> content = page.getContent().stream()
                .map(this::mapScanToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public DependencyScanResponse getLatestScan(UUID projectId) {
        return dependencyScanRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)
                .map(this::mapScanToResponse)
                .orElseThrow(() -> new EntityNotFoundException("No dependency scans found for project"));
    }

    public VulnerabilityResponse addVulnerability(CreateVulnerabilityRequest request) {
        DependencyScan scan = dependencyScanRepository.findById(request.scanId())
                .orElseThrow(() -> new EntityNotFoundException("Dependency scan not found"));
        verifyTeamMembership(scan.getProject().getTeam().getId());

        DependencyVulnerability vuln = DependencyVulnerability.builder()
                .scan(scan)
                .dependencyName(request.dependencyName())
                .currentVersion(request.currentVersion())
                .fixedVersion(request.fixedVersion())
                .cveId(request.cveId())
                .severity(request.severity())
                .description(request.description())
                .status(VulnerabilityStatus.OPEN)
                .build();

        vuln = vulnerabilityRepository.save(vuln);
        return mapVulnToResponse(vuln);
    }

    public List<VulnerabilityResponse> addVulnerabilities(List<CreateVulnerabilityRequest> requests) {
        if (requests.isEmpty()) return List.of();

        UUID firstScanId = requests.get(0).scanId();
        boolean allSameScan = requests.stream().allMatch(r -> r.scanId().equals(firstScanId));
        if (!allSameScan) {
            throw new IllegalArgumentException("All vulnerabilities must belong to the same scan");
        }

        DependencyScan scan = dependencyScanRepository.findById(firstScanId)
                .orElseThrow(() -> new EntityNotFoundException("Dependency scan not found"));
        verifyTeamMembership(scan.getProject().getTeam().getId());

        List<DependencyVulnerability> vulns = requests.stream()
                .map(request -> DependencyVulnerability.builder()
                        .scan(scan)
                        .dependencyName(request.dependencyName())
                        .currentVersion(request.currentVersion())
                        .fixedVersion(request.fixedVersion())
                        .cveId(request.cveId())
                        .severity(request.severity())
                        .description(request.description())
                        .status(VulnerabilityStatus.OPEN)
                        .build())
                .toList();

        vulns = vulnerabilityRepository.saveAll(vulns);
        return vulns.stream().map(this::mapVulnToResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<VulnerabilityResponse> getVulnerabilities(UUID scanId, Pageable pageable) {
        DependencyScan scan = dependencyScanRepository.findById(scanId)
                .orElseThrow(() -> new EntityNotFoundException("Dependency scan not found"));
        verifyTeamMembership(scan.getProject().getTeam().getId());
        Page<DependencyVulnerability> page = vulnerabilityRepository.findByScanId(scanId, pageable);
        List<VulnerabilityResponse> content = page.getContent().stream()
                .map(this::mapVulnToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<VulnerabilityResponse> getVulnerabilitiesBySeverity(UUID scanId, Severity severity, Pageable pageable) {
        Page<DependencyVulnerability> page = vulnerabilityRepository.findByScanIdAndSeverity(scanId, severity, pageable);
        List<VulnerabilityResponse> content = page.getContent().stream()
                .map(this::mapVulnToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<VulnerabilityResponse> getOpenVulnerabilities(UUID scanId, Pageable pageable) {
        Page<DependencyVulnerability> page = vulnerabilityRepository.findByScanIdAndStatus(scanId, VulnerabilityStatus.OPEN, pageable);
        List<VulnerabilityResponse> content = page.getContent().stream()
                .map(this::mapVulnToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    public VulnerabilityResponse updateVulnerabilityStatus(UUID vulnerabilityId, VulnerabilityStatus status) {
        DependencyVulnerability vuln = vulnerabilityRepository.findById(vulnerabilityId)
                .orElseThrow(() -> new EntityNotFoundException("Vulnerability not found"));
        verifyTeamMembership(vuln.getScan().getProject().getTeam().getId());
        vuln.setStatus(status);
        vuln = vulnerabilityRepository.save(vuln);
        return mapVulnToResponse(vuln);
    }

    private DependencyScanResponse mapScanToResponse(DependencyScan scan) {
        return new DependencyScanResponse(
                scan.getId(),
                scan.getProject().getId(),
                scan.getJob() != null ? scan.getJob().getId() : null,
                scan.getManifestFile(),
                scan.getTotalDependencies() != null ? scan.getTotalDependencies() : 0,
                scan.getOutdatedCount() != null ? scan.getOutdatedCount() : 0,
                scan.getVulnerableCount() != null ? scan.getVulnerableCount() : 0,
                scan.getCreatedAt()
        );
    }

    private VulnerabilityResponse mapVulnToResponse(DependencyVulnerability vuln) {
        return new VulnerabilityResponse(
                vuln.getId(),
                vuln.getScan().getId(),
                vuln.getDependencyName(),
                vuln.getCurrentVersion(),
                vuln.getFixedVersion(),
                vuln.getCveId(),
                vuln.getSeverity(),
                vuln.getDescription(),
                vuln.getStatus(),
                vuln.getCreatedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }
}

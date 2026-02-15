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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages dependency scans and their associated vulnerability records for projects.
 *
 * <p>A dependency scan captures a snapshot of a project's dependencies from a manifest file,
 * tracking total, outdated, and vulnerable dependency counts. Vulnerabilities discovered
 * during scans are stored with CVE identifiers, severity levels, and remediation status.
 * All operations verify team membership through the project's team association.</p>
 *
 * @see DependencyController
 * @see DependencyScanRepository
 * @see DependencyVulnerabilityRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DependencyService {

    private static final Logger log = LoggerFactory.getLogger(DependencyService.class);

    private final DependencyScanRepository dependencyScanRepository;
    private final DependencyVulnerabilityRepository vulnerabilityRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final QaJobRepository qaJobRepository;

    /**
     * Creates a new dependency scan for a project, optionally linked to a QA job.
     *
     * @param request the creation request containing project ID, optional job ID, manifest file name,
     *                dependency counts, and raw scan data JSON
     * @return the newly created dependency scan as a response DTO
     * @throws EntityNotFoundException if the referenced project or job does not exist
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public DependencyScanResponse createScan(CreateDependencyScanRequest request) {
        log.debug("createScan called with projectId={}, manifestFile={}", request.projectId(), request.manifestFile());
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
        log.info("Created dependency scan id={} for projectId={}, totalDeps={}, vulnerable={}", scan.getId(), request.projectId(), request.totalDependencies(), request.vulnerableCount());
        return mapScanToResponse(scan);
    }

    /**
     * Retrieves a dependency scan by its unique identifier.
     *
     * @param scanId the UUID of the dependency scan to retrieve
     * @return the dependency scan as a response DTO
     * @throws EntityNotFoundException if no scan exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public DependencyScanResponse getScan(UUID scanId) {
        log.debug("getScan called with scanId={}", scanId);
        DependencyScan scan = dependencyScanRepository.findById(scanId)
                .orElseThrow(() -> new EntityNotFoundException("Dependency scan not found"));
        verifyTeamMembership(scan.getProject().getTeam().getId());
        return mapScanToResponse(scan);
    }

    /**
     * Retrieves a paginated list of dependency scans for a project.
     *
     * @param projectId the UUID of the project to retrieve scans for
     * @param pageable  the pagination and sorting parameters
     * @return a paginated response containing dependency scan DTOs
     * @throws EntityNotFoundException if the referenced project does not exist
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public PageResponse<DependencyScanResponse> getScansForProject(UUID projectId, Pageable pageable) {
        log.debug("getScansForProject called with projectId={}", projectId);
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

    /**
     * Retrieves the most recent dependency scan for a project, ordered by creation timestamp.
     *
     * @param projectId the UUID of the project to retrieve the latest scan for
     * @return the most recent dependency scan as a response DTO
     * @throws EntityNotFoundException if no dependency scans exist for the project
     */
    @Transactional(readOnly = true)
    public DependencyScanResponse getLatestScan(UUID projectId) {
        log.debug("getLatestScan called with projectId={}", projectId);
        return dependencyScanRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)
                .map(this::mapScanToResponse)
                .orElseThrow(() -> new EntityNotFoundException("No dependency scans found for project"));
    }

    /**
     * Adds a single vulnerability record to a dependency scan with initial status {@link VulnerabilityStatus#OPEN}.
     *
     * @param request the creation request containing scan ID, dependency name, versions, CVE ID,
     *                severity, and description
     * @return the newly created vulnerability as a response DTO
     * @throws EntityNotFoundException if the referenced dependency scan does not exist
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public VulnerabilityResponse addVulnerability(CreateVulnerabilityRequest request) {
        log.debug("addVulnerability called with scanId={}, cveId={}, severity={}", request.scanId(), request.cveId(), request.severity());
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
        log.info("Created vulnerability id={} for scanId={}, cveId={}, severity={}", vuln.getId(), request.scanId(), request.cveId(), request.severity());
        return mapVulnToResponse(vuln);
    }

    /**
     * Adds multiple vulnerability records in a single batch. All vulnerabilities must belong to the same scan.
     *
     * <p>Each vulnerability is initialized with {@link VulnerabilityStatus#OPEN} status.</p>
     *
     * @param requests the list of creation requests; must all reference the same scan ID
     * @return a list of the newly created vulnerabilities as response DTOs, or an empty list if input is empty
     * @throws IllegalArgumentException if the requests reference different scan IDs
     * @throws EntityNotFoundException if the referenced dependency scan does not exist
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public List<VulnerabilityResponse> addVulnerabilities(List<CreateVulnerabilityRequest> requests) {
        log.debug("addVulnerabilities called with count={}", requests.size());
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
        log.info("Created {} vulnerabilities for scanId={}", vulns.size(), firstScanId);
        return vulns.stream().map(this::mapVulnToResponse).toList();
    }

    /**
     * Retrieves a paginated list of all vulnerabilities for a dependency scan.
     *
     * @param scanId   the UUID of the dependency scan to retrieve vulnerabilities for
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing vulnerability DTOs
     * @throws EntityNotFoundException if the referenced dependency scan does not exist
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public PageResponse<VulnerabilityResponse> getVulnerabilities(UUID scanId, Pageable pageable) {
        log.debug("getVulnerabilities called with scanId={}", scanId);
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

    /**
     * Retrieves a paginated list of vulnerabilities for a scan filtered by severity level.
     *
     * @param scanId   the UUID of the dependency scan to retrieve vulnerabilities for
     * @param severity the severity level to filter by (e.g., CRITICAL, HIGH, MEDIUM, LOW)
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing vulnerability DTOs matching the given severity
     */
    @Transactional(readOnly = true)
    public PageResponse<VulnerabilityResponse> getVulnerabilitiesBySeverity(UUID scanId, Severity severity, Pageable pageable) {
        log.debug("getVulnerabilitiesBySeverity called with scanId={}, severity={}", scanId, severity);
        Page<DependencyVulnerability> page = vulnerabilityRepository.findByScanIdAndSeverity(scanId, severity, pageable);
        List<VulnerabilityResponse> content = page.getContent().stream()
                .map(this::mapVulnToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Retrieves a paginated list of vulnerabilities with {@link VulnerabilityStatus#OPEN} status for a scan.
     *
     * @param scanId   the UUID of the dependency scan to retrieve open vulnerabilities for
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing only open vulnerability DTOs
     */
    @Transactional(readOnly = true)
    public PageResponse<VulnerabilityResponse> getOpenVulnerabilities(UUID scanId, Pageable pageable) {
        log.debug("getOpenVulnerabilities called with scanId={}", scanId);
        Page<DependencyVulnerability> page = vulnerabilityRepository.findByScanIdAndStatus(scanId, VulnerabilityStatus.OPEN, pageable);
        List<VulnerabilityResponse> content = page.getContent().stream()
                .map(this::mapVulnToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Updates the status of a specific vulnerability (e.g., from OPEN to RESOLVED or IGNORED).
     *
     * @param vulnerabilityId the UUID of the vulnerability to update
     * @param status          the new vulnerability status to set
     * @return the updated vulnerability as a response DTO
     * @throws EntityNotFoundException if no vulnerability exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public VulnerabilityResponse updateVulnerabilityStatus(UUID vulnerabilityId, VulnerabilityStatus status) {
        log.debug("updateVulnerabilityStatus called with vulnerabilityId={}, status={}", vulnerabilityId, status);
        DependencyVulnerability vuln = vulnerabilityRepository.findById(vulnerabilityId)
                .orElseThrow(() -> new EntityNotFoundException("Vulnerability not found"));
        verifyTeamMembership(vuln.getScan().getProject().getTeam().getId());
        VulnerabilityStatus oldStatus = vuln.getStatus();
        vuln.setStatus(status);
        vuln = vulnerabilityRepository.save(vuln);
        log.info("Updated vulnerability id={} status from {} to {}", vulnerabilityId, oldStatus, status);
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

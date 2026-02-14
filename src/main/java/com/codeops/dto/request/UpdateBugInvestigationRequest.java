package com.codeops.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateBugInvestigationRequest(
        @Size(max = 50000) String rcaMd,
        @Size(max = 50000) String impactAssessmentMd,
        @Size(max = 1000) String rcaS3Key,
        Boolean rcaPostedToJira,
        Boolean fixTasksCreatedInJira
) {}

package com.codeops.dto.request;

public record UpdateBugInvestigationRequest(
        String rcaMd,
        String impactAssessmentMd,
        String rcaS3Key,
        Boolean rcaPostedToJira,
        Boolean fixTasksCreatedInJira
) {}

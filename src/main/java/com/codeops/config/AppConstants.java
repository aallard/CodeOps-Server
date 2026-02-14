package com.codeops.config;

public final class AppConstants {
    private AppConstants() {}

    // Team limits
    public static final int MAX_TEAM_MEMBERS = 50;
    public static final int MAX_PROJECTS_PER_TEAM = 100;
    public static final int MAX_PERSONAS_PER_TEAM = 50;
    public static final int MAX_DIRECTIVES_PER_PROJECT = 20;

    // File size limits
    public static final int MAX_REPORT_SIZE_MB = 25;
    public static final int MAX_PERSONA_SIZE_KB = 100;
    public static final int MAX_DIRECTIVE_SIZE_KB = 200;
    public static final int MAX_SPEC_FILE_SIZE_MB = 50;

    // Auth
    public static final int JWT_EXPIRY_HOURS = 24;
    public static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
    public static final int INVITATION_EXPIRY_DAYS = 7;
    public static final int MIN_PASSWORD_LENGTH = 8;

    // Notifications
    public static final int HEALTH_DIGEST_DAY = 1;  // Monday
    public static final int HEALTH_DIGEST_HOUR = 8;  // 8 AM

    // S3 prefixes
    public static final String S3_REPORTS = "reports/";
    public static final String S3_SPECS = "specs/";
    public static final String S3_PERSONAS = "personas/";
    public static final String S3_RELEASES = "releases/";

    // QA
    public static final int MAX_CONCURRENT_AGENTS = 5;
    public static final int AGENT_TIMEOUT_MINUTES = 15;
    public static final int DEFAULT_HEALTH_SCORE = 100;

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}

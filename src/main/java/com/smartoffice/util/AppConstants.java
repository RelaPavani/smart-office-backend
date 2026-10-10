package com.smartoffice.util;

public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    public static final String APP_NAME = "Smart Office Management System";
    public static final String APP_VERSION = "1.0.0";

    // API Paths
    public static final String API_PREFIX = "/api";
    public static final String HEALTH_PATH = "/api/health";

    // Pagination Defaults
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";
}

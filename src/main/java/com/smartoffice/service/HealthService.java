package com.smartoffice.service;

import com.smartoffice.dto.response.HealthResponse;

public interface HealthService {

    /**
     * Checks and returns the current operational health status of the backend application.
     *
     * @return HealthResponse with status and service information
     */
    HealthResponse getHealthStatus();
}

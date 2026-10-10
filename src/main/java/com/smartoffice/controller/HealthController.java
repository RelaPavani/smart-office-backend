package com.smartoffice.controller;

import com.smartoffice.dto.response.HealthResponse;
import com.smartoffice.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {
        HealthResponse healthResponse = healthService.getHealthStatus();
        return ResponseEntity.ok(healthResponse);
    }
}

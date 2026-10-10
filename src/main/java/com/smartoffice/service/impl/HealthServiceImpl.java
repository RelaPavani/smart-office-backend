package com.smartoffice.service.impl;

import com.smartoffice.dto.response.HealthResponse;
import com.smartoffice.service.HealthService;
import com.smartoffice.util.AppConstants;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HealthServiceImpl implements HealthService {

    @Override
    public HealthResponse getHealthStatus() {
        return HealthResponse.builder()
                .status("UP")
                .service(AppConstants.APP_NAME)
                .version(AppConstants.APP_VERSION)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

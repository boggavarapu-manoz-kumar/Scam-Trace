package com.kampusvc.digitalrisk.health.api;

import com.kampusvc.digitalrisk.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class AppHealthController {

    private final String environment;

    public AppHealthController(@Value("${app.environment:development}") String environment) {
        this.environment = environment;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AppHealthResponse>> checkHealth() {
        AppHealthResponse response = new AppHealthResponse("UP", environment, Instant.now());
        return ResponseEntity.ok(ApiResponse.success("Application is operational", response));
    }
}

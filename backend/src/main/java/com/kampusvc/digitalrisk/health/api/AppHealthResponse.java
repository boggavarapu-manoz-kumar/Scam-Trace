package com.kampusvc.digitalrisk.health.api;

import java.time.Instant;

public class AppHealthResponse {

    private final String status;
    private final String environment;
    private final Instant timestamp;

    public AppHealthResponse(String status, String environment, Instant timestamp) {
        this.status = status;
        this.environment = environment;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public String getEnvironment() {
        return environment;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

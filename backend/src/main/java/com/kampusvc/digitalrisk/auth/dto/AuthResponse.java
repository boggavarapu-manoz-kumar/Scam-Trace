package com.kampusvc.digitalrisk.auth.dto;

import java.time.Instant;

public class AuthResponse {
    private String token;
    private Instant expiresAt;
    private UserDto user;
    private OrganizationDto activeOrganization;

    public AuthResponse(String token, Instant expiresAt, UserDto user, OrganizationDto activeOrganization) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
        this.activeOrganization = activeOrganization;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UserDto getUser() {
        return user;
    }

    public OrganizationDto getActiveOrganization() {
        return activeOrganization;
    }
}

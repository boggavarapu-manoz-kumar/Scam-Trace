package com.kampusvc.digitalrisk.auth.dto;

import java.util.UUID;

public class UserDto {
    private UUID id;
    private String email;
    private String fullName;
    private boolean active;

    public UserDto(UUID id, String email, String fullName, boolean active) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isActive() {
        return active;
    }
}

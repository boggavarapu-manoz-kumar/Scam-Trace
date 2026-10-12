package com.kampusvc.digitalrisk.auth.dto;

import com.kampusvc.digitalrisk.identity.model.OrganizationRole;
import java.util.UUID;

public class OrganizationDto {
    private UUID id;
    private String name;
    private String slug;
    private OrganizationRole role;

    public OrganizationDto(UUID id, String name, String slug, OrganizationRole role) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public OrganizationRole getRole() {
        return role;
    }
}

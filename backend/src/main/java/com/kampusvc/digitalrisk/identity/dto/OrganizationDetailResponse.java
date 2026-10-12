package com.kampusvc.digitalrisk.identity.dto;

import com.kampusvc.digitalrisk.identity.model.OrganizationRole;

import java.time.Instant;
import java.util.UUID;

public record OrganizationDetailResponse(
        UUID id,
        String name,
        String slug,
        boolean active,
        OrganizationRole currentUserRole,
        long memberCount,
        Instant createdAt,
        Instant updatedAt
) {}

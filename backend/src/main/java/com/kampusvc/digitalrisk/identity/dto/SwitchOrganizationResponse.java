package com.kampusvc.digitalrisk.identity.dto;

import com.kampusvc.digitalrisk.identity.model.OrganizationRole;

import java.util.UUID;

public record SwitchOrganizationResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID activeOrganizationId,
        String activeOrganizationName,
        OrganizationRole activeRole
) {}

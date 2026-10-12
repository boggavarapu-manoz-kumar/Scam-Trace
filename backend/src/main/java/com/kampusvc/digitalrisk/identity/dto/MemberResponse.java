package com.kampusvc.digitalrisk.identity.dto;

import com.kampusvc.digitalrisk.identity.model.OrganizationRole;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID membershipId,
        UUID userId,
        String email,
        String fullName,
        OrganizationRole role,
        Instant joinedAt
) {}

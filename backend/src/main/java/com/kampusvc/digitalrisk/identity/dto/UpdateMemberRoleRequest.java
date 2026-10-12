package com.kampusvc.digitalrisk.identity.dto;

import com.kampusvc.digitalrisk.identity.model.OrganizationRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        OrganizationRole role
) {}

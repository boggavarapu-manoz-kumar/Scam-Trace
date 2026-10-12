package com.kampusvc.digitalrisk.identity.dto;

import com.kampusvc.digitalrisk.identity.model.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        String email,

        @NotNull(message = "Role is required")
        OrganizationRole role
) {}

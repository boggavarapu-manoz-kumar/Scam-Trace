package com.kampusvc.digitalrisk.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name
) {}

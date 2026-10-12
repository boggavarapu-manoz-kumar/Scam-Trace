package com.kampusvc.digitalrisk.auth.dto;

import java.util.List;

public class CurrentUserResponse {
    private UserDto user;
    private OrganizationDto activeOrganization;
    private List<OrganizationDto> organizations;

    public CurrentUserResponse(UserDto user, OrganizationDto activeOrganization, List<OrganizationDto> organizations) {
        this.user = user;
        this.activeOrganization = activeOrganization;
        this.organizations = organizations;
    }

    public UserDto getUser() {
        return user;
    }

    public OrganizationDto getActiveOrganization() {
        return activeOrganization;
    }

    public List<OrganizationDto> getOrganizations() {
        return organizations;
    }
}

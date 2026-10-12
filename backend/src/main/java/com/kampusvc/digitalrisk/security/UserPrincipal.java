package com.kampusvc.digitalrisk.security;

import com.kampusvc.digitalrisk.identity.model.OrganizationRole;
import com.kampusvc.digitalrisk.identity.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final boolean active;
    private final UUID activeOrganizationId;
    private final OrganizationRole activeRole;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id, String email, String passwordHash, String fullName, boolean active,
                         UUID activeOrganizationId, OrganizationRole activeRole) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.active = active;
        this.activeOrganizationId = activeOrganizationId;
        this.activeRole = activeRole;
        if (activeRole != null) {
            this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + activeRole.name()));
        } else {
            this.authorities = Collections.emptyList();
        }
    }

    public static UserPrincipal create(User user, UUID activeOrgId, OrganizationRole role) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.isActive(),
                activeOrgId,
                role
        );
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public UUID getActiveOrganizationId() {
        return activeOrganizationId;
    }

    public OrganizationRole getActiveRole() {
        return activeRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}

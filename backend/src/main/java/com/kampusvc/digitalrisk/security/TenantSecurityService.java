package com.kampusvc.digitalrisk.security;

import com.kampusvc.digitalrisk.identity.model.OrganizationMembership;
import com.kampusvc.digitalrisk.identity.model.OrganizationRole;
import com.kampusvc.digitalrisk.identity.repository.OrganizationMembershipRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service("tenantSecurity")
public class TenantSecurityService {

    private final OrganizationMembershipRepository membershipRepository;

    public TenantSecurityService(OrganizationMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public boolean canSwitchTo(UUID targetOrgId) {
        if (targetOrgId == null) {
            return false;
        }
        UserPrincipal principal = getCurrentPrincipal();
        if (principal == null) {
            return false;
        }
        return membershipRepository.existsByUserIdAndOrganizationId(principal.getId(), targetOrgId);
    }

    @Transactional(readOnly = true)
    public boolean isMember(UUID organizationId) {
        if (organizationId == null) {
            return false;
        }
        UserPrincipal principal = getCurrentPrincipal();
        if (principal == null || principal.getActiveOrganizationId() == null) {
            return false;
        }
        // Strict tenant isolation: the authenticated active organization context in the token
        // MUST match the requested organizationId AND the user must hold an active membership.
        if (!organizationId.equals(principal.getActiveOrganizationId())) {
            return false;
        }
        return membershipRepository.existsByUserIdAndOrganizationId(principal.getId(), organizationId);
    }

    @Transactional(readOnly = true)
    public boolean hasRole(UUID organizationId, String... allowedRoles) {
        if (organizationId == null || allowedRoles == null || allowedRoles.length == 0) {
            return false;
        }
        UserPrincipal principal = getCurrentPrincipal();
        if (principal == null || principal.getActiveOrganizationId() == null) {
            return false;
        }
        if (!organizationId.equals(principal.getActiveOrganizationId())) {
            return false;
        }

        Optional<OrganizationMembership> membershipOpt =
                membershipRepository.findByUserIdAndOrganizationId(principal.getId(), organizationId);

        if (membershipOpt.isEmpty()) {
            return false;
        }

        OrganizationRole currentRole = membershipOpt.get().getRole();
        return Arrays.stream(allowedRoles)
                .anyMatch(r -> r.equalsIgnoreCase(currentRole.name()));
    }

    public UserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }
}

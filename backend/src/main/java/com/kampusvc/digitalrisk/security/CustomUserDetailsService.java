package com.kampusvc.digitalrisk.security;

import com.kampusvc.digitalrisk.identity.model.OrganizationMembership;
import com.kampusvc.digitalrisk.identity.model.User;
import com.kampusvc.digitalrisk.identity.repository.OrganizationMembershipRepository;
import com.kampusvc.digitalrisk.identity.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public CustomUserDetailsService(UserRepository userRepository, OrganizationMembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<OrganizationMembership> memberships = membershipRepository.findAllByUserId(user.getId());
        OrganizationMembership primary = memberships.isEmpty() ? null : memberships.get(0);

        return UserPrincipal.create(
                user,
                primary != null ? primary.getOrganization().getId() : null,
                primary != null ? primary.getRole() : null
        );
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadUserByIdAndOrg(UUID userId, UUID orgId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        OrganizationMembership membership = orgId != null
                ? membershipRepository.findByUserIdAndOrganizationId(userId, orgId).orElse(null)
                : null;

        return UserPrincipal.create(
                user,
                membership != null ? membership.getOrganization().getId() : null,
                membership != null ? membership.getRole() : null
        );
    }
}

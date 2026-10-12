package com.kampusvc.digitalrisk.identity.service;

import com.kampusvc.digitalrisk.common.exception.ApiException;
import com.kampusvc.digitalrisk.common.exception.ResourceNotFoundException;
import com.kampusvc.digitalrisk.identity.dto.*;
import com.kampusvc.digitalrisk.identity.model.*;
import com.kampusvc.digitalrisk.identity.repository.OrganizationMembershipRepository;
import com.kampusvc.digitalrisk.identity.repository.OrganizationRepository;
import com.kampusvc.digitalrisk.identity.repository.SecurityAuditLogRepository;
import com.kampusvc.digitalrisk.identity.repository.UserRepository;
import com.kampusvc.digitalrisk.security.JwtTokenProvider;
import com.kampusvc.digitalrisk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final SecurityAuditLogRepository auditLogRepository;
    private final JwtTokenProvider tokenProvider;

    public OrganizationService(OrganizationRepository organizationRepository,
                               OrganizationMembershipRepository membershipRepository,
                               UserRepository userRepository,
                               SecurityAuditLogRepository auditLogRepository,
                               JwtTokenProvider tokenProvider) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public List<OrganizationDetailResponse> getUserOrganizations(UserPrincipal principal) {
        List<OrganizationMembership> memberships = membershipRepository.findAllByUserId(principal.getId());

        return memberships.stream().map(m -> {
            Organization org = m.getOrganization();
            long count = membershipRepository.findAllByOrganizationId(org.getId()).size();
            return new OrganizationDetailResponse(
                    org.getId(),
                    org.getName(),
                    org.getSlug(),
                    org.isActive(),
                    m.getRole(),
                    count,
                    org.getCreatedAt(),
                    org.getUpdatedAt()
            );
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizationDetailResponse getOrganization(UUID orgId, UserPrincipal principal) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationMembership membership = membershipRepository.findByUserIdAndOrganizationId(principal.getId(), orgId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not a member of this organization"));

        long memberCount = membershipRepository.findAllByOrganizationId(orgId).size();

        return new OrganizationDetailResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.isActive(),
                membership.getRole(),
                memberCount,
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }

    @Transactional
    public OrganizationDetailResponse createOrganization(CreateOrganizationRequest request, UserPrincipal principal, String clientIp) {
        if (organizationRepository.existsBySlug(request.slug())) {
            throw new ApiException(HttpStatus.CONFLICT, "ORGANIZATION_SLUG_EXISTS", "Organization slug already in use");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organization org = new Organization(null, request.name(), request.slug());
        org = organizationRepository.save(org);

        OrganizationMembership membership = new OrganizationMembership(null, user, org, OrganizationRole.OWNER);
        membershipRepository.save(membership);

        recordAudit(principal.getId(), org.getId(), "ORGANIZATION_CREATE", clientIp, "Created organization " + org.getName());

        return new OrganizationDetailResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.isActive(),
                OrganizationRole.OWNER,
                1L,
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }

    @Transactional
    public OrganizationDetailResponse updateOrganization(UUID orgId, UpdateOrganizationRequest request, UserPrincipal principal, String clientIp) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationMembership membership = membershipRepository.findByUserIdAndOrganizationId(principal.getId(), orgId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not a member of this organization"));

        if (membership.getRole() != OrganizationRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only organization owners can modify organization details");
        }

        org.setName(request.name());
        org = organizationRepository.save(org);

        long memberCount = membershipRepository.findAllByOrganizationId(orgId).size();
        recordAudit(principal.getId(), orgId, "ORGANIZATION_UPDATE", clientIp, "Updated organization name to " + org.getName());

        return new OrganizationDetailResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.isActive(),
                membership.getRole(),
                memberCount,
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(UUID orgId, UserPrincipal principal) {
        if (!membershipRepository.existsByUserIdAndOrganizationId(principal.getId(), orgId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not a member of this organization");
        }

        List<OrganizationMembership> members = membershipRepository.findAllByOrganizationId(orgId);
        return members.stream().map(m -> new MemberResponse(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getEmail(),
                m.getUser().getFullName(),
                m.getRole(),
                m.getJoinedAt()
        )).collect(Collectors.toList());
    }

    @Transactional
    public MemberResponse inviteOrAddMember(UUID orgId, InviteMemberRequest request, UserPrincipal principal, String clientIp) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationMembership callerMembership = membershipRepository.findByUserIdAndOrganizationId(principal.getId(), orgId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not a member of this organization"));

        if (callerMembership.getRole() != OrganizationRole.OWNER && callerMembership.getRole() != OrganizationRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only OWNER and ADMIN can invite or add members");
        }

        if (request.role() == OrganizationRole.OWNER && callerMembership.getRole() != OrganizationRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only OWNER can assign the OWNER role");
        }

        User targetUser = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "No user found with email " + request.email() + ". User must register first."));

        if (membershipRepository.existsByUserIdAndOrganizationId(targetUser.getId(), orgId)) {
            throw new ApiException(HttpStatus.CONFLICT, "MEMBER_ALREADY_EXISTS", "User is already a member of this organization");
        }

        OrganizationMembership newMembership = new OrganizationMembership(null, targetUser, org, request.role());
        newMembership = membershipRepository.save(newMembership);

        recordAudit(principal.getId(), orgId, "MEMBER_ADD", clientIp, "Added user " + targetUser.getEmail() + " as " + request.role());

        return new MemberResponse(
                newMembership.getId(),
                targetUser.getId(),
                targetUser.getEmail(),
                targetUser.getFullName(),
                newMembership.getRole(),
                newMembership.getJoinedAt()
        );
    }

    @Transactional
    public MemberResponse updateMemberRole(UUID orgId, UUID membershipId, UpdateMemberRoleRequest request, UserPrincipal principal, String clientIp) {
        OrganizationMembership callerMembership = membershipRepository.findByUserIdAndOrganizationId(principal.getId(), orgId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not a member of this organization"));

        if (callerMembership.getRole() != OrganizationRole.OWNER && callerMembership.getRole() != OrganizationRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only OWNER and ADMIN can update member roles");
        }

        OrganizationMembership targetMembership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership record not found"));

        if (!targetMembership.getOrganization().getId().equals(orgId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Membership does not belong to this organization");
        }

        if (callerMembership.getRole() == OrganizationRole.ADMIN) {
            if (targetMembership.getRole() == OrganizationRole.OWNER || targetMembership.getRole() == OrganizationRole.ADMIN) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "ADMINs cannot alter roles of OWNER or other ADMINs");
            }
            if (request.role() == OrganizationRole.OWNER || request.role() == OrganizationRole.ADMIN) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "ADMINs cannot grant OWNER or ADMIN roles");
            }
        }

        // Prevent demoting the last OWNER
        if (targetMembership.getRole() == OrganizationRole.OWNER && request.role() != OrganizationRole.OWNER) {
            long ownerCount = membershipRepository.findAllByOrganizationId(orgId).stream()
                    .filter(m -> m.getRole() == OrganizationRole.OWNER)
                    .count();
            if (ownerCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "LAST_OWNER_DEMOTION_FORBIDDEN", "Cannot demote the only remaining OWNER of the organization");
            }
        }

        targetMembership.setRole(request.role());
        targetMembership = membershipRepository.save(targetMembership);

        recordAudit(principal.getId(), orgId, "MEMBER_ROLE_CHANGE", clientIp, "Changed role of " + targetMembership.getUser().getEmail() + " to " + request.role());

        return new MemberResponse(
                targetMembership.getId(),
                targetMembership.getUser().getId(),
                targetMembership.getUser().getEmail(),
                targetMembership.getUser().getFullName(),
                targetMembership.getRole(),
                targetMembership.getJoinedAt()
        );
    }

    @Transactional
    public void removeMember(UUID orgId, UUID membershipId, UserPrincipal principal, String clientIp) {
        OrganizationMembership callerMembership = membershipRepository.findByUserIdAndOrganizationId(principal.getId(), orgId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not a member of this organization"));

        if (callerMembership.getRole() != OrganizationRole.OWNER && callerMembership.getRole() != OrganizationRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only OWNER and ADMIN can remove members");
        }

        OrganizationMembership targetMembership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership record not found"));

        if (!targetMembership.getOrganization().getId().equals(orgId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Membership does not belong to this organization");
        }

        if (callerMembership.getRole() == OrganizationRole.ADMIN &&
                (targetMembership.getRole() == OrganizationRole.OWNER || targetMembership.getRole() == OrganizationRole.ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "ADMINs cannot remove OWNER or other ADMINs");
        }

        // Prevent removing the last owner
        if (targetMembership.getRole() == OrganizationRole.OWNER) {
            long ownerCount = membershipRepository.findAllByOrganizationId(orgId).stream()
                    .filter(m -> m.getRole() == OrganizationRole.OWNER)
                    .count();
            if (ownerCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "LAST_OWNER_REMOVAL_FORBIDDEN", "Cannot remove the only remaining OWNER of the organization");
            }
        }

        membershipRepository.delete(targetMembership);
        recordAudit(principal.getId(), orgId, "MEMBER_REMOVE", clientIp, "Removed user " + targetMembership.getUser().getEmail() + " from organization");
    }

    @Transactional(readOnly = true)
    public SwitchOrganizationResponse switchOrganization(UUID targetOrgId, UserPrincipal principal) {
        OrganizationMembership membership = membershipRepository.findByUserIdAndOrganizationId(principal.getId(), targetOrgId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not belong to the requested organization"));

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserPrincipal switchedPrincipal = UserPrincipal.create(user, targetOrgId, membership.getRole());
        String token = tokenProvider.generateToken(switchedPrincipal);

        return new SwitchOrganizationResponse(
                token,
                "Bearer",
                3600L,
                targetOrgId,
                membership.getOrganization().getName(),
                membership.getRole()
        );
    }

    private void recordAudit(UUID userId, UUID orgId, String eventType, String ip, String details) {
        SecurityAuditLog log = new SecurityAuditLog(
                null,
                userId,
                orgId,
                eventType,
                ip != null ? ip : "127.0.0.1",
                details
        );
        auditLogRepository.save(log);
    }
}

package com.kampusvc.digitalrisk.identity.api;

import com.kampusvc.digitalrisk.common.api.ApiResponse;
import com.kampusvc.digitalrisk.identity.dto.*;
import com.kampusvc.digitalrisk.identity.service.OrganizationService;
import com.kampusvc.digitalrisk.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationDetailResponse>>> getMyOrganizations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<OrganizationDetailResponse> list = organizationService.getUserOrganizations(principal);
        return ResponseEntity.ok(ApiResponse.success("Organizations retrieved", list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationDetailResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        OrganizationDetailResponse response = organizationService.createOrganization(request, principal, ip);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organization created successfully", response));
    }

    @GetMapping("/{orgId}")
    @PreAuthorize("@tenantSecurity.isMember(#orgId)")
    public ResponseEntity<ApiResponse<OrganizationDetailResponse>> getOrganization(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrganizationDetailResponse response = organizationService.getOrganization(orgId, principal);
        return ResponseEntity.ok(ApiResponse.success("Organization details retrieved", response));
    }

    @PutMapping("/{orgId}")
    @PreAuthorize("@tenantSecurity.hasRole(#orgId, 'OWNER')")
    public ResponseEntity<ApiResponse<OrganizationDetailResponse>> updateOrganization(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        OrganizationDetailResponse response = organizationService.updateOrganization(orgId, request, principal, ip);
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", response));
    }

    @GetMapping("/{orgId}/members")
    @PreAuthorize("@tenantSecurity.isMember(#orgId)")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembers(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<MemberResponse> members = organizationService.getMembers(orgId, principal);
        return ResponseEntity.ok(ApiResponse.success("Organization members retrieved", members));
    }

    @PostMapping("/{orgId}/members")
    @PreAuthorize("@tenantSecurity.hasRole(#orgId, 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponse>> addMember(
            @PathVariable UUID orgId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        MemberResponse response = organizationService.inviteOrAddMember(orgId, request, principal, ip);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Member added successfully", response));
    }

    @PutMapping("/{orgId}/members/{membershipId}/role")
    @PreAuthorize("@tenantSecurity.hasRole(#orgId, 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMemberRole(
            @PathVariable UUID orgId,
            @PathVariable UUID membershipId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        MemberResponse response = organizationService.updateMemberRole(orgId, membershipId, request, principal, ip);
        return ResponseEntity.ok(ApiResponse.success("Member role updated successfully", response));
    }

    @DeleteMapping("/{orgId}/members/{membershipId}")
    @PreAuthorize("@tenantSecurity.hasRole(#orgId, 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID orgId,
            @PathVariable UUID membershipId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        organizationService.removeMember(orgId, membershipId, principal, ip);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", null));
    }

    @PostMapping("/{orgId}/switch")
    @PreAuthorize("@tenantSecurity.canSwitchTo(#orgId)")
    public ResponseEntity<ApiResponse<SwitchOrganizationResponse>> switchOrganization(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        SwitchOrganizationResponse response = organizationService.switchOrganization(orgId, principal);
        return ResponseEntity.ok(ApiResponse.success("Switched active organization context", response));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

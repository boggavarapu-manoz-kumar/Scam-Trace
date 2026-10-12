package com.kampusvc.digitalrisk.auth.service;

import com.kampusvc.digitalrisk.auth.dto.*;
import com.kampusvc.digitalrisk.common.exception.ApiException;
import com.kampusvc.digitalrisk.identity.model.Organization;
import com.kampusvc.digitalrisk.identity.model.OrganizationMembership;
import com.kampusvc.digitalrisk.identity.model.OrganizationRole;
import com.kampusvc.digitalrisk.identity.model.SecurityAuditLog;
import com.kampusvc.digitalrisk.identity.model.User;
import com.kampusvc.digitalrisk.identity.repository.OrganizationMembershipRepository;
import com.kampusvc.digitalrisk.identity.repository.OrganizationRepository;
import com.kampusvc.digitalrisk.identity.repository.SecurityAuditLogRepository;
import com.kampusvc.digitalrisk.identity.repository.UserRepository;
import com.kampusvc.digitalrisk.security.JwtTokenProvider;
import com.kampusvc.digitalrisk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final SecurityAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       OrganizationMembershipRepository membershipRepository,
                       SecurityAuditLogRepository auditLogRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists");
        }

        String baseSlug = generateSlug(request.getOrganizationName());
        String slug = baseSlug;
        int counter = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        User user = new User(
                null,
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim()
        );
        user = userRepository.save(user);

        Organization organization = new Organization(
                null,
                request.getOrganizationName().trim(),
                slug
        );
        organization = organizationRepository.save(organization);

        OrganizationMembership membership = new OrganizationMembership(
                null,
                user,
                organization,
                OrganizationRole.OWNER
        );
        membershipRepository.save(membership);

        auditLogRepository.save(new SecurityAuditLog(
                null,
                user.getId(),
                organization.getId(),
                "AUTH_REGISTER",
                ipAddress,
                "User registered with organization " + organization.getName()
        ));

        UserPrincipal principal = UserPrincipal.create(user, organization.getId(), OrganizationRole.OWNER);
        String token = tokenProvider.generateToken(principal);
        Instant expiresAt = tokenProvider.getExpirationFromToken(token);

        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.isActive());
        OrganizationDto orgDto = new OrganizationDto(organization.getId(), organization.getName(), organization.getSlug(), OrganizationRole.OWNER);

        return new AuthResponse(token, expiresAt, userDto, orgDto);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    auditLogRepository.save(new SecurityAuditLog(
                            null, null, null, "AUTH_LOGIN_FAILED", ipAddress, "Unknown email attempt"
                    ));
                    return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
                });

        if (!user.isActive()) {
            auditLogRepository.save(new SecurityAuditLog(
                    null, user.getId(), null, "AUTH_LOGIN_FAILED", ipAddress, "Account disabled"
            ));
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Your account has been disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditLogRepository.save(new SecurityAuditLog(
                    null, user.getId(), null, "AUTH_LOGIN_FAILED", ipAddress, "Bad password"
            ));
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }

        List<OrganizationMembership> memberships = membershipRepository.findAllByUserId(user.getId());
        OrganizationMembership primary = memberships.isEmpty() ? null : memberships.get(0);

        UUID activeOrgId = primary != null ? primary.getOrganization().getId() : null;
        OrganizationRole activeRole = primary != null ? primary.getRole() : null;

        auditLogRepository.save(new SecurityAuditLog(
                null,
                user.getId(),
                activeOrgId,
                "AUTH_LOGIN_SUCCESS",
                ipAddress,
                "User logged in successfully"
        ));

        UserPrincipal principal = UserPrincipal.create(user, activeOrgId, activeRole);
        String token = tokenProvider.generateToken(principal);
        Instant expiresAt = tokenProvider.getExpirationFromToken(token);

        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.isActive());
        OrganizationDto orgDto = primary != null
                ? new OrganizationDto(primary.getOrganization().getId(), primary.getOrganization().getName(), primary.getOrganization().getSlug(), primary.getRole())
                : null;

        return new AuthResponse(token, expiresAt, userDto, orgDto);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        List<OrganizationMembership> memberships = membershipRepository.findAllByUserId(user.getId());

        List<OrganizationDto> orgDtos = memberships.stream()
                .map(m -> new OrganizationDto(m.getOrganization().getId(), m.getOrganization().getName(), m.getOrganization().getSlug(), m.getRole()))
                .collect(Collectors.toList());

        OrganizationDto activeOrgDto = orgDtos.stream()
                .filter(o -> o.getId().equals(principal.getActiveOrganizationId()))
                .findFirst()
                .orElse(orgDtos.isEmpty() ? null : orgDtos.get(0));

        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.isActive());

        return new CurrentUserResponse(userDto, activeOrgDto, orgDtos);
    }

    private String generateSlug(String name) {
        String slug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
        return slug.isEmpty() ? "organization" : slug;
    }
}

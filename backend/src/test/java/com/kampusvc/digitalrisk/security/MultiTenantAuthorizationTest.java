package com.kampusvc.digitalrisk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kampusvc.digitalrisk.DigitalRiskPlatformApplication;
import com.kampusvc.digitalrisk.auth.dto.LoginRequest;
import com.kampusvc.digitalrisk.auth.dto.RegisterRequest;
import com.kampusvc.digitalrisk.identity.dto.*;
import com.kampusvc.digitalrisk.identity.model.Organization;
import com.kampusvc.digitalrisk.identity.model.OrganizationMembership;
import com.kampusvc.digitalrisk.identity.model.OrganizationRole;
import com.kampusvc.digitalrisk.identity.model.User;
import com.kampusvc.digitalrisk.identity.repository.OrganizationMembershipRepository;
import com.kampusvc.digitalrisk.identity.repository.OrganizationRepository;
import com.kampusvc.digitalrisk.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DigitalRiskPlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MultiTenantAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User alice; // Owner of Org A
    private Organization orgA;
    private String tokenAliceOrgA;

    private User bob; // Member/Analyst of Org A, Owner of Org B
    private Organization orgB;
    private String tokenBobOrgA;
    private String tokenBobOrgB;

    private User eve; // Viewer of Org A, or completely external
    private String tokenEve;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Organization A
        orgA = new Organization(null, "Acme Corporation", "acme-corp");
        orgA = organizationRepository.save(orgA);

        // 2. Create Organization B
        orgB = new Organization(null, "Beta Technologies", "beta-tech");
        orgB = organizationRepository.save(orgB);

        // 3. User Alice (Owner of Org A)
        alice = new User(null, "alice@acme.com", passwordEncoder.encode("Password123!"), "Alice Owner");
        alice = userRepository.save(alice);
        OrganizationMembership memAliceA = new OrganizationMembership(null, alice, orgA, OrganizationRole.OWNER);
        membershipRepository.save(memAliceA);
        tokenAliceOrgA = tokenProvider.generateToken(UserPrincipal.create(alice, orgA.getId(), OrganizationRole.OWNER));

        // 4. User Bob (Analyst in Org A, Owner of Org B)
        bob = new User(null, "bob@beta.com", passwordEncoder.encode("Password123!"), "Bob Analyst");
        bob = userRepository.save(bob);
        OrganizationMembership memBobA = new OrganizationMembership(null, bob, orgA, OrganizationRole.ANALYST);
        membershipRepository.save(memBobA);
        OrganizationMembership memBobB = new OrganizationMembership(null, bob, orgB, OrganizationRole.OWNER);
        membershipRepository.save(memBobB);
        tokenBobOrgA = tokenProvider.generateToken(UserPrincipal.create(bob, orgA.getId(), OrganizationRole.ANALYST));
        tokenBobOrgB = tokenProvider.generateToken(UserPrincipal.create(bob, orgB.getId(), OrganizationRole.OWNER));

        // 5. User Eve (External / Viewer)
        eve = new User(null, "eve@outsider.com", passwordEncoder.encode("Password123!"), "Eve Viewer");
        eve = userRepository.save(eve);
        OrganizationMembership memEveA = new OrganizationMembership(null, eve, orgA, OrganizationRole.VIEWER);
        membershipRepository.save(memEveA);
        tokenEve = tokenProvider.generateToken(UserPrincipal.create(eve, orgA.getId(), OrganizationRole.VIEWER));
    }

    @Test
    @DisplayName("Negative: User in Org B cannot read Org A record by guessing its ID")
    void shouldRejectCrossTenantAccessWhenReadingAnotherOrgById() throws Exception {
        // Bob acting with Org B credentials tries to read Org A details
        mockMvc.perform(get("/api/v1/organizations/" + orgA.getId())
                        .header("Authorization", "Bearer " + tokenBobOrgB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Negative: User without membership cannot list members of an organization")
    void shouldRejectListingMembersForNonMember() throws Exception {
        // User with no membership in Org B tries to access Org B members
        mockMvc.perform(get("/api/v1/organizations/" + orgB.getId() + "/members")
                        .header("Authorization", "Bearer " + tokenAliceOrgA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Negative: Analyst cannot update organization details (Requires OWNER)")
    void shouldRejectNonOwnerUpdatingOrganization() throws Exception {
        UpdateOrganizationRequest request = new UpdateOrganizationRequest("Hacked Acme Corp");

        mockMvc.perform(put("/api/v1/organizations/" + orgA.getId())
                        .header("Authorization", "Bearer " + tokenBobOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Negative: Viewer cannot add or invite members to organization (Requires OWNER/ADMIN)")
    void shouldRejectViewerInvitingMembers() throws Exception {
        InviteMemberRequest request = new InviteMemberRequest("newuser@example.com", OrganizationRole.ANALYST);

        mockMvc.perform(post("/api/v1/organizations/" + orgA.getId() + "/members")
                        .header("Authorization", "Bearer " + tokenEve)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Negative: Privilege escalation - non-owner cannot grant OWNER role to members")
    void shouldPreventPrivilegeEscalationWhenAssigningRoles() throws Exception {
        // Create an Admin user in Org A
        User admin = new User(null, "admin@acme.com", passwordEncoder.encode("Password123!"), "Admin User");
        admin = userRepository.save(admin);
        OrganizationMembership adminMem = new OrganizationMembership(null, admin, orgA, OrganizationRole.ADMIN);
        membershipRepository.save(adminMem);
        String tokenAdmin = tokenProvider.generateToken(UserPrincipal.create(admin, orgA.getId(), OrganizationRole.ADMIN));

        // Admin tries to invite someone as OWNER -> Must be rejected (Only OWNER can assign OWNER)
        User testTarget = new User(null, "futureowner@acme.com", passwordEncoder.encode("Password123!"), "Target User");
        userRepository.save(testTarget);

        InviteMemberRequest request = new InviteMemberRequest("futureowner@acme.com", OrganizationRole.OWNER);

        mockMvc.perform(post("/api/v1/organizations/" + orgA.getId() + "/members")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("Negative: Demoting or removing the sole organization OWNER must be rejected")
    void shouldPreventDemotingOrRemovingLastOwner() throws Exception {
        OrganizationMembership aliceMem = membershipRepository.findByUserIdAndOrganizationId(alice.getId(), orgA.getId()).orElseThrow();

        // Alice tries to demote herself to VIEWER while being the only owner
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(OrganizationRole.VIEWER);

        mockMvc.perform(put("/api/v1/organizations/" + orgA.getId() + "/members/" + aliceMem.getId() + "/role")
                        .header("Authorization", "Bearer " + tokenAliceOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("LAST_OWNER_DEMOTION_FORBIDDEN")));

        // Alice tries to delete her own membership while being the only owner
        mockMvc.perform(delete("/api/v1/organizations/" + orgA.getId() + "/members/" + aliceMem.getId())
                        .header("Authorization", "Bearer " + tokenAliceOrgA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("LAST_OWNER_REMOVAL_FORBIDDEN")));
    }

    @Test
    @DisplayName("Negative: Cannot switch active organization to an organization where user has no membership")
    void shouldRejectSwitchingToNonMemberOrganization() throws Exception {
        // Alice has no membership in Org B
        mockMvc.perform(post("/api/v1/organizations/" + orgB.getId() + "/switch")
                        .header("Authorization", "Bearer " + tokenAliceOrgA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Positive: Organization switching issues a valid tenant-scoped token")
    void shouldAllowSwitchingToAuthorizedOrganization() throws Exception {
        // Bob belongs to both Org A and Org B. When he switches from Org A to Org B:
        mockMvc.perform(post("/api/v1/organizations/" + orgB.getId() + "/switch")
                        .header("Authorization", "Bearer " + tokenBobOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.activeOrganizationId", is(orgB.getId().toString())))
                .andExpect(jsonPath("$.data.activeRole", is("OWNER")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("Positive: Owner can update organization and invite valid members")
    void shouldAllowOwnerToUpdateAndInviteMembers() throws Exception {
        // 1. Owner updates Org A name
        UpdateOrganizationRequest updateReq = new UpdateOrganizationRequest("Acme Global Security");
        mockMvc.perform(put("/api/v1/organizations/" + orgA.getId())
                        .header("Authorization", "Bearer " + tokenAliceOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Acme Global Security")));

        // 2. Owner invites a registered user as ANALYST
        User newCandidate = new User(null, "candidate@acme.com", passwordEncoder.encode("Password123!"), "Candidate Analyst");
        userRepository.save(newCandidate);

        InviteMemberRequest inviteReq = new InviteMemberRequest("candidate@acme.com", OrganizationRole.ANALYST);
        mockMvc.perform(post("/api/v1/organizations/" + orgA.getId() + "/members")
                        .header("Authorization", "Bearer " + tokenAliceOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("candidate@acme.com")))
                .andExpect(jsonPath("$.data.role", is("ANALYST")));
    }
    @Test
    @DisplayName("Negative: Expired or forged JWT token must be rejected with 401 UNAUTHORIZED")
    void shouldRejectExpiredOrForgedToken() throws Exception {
        // 1. Forged token with invalid signature
        String forgedToken = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxMjM0NTY3OC0xMjM0LTEyMzQtMTIzNC0xMjM0NTY3ODEyMzQiLCJpYXQiOjE2MDAwMDAwMDAsImV4cCI6MjAwMDAwMDAwMH0.fakeSignatureHere";

        mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }
}
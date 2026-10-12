package com.kampusvc.digitalrisk.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kampusvc.digitalrisk.auth.dto.LoginRequest;
import com.kampusvc.digitalrisk.auth.dto.RegisterRequest;
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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

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

    private User activeUser;
    private User disabledUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(new Organization(null, "Test Security Corp", "test-sec-corp"));

        activeUser = new User(null, "active@kampus.vc", passwordEncoder.encode("ValidPass123!"), "Active User");
        activeUser = userRepository.save(activeUser);
        membershipRepository.save(new OrganizationMembership(null, activeUser, testOrg, OrganizationRole.ADMIN));

        disabledUser = new User(null, "disabled@kampus.vc", passwordEncoder.encode("ValidPass123!"), "Disabled User");
        disabledUser.setActive(false);
        disabledUser = userRepository.save(disabledUser);
        membershipRepository.save(new OrganizationMembership(null, disabledUser, testOrg, OrganizationRole.VIEWER));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should create account, org, and return valid JWT")
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice Smith", "alice@kampus.vc", "StrongP@ss123", "Alice Security Labs");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email").value("alice@kampus.vc"))
                .andExpect(jsonPath("$.data.activeOrganization.name").value("Alice Security Labs"))
                .andExpect(jsonPath("$.data.activeOrganization.role").value("OWNER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with duplicate email should return 409 Conflict")
    void shouldRejectDuplicateRegistration() throws Exception {
        RegisterRequest req = new RegisterRequest("Duplicate User", "active@kampus.vc", "StrongP@ss123", "Another Org");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials should return token")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest req = new LoginRequest("active@kampus.vc", "ValidPass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email").value("active@kampus.vc"))
                .andExpect(jsonPath("$.data.activeOrganization.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with bad password should return 401 Unauthorized")
    void shouldRejectBadPassword() throws Exception {
        LoginRequest req = new LoginRequest("active@kampus.vc", "WrongPassword999!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with non-existent email should return 401 Unauthorized")
    void shouldRejectNonExistentEmailWithoutEnumeration() throws Exception {
        LoginRequest req = new LoginRequest("nonexistent@kampus.vc", "AnyPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login for disabled user should return 403 Forbidden")
    void shouldRejectDisabledAccount() throws Exception {
        LoginRequest req = new LoginRequest("disabled@kampus.vc", "ValidPass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without token should return 401 Unauthorized")
    void shouldRejectUnauthenticatedMeRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with valid Bearer token should return profile")
    void shouldReturnProfileWithValidToken() throws Exception {
        LoginRequest req = new LoginRequest("active@kampus.vc", "ValidPass123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).path("data").path("token").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("active@kampus.vc"))
                .andExpect(jsonPath("$.data.activeOrganization.name").value("Test Security Corp"));
    }
}

package com.kampusvc.digitalrisk.identity;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User(null, "analyst@kampus.vc", "$2a$12$e8xyzhashedpassword", "Analyst User"));
        testOrg = organizationRepository.save(new Organization(null, "Kampus Security", "kampus-sec"));
    }

    @Test
    @DisplayName("Should create and retrieve user by case-insensitive email")
    void shouldCreateAndFindUserByEmail() {
        Optional<User> found = userRepository.findByEmailIgnoreCase("ANALYST@KAMPUS.VC");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Analyst User");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void shouldEnforceUniqueEmail() {
        User duplicate = new User(null, "analyst@kampus.vc", "$2a$12$anotherhash", "Duplicate User");
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce unique organization slug constraint")
    void shouldEnforceUniqueOrganizationSlug() {
        Organization duplicateOrg = new Organization(null, "Another Org", "kampus-sec");
        assertThatThrownBy(() -> organizationRepository.saveAndFlush(duplicateOrg))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should create membership and query by user and organization")
    void shouldCreateAndQueryMembership() {
        OrganizationMembership membership = membershipRepository.save(
                new OrganizationMembership(null, testUser, testOrg, OrganizationRole.ADMIN));

        assertThat(membership.getId()).isNotNull();
        assertThat(membership.getRole()).isEqualTo(OrganizationRole.ADMIN);

        Optional<OrganizationMembership> found = membershipRepository.findByUserIdAndOrganizationId(
                testUser.getId(), testOrg.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(OrganizationRole.ADMIN);
    }

    @Test
    @DisplayName("Should enforce unique membership rule for user and organization")
    void shouldEnforceUniqueMembershipConstraint() {
        membershipRepository.saveAndFlush(
                new OrganizationMembership(null, testUser, testOrg, OrganizationRole.OWNER));

        OrganizationMembership duplicate = new OrganizationMembership(
                null, testUser, testOrg, OrganizationRole.ANALYST);

        assertThatThrownBy(() -> membershipRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
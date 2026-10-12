package com.kampusvc.digitalrisk.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway migrations should apply all versions up to V2")
    void flywayMigrationShouldApplySuccessfully() {
        assertThat(flyway).isNotNull();
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }

    @Test
    @DisplayName("V1 baseline schema table should exist and be queryable")
    void baselineSchemaTableShouldBeCreatedAndQueryable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_initialization_record", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);

        String id = jdbcTemplate.queryForObject(
                "SELECT id FROM schema_initialization_record WHERE id = 'PLATFORM_BASELINE_INIT'", String.class);
        assertThat(id).isEqualTo("PLATFORM_BASELINE_INIT");
    }

    @Test
    @DisplayName("V2 tables should be created and empty")
    void v2IdentityTablesShouldBeCreated() {
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer orgCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organizations", Integer.class);
        Integer memberCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_memberships", Integer.class);
        Integer auditCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM security_audit_logs", Integer.class);

        assertThat(userCount).isEqualTo(0);
        assertThat(orgCount).isEqualTo(0);
        assertThat(memberCount).isEqualTo(0);
        assertThat(auditCount).isEqualTo(0);
    }
}

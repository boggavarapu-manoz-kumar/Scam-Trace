-- ============================================================================
-- V1__baseline_schema.sql
-- Digital Risk Protection Platform - Foundation Schema Baseline
-- ============================================================================
-- Establishes initial database schema metadata and migration verification.
-- Future sprint migrations will introduce domain entities (brands, identities,
-- scans, findings).

CREATE TABLE schema_initialization_record (
    id VARCHAR(64) PRIMARY KEY,
    initialized_at TIMESTAMP WITH TIME ZONE NOT NULL,
    environment VARCHAR(32) NOT NULL,
    description VARCHAR(255) NOT NULL
);

INSERT INTO schema_initialization_record (id, initialized_at, environment, description)
VALUES ('PLATFORM_BASELINE_INIT', CURRENT_TIMESTAMP, 'BASELINE', 'Platform migration baseline established');

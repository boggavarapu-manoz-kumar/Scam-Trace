package com.kampusvc.digitalrisk.identity.repository;

import com.kampusvc.digitalrisk.identity.model.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {
    List<SecurityAuditLog> findAllByUserId(UUID userId);
    List<SecurityAuditLog> findAllByOrganizationId(UUID organizationId);
}

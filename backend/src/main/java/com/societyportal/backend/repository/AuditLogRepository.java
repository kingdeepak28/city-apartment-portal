// Author: deepak.maheshwari

package com.societyportal.backend.repository;

import com.societyportal.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findTop10ByModuleAndActionAndActorIdOrderByOccurredAtDesc(String module, String action, UUID actorId);
}

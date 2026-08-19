// Author: deepak.maheshwari

package com.societyportal.backend.repository;

import com.societyportal.backend.domain.NoticeReadLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NoticeReadLogRepository extends JpaRepository<NoticeReadLog, UUID> {

    Optional<NoticeReadLog> findByDocumentIdAndUserId(UUID documentId, UUID userId);

    long countByDocumentId(UUID documentId);

    boolean existsByDocumentIdAndUserId(UUID documentId, UUID userId);
}

// Author: deepak.maheshwari

package com.societyportal.backend.repository;

import com.societyportal.backend.domain.DocumentAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentAccessLogRepository extends JpaRepository<DocumentAccessLog, UUID> {
}

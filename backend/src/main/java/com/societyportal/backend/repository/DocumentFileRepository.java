// Author: deepak.maheshwari

package com.societyportal.backend.repository;

import com.societyportal.backend.domain.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> {

    List<DocumentFile> findByDocumentIdOrderByVersionNoDesc(UUID documentId);

    List<DocumentFile> findByDocumentIdAndCurrentTrue(UUID documentId);
}

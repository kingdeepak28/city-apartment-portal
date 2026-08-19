// Author: deepak.maheshwari

package com.societyportal.backend.repository;

import com.societyportal.backend.domain.Document;
import com.societyportal.backend.domain.enums.ContentType;
import com.societyportal.backend.domain.enums.DocumentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {

    // NOTE: derived query methods must reference the entity's Java *field* name ("deleted"),
    // not its accessor name ("isDeleted") - Spring Data resolves properties against the JPA
    // metamodel attribute, which for field-access entities is the raw field name.
    List<Document> findTop10ByDeletedFalseOrderByCreatedAtDesc();

    List<Document> findTop5ByContentTypeAndStatusAndDeletedFalseOrderByPublishedOnDesc(
            ContentType contentType, DocumentStatus status);

    long countByStatusAndDeletedFalse(DocumentStatus status);

    long countByContentTypeAndStatusAndDeletedFalse(ContentType contentType, DocumentStatus status);

    @Query("select count(d) from Document d where d.deleted = false and d.createdAt >= :since")
    long countUploadedSince(@Param("since") OffsetDateTime since);

    List<Document> findByContentTypeAndStatusAndExpiryAtBefore(
            ContentType contentType, DocumentStatus status, OffsetDateTime before);

    List<Document> findByDeletedTrueAndDeletedAtBefore(OffsetDateTime before);

    @Query("select d from Document d where d.deleted = true order by d.deletedAt desc")
    List<Document> findRecycleBin(Pageable pageable);
}

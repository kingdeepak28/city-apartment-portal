// Author: deepak.maheshwari

package com.societyportal.backend.service;

import com.societyportal.backend.domain.*;
import com.societyportal.backend.domain.enums.*;
import com.societyportal.backend.dto.DocumentDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.*;
import com.societyportal.backend.security.AuthPrincipal;
import com.societyportal.backend.security.CurrentUser;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Generic content lifecycle shared by every Document-backed module
 * (Reports, Notices today; Photos/Minutes/Tenders once Phase 2 lands).
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentFileRepository documentFileRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    private final CategoryRepository categoryRepository;
    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ---------------------------------------------------------------- create / update

    @Transactional
    public Document createDraft(ContentType type, String title, UUID categoryId, UUID subCategoryId,
                                 String description, String tags, Map<String, Object> metadata,
                                 String visibilityType, List<String> blocks, List<UUID> userIds,
                                 OffsetDateTime publishAt, OffsetDateTime expiryAt, boolean pinned) {
        Document doc = Document.builder()
                .contentType(type)
                .title(title)
                .description(description)
                .tags(tags)
                .metadata(metadata != null ? metadata : new HashMap<>())
                .status(DocumentStatus.DRAFT)
                .createdBy(currentAdmin())
                .build();
        applyCommonFields(doc, categoryId, subCategoryId, visibilityType, blocks, userIds, publishAt, expiryAt, pinned);
        doc = documentRepository.save(doc);
        auditService.log(type.name(), "CREATE_DRAFT", doc.getId().toString(), null, title);
        return doc;
    }

    /** Content Uploaders may only touch their own drafts (Access Control Matrix: "Own drafts only"). */
    public void enforceUploaderRestriction(Document doc) {
        AuthPrincipal principal = CurrentUser.get();
        if ("UPLOADER".equals(principal.getRole())
                && (doc.getStatus() != DocumentStatus.DRAFT
                    || doc.getCreatedBy() == null
                    || !doc.getCreatedBy().getId().equals(principal.getId()))) {
            throw ApiException.forbidden("Uploaders may only edit their own draft items");
        }
    }

    @Transactional
    public Document update(UUID documentId, String title, UUID categoryId, UUID subCategoryId,
                            String description, String tags, Map<String, Object> metadataPatch,
                            String visibilityType, List<String> blocks, List<UUID> userIds,
                            OffsetDateTime publishAt, OffsetDateTime expiryAt, boolean pinned) {
        Document doc = getOrThrow(documentId);
        enforceUploaderRestriction(doc);
        String oldTitle = doc.getTitle();
        doc.setTitle(title);
        doc.setDescription(description);
        doc.setTags(tags);
        if (metadataPatch != null) {
            doc.getMetadata().putAll(metadataPatch);
        }
        applyCommonFields(doc, categoryId, subCategoryId, visibilityType, blocks, userIds, publishAt, expiryAt, pinned);
        doc.setUpdatedBy(currentAdmin());
        documentRepository.save(doc);
        auditService.log(doc.getContentType().name(), "UPDATE", documentId.toString(), oldTitle, title);
        return doc;
    }

    private void applyCommonFields(Document doc, UUID categoryId, UUID subCategoryId, String visibilityType,
                                    List<String> blocks, List<UUID> userIds, OffsetDateTime publishAt,
                                    OffsetDateTime expiryAt, boolean pinned) {
        if (categoryId != null) {
            doc.setCategory(categoryRepository.findById(categoryId).orElseThrow(() -> ApiException.notFound("Category not found")));
        } else {
            doc.setCategory(null);
        }
        doc.setSubCategory(subCategoryId != null
                ? categoryRepository.findById(subCategoryId).orElseThrow(() -> ApiException.notFound("Sub-category not found"))
                : null);
        if (visibilityType != null) {
            doc.setVisibilityType(VisibilityType.valueOf(visibilityType.toUpperCase()));
        }
        doc.setVisibilityBlocks(blocks != null ? new HashSet<>(blocks) : new HashSet<>());
        doc.setVisibilityUserIds(userIds != null ? new HashSet<>(userIds) : new HashSet<>());
        doc.setPublishAt(publishAt);
        doc.setExpiryAt(expiryAt);
        doc.setPinned(pinned);
    }

    // ---------------------------------------------------------------- files / versioning

    @Transactional
    public void addFiles(UUID documentId, List<MultipartFile> files, String subDir) {
        Document doc = getOrThrow(documentId);
        enforceUploaderRestriction(doc);
        AdminUser uploader = currentAdmin();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            FileStorageService.StoredFile stored = fileStorageService.store(file, subDir);
            int nextVersion = doc.getFiles().stream()
                    .filter(f -> f.getFileName().equalsIgnoreCase(stored.originalName()))
                    .mapToInt(DocumentFile::getVersionNo).max().orElse(0) + 1;
            doc.getFiles().stream()
                    .filter(f -> f.getFileName().equalsIgnoreCase(stored.originalName()) && f.isCurrent())
                    .forEach(f -> f.setCurrent(false));
            DocumentFile documentFile = DocumentFile.builder()
                    .document(doc).fileName(stored.originalName()).filePath(stored.relativePath())
                    .mimeType(stored.mimeType()).fileSize(stored.size())
                    .versionNo(nextVersion).current(true).uploadedBy(uploader).build();
            doc.getFiles().add(documentFile);
        }
        documentRepository.save(doc);
        auditService.log(doc.getContentType().name(), "UPLOAD_FILE", documentId.toString(), null,
                files.size() + " file(s)");
    }

    public List<DocumentDtos.FileInfo> versionHistory(UUID documentId) {
        return documentFileRepository.findByDocumentIdOrderByVersionNoDesc(documentId).stream()
                .map(this::toFileInfo).toList();
    }

    /**
     * The current (non-superseded) attachments for a document, fetched via its own repository
     * query rather than {@code document.getFiles()} - the latter is a lazy collection that would
     * throw LazyInitializationException as soon as it's touched outside the transaction that loaded
     * the owning Document (open-in-view is disabled), which is exactly what every caller here does:
     * fetch a Document in one call, then build a response DTO or a ZIP from its files in another.
     */
    public List<DocumentFile> currentFiles(UUID documentId) {
        return documentFileRepository.findByDocumentIdAndCurrentTrue(documentId);
    }

    // ---------------------------------------------------------------- lifecycle

    @Transactional
    public void publish(UUID documentId) {
        Document doc = getOrThrow(documentId);
        if (doc.getPublishAt() != null && doc.getPublishAt().isAfter(OffsetDateTime.now())) {
            documentRepository.save(doc); // scheduled - the publisher scheduler will flip status later
            auditService.log(doc.getContentType().name(), "SCHEDULE_PUBLISH", documentId.toString(), null, String.valueOf(doc.getPublishAt()));
            return;
        }
        doPublishNow(doc);
    }

    @Transactional
    public void doPublishNow(Document doc) {
        doc.setStatus(DocumentStatus.PUBLISHED);
        doc.setPublishedOn(OffsetDateTime.now());
        documentRepository.save(doc);
        auditService.log(doc.getContentType().name(), "PUBLISH", doc.getId().toString(), null, doc.getTitle());

        List<User> audience = resolveAudience(doc);
        NotificationType type = doc.getContentType() == ContentType.NOTICE
                ? (isUrgent(doc) ? NotificationType.NOTICE_URGENT : NotificationType.NOTICE_PUBLISHED)
                : NotificationType.REPORT_PUBLISHED;
        boolean force = doc.getContentType() == ContentType.NOTICE && isUrgent(doc);
        String prefCategory = doc.getContentType() == ContentType.NOTICE ? "notice" : "report";
        String link = "/" + (doc.getContentType() == ContentType.NOTICE ? "notices" : "reports") + "/" + doc.getId();
        notificationService.notifyMembers(audience, type, "New " + doc.getContentType().name().toLowerCase() + ": " + doc.getTitle(),
                doc.getDescription() != null ? doc.getDescription() : doc.getTitle(), link, force, prefCategory);
    }

    private boolean isUrgent(Document doc) {
        Object p = doc.getMetadata().get("priority");
        return p != null && "URGENT".equalsIgnoreCase(p.toString());
    }

    @Transactional
    public void archive(UUID documentId) {
        Document doc = getOrThrow(documentId);
        doc.setStatus(DocumentStatus.ARCHIVED);
        documentRepository.save(doc);
        auditService.log(doc.getContentType().name(), "ARCHIVE", documentId.toString(), null, null);
    }

    @Transactional
    public void softDelete(UUID documentId) {
        Document doc = getOrThrow(documentId);
        enforceUploaderRestriction(doc);
        doc.setDeleted(true);
        doc.setDeletedAt(OffsetDateTime.now());
        documentRepository.save(doc);
        auditService.log(doc.getContentType().name(), "SOFT_DELETE", documentId.toString(), null, null);
    }

    @Transactional
    public void restore(UUID documentId) {
        // Deliberately NOT getOrThrow() - that filters out deleted items, which is exactly what
        // we're looking for here (the item must currently be in the recycle bin to restore it).
        Document doc = getDeletedOrThrow(documentId);
        doc.setDeleted(false);
        doc.setDeletedAt(null);
        documentRepository.save(doc);
        auditService.log(doc.getContentType().name(), "RESTORE", documentId.toString(), null, null);
    }

    @Transactional
    public void permanentDelete(UUID documentId) {
        Document doc = getDeletedOrThrow(documentId);
        // Every version's physical file, not just the current one - JPA cascade (files is
        // cascade=ALL, orphanRemoval=true) takes care of the DB rows once doc itself is deleted,
        // but the files on disk are outside JPA's reach and need cleaning up explicitly.
        documentFileRepository.findByDocumentIdOrderByVersionNoDesc(documentId)
                .forEach(f -> fileStorageService.delete(f.getFilePath()));
        documentRepository.delete(doc);
        auditService.log(doc.getContentType().name(), "PERMANENT_DELETE", documentId.toString(), doc.getTitle(), null);
    }

    private Document getDeletedOrThrow(UUID id) {
        return documentRepository.findById(id).filter(Document::isDeleted)
                .orElseThrow(() -> ApiException.notFound("This item is not in the recycle bin"));
    }

    public Page<Document> recycleBin(Pageable pageable) {
        return documentRepository.findAll((root, query, cb) -> cb.equal(root.get("deleted"), true), pageable);
    }

    @Transactional
    public void bulkPublish(List<UUID> ids) {
        ids.forEach(this::publish);
    }

    @Transactional
    public void bulkArchive(List<UUID> ids) {
        ids.forEach(this::archive);
    }

    @Transactional
    public void bulkDelete(List<UUID> ids) {
        ids.forEach(this::softDelete);
    }

    @Transactional
    public void bulkChangeCategory(List<UUID> ids, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> ApiException.notFound("Category not found"));
        ids.forEach(id -> {
            Document doc = getOrThrow(id);
            doc.setCategory(category);
            documentRepository.save(doc);
        });
        auditService.log("DOCUMENT", "BULK_CHANGE_CATEGORY", ids.toString(), null, category.getName());
    }

    // ---------------------------------------------------------------- reads

    public Document getOrThrow(UUID id) {
        return documentRepository.findById(id).filter(d -> !d.isDeleted())
                .orElseThrow(() -> ApiException.notFound("Document not found"));
    }

    /**
     * The member-facing equivalent of {@link #getOrThrow}: a document is only ever returned if it is
     * Published and within this member's visibility group - never by guessing an ID for a draft,
     * archived or out-of-audience item (FR-US-17: "nothing else is retrievable, including by direct URL").
     * A 404 (not 403) is used so the response does not confirm the document even exists.
     */
    public Document getPublishedForMember(UUID id) {
        return getForMemberAllowingStatuses(id, java.util.Set.of(DocumentStatus.PUBLISHED));
    }

    /** Same idea as {@link #getPublishedForMember}, but also allows Archived - used by the Notices
     *  "Archive" tab (FR-US-30), which members may still open even after expiry. */
    public Document getForMemberAllowingStatuses(UUID id, java.util.Set<DocumentStatus> allowedStatuses) {
        Document doc = getOrThrow(id);
        if (!allowedStatuses.contains(doc.getStatus()) || !isVisibleToUser(doc, currentMember())) {
            throw ApiException.notFound("Document not found");
        }
        return doc;
    }

    public Page<Document> listForAdmin(ContentType type, UUID categoryId, String status, String keyword,
                                        LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Document> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("contentType"), type));
            predicates.add(cb.equal(root.get("deleted"), false));
            // A document is filed under EITHER a top-level category OR a sub-category, never both
            // levels of the same branch at once - so filtering by whichever one the caller picked
            // has to check both columns, or picking a sub-category (which never appears in
            // "category") would always come back empty even though its documents are right there.
            // subCategory must be an explicit LEFT join: Path.get() on a to-one association joins
            // INNER by default, and most documents have no sub-category at all - an inner join
            // there would silently drop every one of them from the result before the OR below
            // even runs, breaking top-level filtering too, not just fixing sub-category filtering.
            if (categoryId != null) predicates.add(cb.or(
                    cb.equal(root.get("category").get("id"), categoryId),
                    cb.equal(root.join("subCategory", JoinType.LEFT).get("id"), categoryId)));
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), DocumentStatus.valueOf(status.toUpperCase())));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay().atOffset(ZoneOffset.UTC)));
            if (to != null) predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("tags")), like)));
            }
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return documentRepository.findAll(spec, pageable);
    }

    public Page<Document> listForMember(ContentType type, UUID categoryId, String keyword,
                                         LocalDate from, LocalDate to, Pageable pageable) {
        User user = currentMember();
        Specification<Document> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("contentType"), type));
            predicates.add(cb.equal(root.get("status"), DocumentStatus.PUBLISHED));
            predicates.add(cb.equal(root.get("deleted"), false));
            predicates.add(visibilityPredicate(root, cb, user));
            // See the matching comment in listForAdmin: subCategory needs an explicit LEFT join
            // here too, for the same reason.
            if (categoryId != null) predicates.add(cb.or(
                    cb.equal(root.get("category").get("id"), categoryId),
                    cb.equal(root.join("subCategory", JoinType.LEFT).get("id"), categoryId)));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("publishedOn"), from.atStartOfDay().atOffset(ZoneOffset.UTC)));
            if (to != null) predicates.add(cb.lessThan(root.get("publishedOn"), to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("tags")), like)));
            }
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return documentRepository.findAll(spec, pageable);
    }

    /** Builds the OR-of-visibility-rules predicate, joining collections as LEFT so documents with no
     *  block/user targets are not silently excluded from unrelated branches of the OR. */
    private Predicate visibilityPredicate(jakarta.persistence.criteria.Root<Document> root,
                                           jakarta.persistence.criteria.CriteriaBuilder cb, User user) {
        List<Predicate> orPredicates = new ArrayList<>();
        orPredicates.add(cb.equal(root.get("visibilityType"), VisibilityType.ALL));
        if (user.getResidentType() == ResidentType.OWNER) {
            orPredicates.add(cb.equal(root.get("visibilityType"), VisibilityType.OWNERS));
        }
        if (user.getResidentType() == ResidentType.TENANT) {
            orPredicates.add(cb.equal(root.get("visibilityType"), VisibilityType.TENANTS));
        }
        var blockJoin = root.join("visibilityBlocks", JoinType.LEFT);
        orPredicates.add(cb.and(cb.equal(root.get("visibilityType"), VisibilityType.BLOCKS), cb.equal(blockJoin, user.getBlock())));
        var userJoin = root.join("visibilityUserIds", JoinType.LEFT);
        orPredicates.add(cb.and(cb.equal(root.get("visibilityType"), VisibilityType.USERS), cb.equal(userJoin, user.getId())));
        return cb.or(orPredicates.toArray(new Predicate[0]));
    }

    public boolean isVisibleToUser(Document doc, User user) {
        return switch (doc.getVisibilityType()) {
            case ALL -> true;
            case OWNERS -> user.getResidentType() == ResidentType.OWNER;
            case TENANTS -> user.getResidentType() == ResidentType.TENANT;
            case BLOCKS -> doc.getVisibilityBlocks().contains(user.getBlock());
            case USERS -> doc.getVisibilityUserIds().contains(user.getId());
        };
    }

    @Transactional
    public void recordAccess(UUID documentId, AccessAction action) {
        Document doc = getOrThrow(documentId);
        UUID userId = null;
        try {
            AuthPrincipal principal = CurrentUser.get();
            if (!principal.isAdmin()) {
                userId = principal.getId();
                if (!isVisibleToUser(doc, currentMember())) {
                    throw ApiException.forbidden("You do not have access to this document");
                }
            }
        } catch (IllegalStateException ignored) {
            // no authenticated principal (shouldn't happen behind the security filter)
        }
        if (action == AccessAction.VIEW) {
            doc.setViewCount(doc.getViewCount() + 1);
        } else {
            doc.setDownloadCount(doc.getDownloadCount() + 1);
        }
        documentRepository.save(doc);
        DocumentAccessLog log = DocumentAccessLog.builder().document(doc).action(action).build();
        if (userId != null) {
            log.setUser(userRepository.getReferenceById(userId));
        }
        accessLogRepository.save(log);
    }

    public long countPublished(ContentType type) {
        return documentRepository.countByContentTypeAndStatusAndDeletedFalse(type, DocumentStatus.PUBLISHED);
    }

    // ---------------------------------------------------------------- helpers

    public DocumentDtos.FileInfo toFileInfo(DocumentFile f) {
        return DocumentDtos.FileInfo.builder()
                .id(f.getId()).fileName(f.getFileName()).fileSize(f.getFileSize()).mimeType(f.getMimeType())
                .versionNo(f.getVersionNo()).current(f.isCurrent()).uploadedAt(f.getUploadedAt())
                .downloadUrl("/api/files/download/" + f.getId())
                .previewUrl("/api/files/preview/" + f.getId())
                .build();
    }

    private AdminUser currentAdmin() {
        return adminUserRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Admin account not found"));
    }

    private User currentMember() {
        return userRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Member account not found"));
    }

    private List<User> resolveAudience(Document doc) {
        List<User> allActive = userRepository.findByStatus(UserStatus.ACTIVE);
        return allActive.stream().filter(u -> isVisibleToUser(doc, u)).toList();
    }
}

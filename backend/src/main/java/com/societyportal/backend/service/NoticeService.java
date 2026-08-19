// Author: deepak.maheshwari

package com.societyportal.backend.service;

import com.societyportal.backend.domain.Document;
import com.societyportal.backend.domain.NoticeReadLog;
import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.ContentType;
import com.societyportal.backend.domain.enums.DocumentStatus;
import com.societyportal.backend.domain.enums.NotificationType;
import com.societyportal.backend.dto.DocumentDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.DocumentRepository;
import com.societyportal.backend.repository.NoticeReadLogRepository;
import com.societyportal.backend.repository.SettingRepository;
import com.societyportal.backend.repository.UserRepository;
import com.societyportal.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final NoticeReadLogRepository noticeReadLogRepository;
    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final NotificationService notificationService;

    @Transactional
    public Document create(DocumentDtos.NoticeUpsertRequest req) {
        Map<String, Object> metadata = metadataOf(req);
        metadata.put("noticeNumber", generateNoticeNumber());
        Document doc = documentService.createDraft(ContentType.NOTICE, req.getTitle(), req.getCategoryId(), null,
                req.getBodyHtml(), req.getTags(), metadata, req.getVisibilityType(), req.getVisibilityBlocks(),
                req.getVisibilityUserIds(), req.getPublishAt(), req.getExpiryAt(), req.isPinned());
        if ("PUBLISHED".equalsIgnoreCase(req.getStatus())) {
            documentService.publish(doc.getId());
        }
        return doc;
    }

    @Transactional
    public Document update(UUID id, DocumentDtos.NoticeUpsertRequest req) {
        Document doc = documentService.update(id, req.getTitle(), req.getCategoryId(), null,
                req.getBodyHtml(), req.getTags(), metadataOf(req), req.getVisibilityType(),
                req.getVisibilityBlocks(), req.getVisibilityUserIds(), req.getPublishAt(), req.getExpiryAt(), req.isPinned());
        if ("PUBLISHED".equalsIgnoreCase(req.getStatus()) && doc.getStatus() != DocumentStatus.PUBLISHED) {
            documentService.publish(id);
        }
        return doc;
    }

    private Map<String, Object> metadataOf(DocumentDtos.NoticeUpsertRequest req) {
        Map<String, Object> m = new HashMap<>();
        m.put("priority", req.getPriority() != null ? req.getPriority().toUpperCase() : "NORMAL");
        return m;
    }

    private String generateNoticeNumber() {
        String format = settingRepository.findById("notice.numberFormat").map(s -> s.getValue()).orElse("NOT/{FY}/{SEQ}");
        String fy = financialYear();
        long countThisYear = documentRepository.countUploadedSince(
                fyStart()) ; // approx running count within current FY window
        String seq = String.format("%04d", countThisYear + 1);
        return format.replace("{FY}", fy).replace("{SEQ}", seq);
    }

    private String financialYear() {
        LocalDate now = LocalDate.now();
        int startYear = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
        return startYear + "-" + String.valueOf(startYear + 1).substring(2);
    }

    private OffsetDateTime fyStart() {
        LocalDate now = LocalDate.now();
        int startYear = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
        return LocalDate.of(startYear, 4, 1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
    }

    public Page<DocumentDtos.DocumentListItem> listForAdmin(UUID categoryId, String status, String keyword,
                                                              LocalDate from, LocalDate to, Pageable pageable) {
        return documentService.listForAdmin(ContentType.NOTICE, categoryId, status, keyword, from, to, pageable)
                .map(this::toListItem);
    }

    /** Priority and unread-only are applied in-memory on top of the DB page, since they are cheap
     *  post-filters on an already-small "active notices for this member" result set. */
    public List<DocumentDtos.DocumentListItem> listActiveForMember(UUID categoryId, String priority, String keyword,
                                                                     boolean unreadOnly, Pageable pageable) {
        User user = currentMember();
        Page<Document> page = documentService.listForMember(ContentType.NOTICE, categoryId, keyword, null, null, pageable);
        return page.getContent().stream()
                .map(doc -> toListItemWithReadState(doc, user))
                .filter(item -> priority == null || priority.isBlank() || priority.equalsIgnoreCase(item.getPriority()))
                .filter(item -> !unreadOnly || item.isUnread())
                .sorted(java.util.Comparator.comparing(DocumentDtos.DocumentListItem::isPinned).reversed()
                        .thenComparing(DocumentDtos.DocumentListItem::getPublishedOn, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }

    public List<DocumentDtos.DocumentListItem> archived(UUID categoryId, Pageable pageable) {
        User user = currentMember();
        // archived notices are still member-visible for history, filtered by visibility + status ARCHIVED
        return documentRepository.findAll((root, query, cb) -> {
                    var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    predicates.add(cb.equal(root.get("contentType"), ContentType.NOTICE));
                    predicates.add(cb.equal(root.get("status"), DocumentStatus.ARCHIVED));
                    predicates.add(cb.equal(root.get("deleted"), false));
                    if (categoryId != null) {
                        predicates.add(cb.equal(root.get("category").get("id"), categoryId));
                    }
                    return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                }, pageable)
                .stream()
                .filter(d -> documentService.isVisibleToUser(d, user))
                .map(d -> toListItemWithReadState(d, user))
                .toList();
    }

    @Transactional
    public void markRead(UUID documentId) {
        Document doc = documentService.getOrThrow(documentId);
        User user = currentMember();
        if (!noticeReadLogRepository.existsByDocumentIdAndUserId(documentId, user.getId())) {
            noticeReadLogRepository.save(NoticeReadLog.builder().document(doc).user(user).build());
        }
    }

    public ReadReport readReport(UUID documentId) {
        Document doc = documentService.getOrThrow(documentId);
        List<User> audience = userRepository.findByStatus(com.societyportal.backend.domain.enums.UserStatus.ACTIVE).stream()
                .filter(u -> documentService.isVisibleToUser(doc, u)).toList();
        List<User> readUsers = audience.stream()
                .filter(u -> noticeReadLogRepository.existsByDocumentIdAndUserId(documentId, u.getId())).toList();
        List<User> unreadUsers = audience.stream()
                .filter(u -> !noticeReadLogRepository.existsByDocumentIdAndUserId(documentId, u.getId())).toList();
        return new ReadReport(audience.size(), readUsers, unreadUsers);
    }

    @Transactional
    public void sendReminder(UUID documentId) {
        Document doc = documentService.getOrThrow(documentId);
        ReadReport report = readReport(documentId);
        if (!report.unreadUsers().isEmpty()) {
            notificationService.notifyMembers(report.unreadUsers(), NotificationType.NOTICE_REMINDER,
                    "Reminder: " + doc.getTitle(), "You have not yet read this notice. Please review it.",
                    "/notices/" + doc.getId(), false, "notice");
        }
    }

    /** Called by the scheduled job to move expired notices to ARCHIVED. */
    @Transactional
    public int archiveExpired() {
        List<Document> expired = documentRepository.findByContentTypeAndStatusAndExpiryAtBefore(
                ContentType.NOTICE, DocumentStatus.PUBLISHED, OffsetDateTime.now());
        expired.forEach(d -> d.setStatus(DocumentStatus.ARCHIVED));
        documentRepository.saveAll(expired);
        return expired.size();
    }

    /** Called by the scheduled job to publish items whose scheduled publishAt time has arrived. */
    @Transactional
    public int publishScheduled(ContentType type) {
        // draft items whose publishAt has passed
        var candidates = documentRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("contentType"), type),
                cb.equal(root.get("status"), DocumentStatus.DRAFT),
                cb.isNotNull(root.get("publishAt")),
                cb.lessThanOrEqualTo(root.get("publishAt"), OffsetDateTime.now())));
        candidates.forEach(documentService::doPublishNow);
        return candidates.size();
    }

    private DocumentDtos.DocumentListItem toListItem(Document doc) {
        Map<String, Object> md = doc.getMetadata();
        return DocumentDtos.DocumentListItem.builder()
                .id(doc.getId()).contentType(doc.getContentType().name()).title(doc.getTitle())
                .categoryName(doc.getCategory() != null ? doc.getCategory().getName() : null)
                .status(doc.getStatus().name()).visibilityType(doc.getVisibilityType().name())
                .pinned(doc.isPinned()).tags(doc.getTags())
                .publishedOn(doc.getPublishedOn()).publishAt(doc.getPublishAt()).expiryAt(doc.getExpiryAt())
                .createdAt(doc.getCreatedAt())
                .fileCount(documentService.currentFiles(doc.getId()).size())
                .viewCount(doc.getViewCount()).downloadCount(doc.getDownloadCount())
                .noticeNumber(str(md.get("noticeNumber"))).priority(str(md.get("priority")))
                .build();
    }

    private DocumentDtos.DocumentListItem toListItemWithReadState(Document doc, User user) {
        DocumentDtos.DocumentListItem item = toListItem(doc);
        item.setUnread(!noticeReadLogRepository.existsByDocumentIdAndUserId(doc.getId(), user.getId()));
        return item;
    }

    public DocumentDtos.DocumentDetail detail(Document doc) {
        Map<String, Object> md = doc.getMetadata();
        Long readCount = noticeReadLogRepository.countByDocumentId(doc.getId());
        return DocumentDtos.DocumentDetail.builder()
                .id(doc.getId()).contentType(doc.getContentType().name()).title(doc.getTitle())
                .categoryId(doc.getCategory() != null ? doc.getCategory().getId() : null)
                .categoryName(doc.getCategory() != null ? doc.getCategory().getName() : null)
                .bodyHtml(doc.getDescription()).tags(doc.getTags()).status(doc.getStatus().name())
                .visibilityType(doc.getVisibilityType().name())
                .visibilityBlocks(doc.getVisibilityBlocks().stream().toList())
                .visibilityUserIds(doc.getVisibilityUserIds().stream().toList())
                .pinned(doc.isPinned())
                .publishAt(doc.getPublishAt()).publishedOn(doc.getPublishedOn()).expiryAt(doc.getExpiryAt())
                .createdByName(doc.getCreatedBy() != null ? doc.getCreatedBy().getName() : null)
                .createdAt(doc.getCreatedAt()).updatedAt(doc.getUpdatedAt())
                .viewCount(doc.getViewCount()).downloadCount(doc.getDownloadCount())
                .files(documentService.currentFiles(doc.getId()).stream().map(documentService::toFileInfo).toList())
                .noticeNumber(str(md.get("noticeNumber"))).priority(str(md.get("priority")))
                .readCount(readCount)
                .build();
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private User currentMember() {
        return userRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Member account not found"));
    }

    public record ReadReport(long audienceSize, List<User> readUsers, List<User> unreadUsers) {
    }
}

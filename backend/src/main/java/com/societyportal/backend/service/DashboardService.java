package com.societyportal.backend.service;

import com.societyportal.backend.domain.Document;
import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.ContentType;
import com.societyportal.backend.domain.enums.DocumentStatus;
import com.societyportal.backend.domain.enums.UserStatus;
import com.societyportal.backend.dto.DashboardDtos;
import com.societyportal.backend.dto.DocumentDtos;
import com.societyportal.backend.repository.DocumentRepository;
import com.societyportal.backend.repository.UserRepository;
import com.societyportal.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;
    private final NoticeService noticeService;
    private final ReportService reportService;
    private final NotificationService notificationService;

    public DashboardDtos.AdminDashboardResponse adminDashboard() {
        long pendingApprovals = userRepository.countByStatus(UserStatus.PENDING);
        long totalActiveUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long totalDocumentsPublished = documentRepository.countByStatusAndDeletedFalse(DocumentStatus.PUBLISHED);
        long uploadsThisMonth = documentRepository.countUploadedSince(
                OffsetDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC));
        long noticesLive = documentRepository.countByContentTypeAndStatusAndDeletedFalse(ContentType.NOTICE, DocumentStatus.PUBLISHED);

        List<DashboardDtos.RecentItem> recentlyUploaded = documentRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()
                .stream().map(d -> DashboardDtos.RecentItem.builder()
                        .id(d.getId()).contentType(d.getContentType().name()).title(d.getTitle())
                        .uploaderName(d.getCreatedBy() != null ? d.getCreatedBy().getName() : "-")
                        .timestamp(d.getCreatedAt()).status(d.getStatus().name()).build())
                .toList();

        Map<String, Map<String, Long>> uploadsPerMonth = uploadsPerMonthByType();

        List<DashboardDtos.AlertItem> alerts = new ArrayList<>();
        long overdueApprovals = approvalService.overdueCount();
        if (overdueApprovals > 0) {
            alerts.add(DashboardDtos.AlertItem.builder().type("APPROVAL_OVERDUE")
                    .message(overdueApprovals + " registration(s) pending beyond SLA").link("/admin/approvals").build());
        }
        List<Document> expiringNotices = documentRepository.findByContentTypeAndStatusAndExpiryAtBefore(
                ContentType.NOTICE, DocumentStatus.PUBLISHED, OffsetDateTime.now().plusDays(7));
        expiringNotices.forEach(n -> alerts.add(DashboardDtos.AlertItem.builder().type("NOTICE_EXPIRING")
                .message("Notice \"" + n.getTitle() + "\" expires soon").link("/admin/notices/" + n.getId()).build()));

        return DashboardDtos.AdminDashboardResponse.builder()
                .pendingApprovals(pendingApprovals).totalActiveUsers(totalActiveUsers)
                .totalDocumentsPublished(totalDocumentsPublished).uploadsThisMonth(uploadsThisMonth)
                .noticesLive(noticesLive)
                .recentPendingApprovals(approvalService.recentPending(5))
                .recentlyUploaded(recentlyUploaded)
                .uploadsPerMonthByType(uploadsPerMonth)
                .mostViewed(topViewedDocuments(5))
                .alerts(alerts)
                .build();
    }

    private Map<String, Map<String, Long>> uploadsPerMonthByType() {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 11; i >= 0; i--) {
            OffsetDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).toLocalDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);
            String label = monthStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + monthStart.getYear();
            Map<String, Long> byType = new LinkedHashMap<>();
            for (ContentType type : new ContentType[]{ContentType.REPORT, ContentType.NOTICE}) {
                long count = documentRepository.count((root, query, cb) -> cb.and(
                        cb.equal(root.get("contentType"), type),
                        cb.equal(root.get("deleted"), false),
                        cb.greaterThanOrEqualTo(root.get("createdAt"), monthStart),
                        cb.lessThan(root.get("createdAt"), monthEnd)));
                byType.put(type.name(), count);
            }
            result.put(label, byType);
        }
        return result;
    }

    private List<DocumentDtos.DocumentListItem> topViewedDocuments(int limit) {
        org.springframework.data.jpa.domain.Specification<Document> publishedOnly = (root, query, cb) -> cb.and(
                cb.equal(root.get("deleted"), false), cb.equal(root.get("status"), DocumentStatus.PUBLISHED));
        return documentRepository.findAll(publishedOnly,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount")))
                .stream().map(d -> DocumentDtos.DocumentListItem.builder()
                        .id(d.getId()).contentType(d.getContentType().name()).title(d.getTitle())
                        .viewCount(d.getViewCount()).downloadCount(d.getDownloadCount())
                        .status(d.getStatus().name()).build())
                .toList();
    }

    public DashboardDtos.MemberDashboardResponse memberDashboard() {
        User user = userRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> new IllegalStateException("Member not found"));

        List<DocumentDtos.DocumentListItem> latestNotices = noticeService.listActiveForMember(
                null, null, null, false, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "publishedOn")));

        List<DocumentDtos.DocumentListItem> recentReports = reportService.listForMember(
                null, null, null, null, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "publishedOn"))).getContent();

        DocumentDtos.DocumentListItem pinned = latestNotices.stream().filter(DocumentDtos.DocumentListItem::isPinned)
                .findFirst().orElse(null);

        return DashboardDtos.MemberDashboardResponse.builder()
                .memberName(user.getName()).flatNo(user.getFlatNo())
                .unreadNotificationCount(notificationService.unreadCount(user.getId()))
                .latestNotices(latestNotices).recentReports(recentReports)
                .pinnedNotice(pinned)
                .build();
    }
}

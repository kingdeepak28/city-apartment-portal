package com.societyportal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DashboardDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentItem {
        private UUID id;
        private String contentType;
        private String title;
        private String uploaderName;
        private OffsetDateTime timestamp;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertItem {
        private String type; // TENDER_CLOSING / NOTICE_EXPIRING / APPROVAL_OVERDUE
        private String message;
        private String link;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDashboardResponse {
        private long pendingApprovals;
        private long totalActiveUsers;
        private long totalDocumentsPublished;
        private long uploadsThisMonth;
        private long noticesLive;
        private List<UserDtos.UserSummary> recentPendingApprovals;
        private List<RecentItem> recentlyUploaded;
        private Map<String, Map<String, Long>> uploadsPerMonthByType; // month -> contentType -> count
        private List<DocumentDtos.DocumentListItem> mostViewed;
        private List<AlertItem> alerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDashboardResponse {
        private String memberName;
        private String flatNo;
        private long unreadNotificationCount;
        private List<DocumentDtos.DocumentListItem> latestNotices;
        private List<DocumentDtos.DocumentListItem> recentReports;
        private DocumentDtos.DocumentListItem pinnedNotice;
    }
}

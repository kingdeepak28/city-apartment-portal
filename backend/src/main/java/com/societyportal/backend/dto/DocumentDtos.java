package com.societyportal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class DocumentDtos {

    @Data
    public static class ReportUpsertRequest {
        @NotBlank
        private String title;
        private UUID categoryId;
        private UUID subCategoryId;
        private String financialYear;      // e.g. 2025-26
        private String reportPeriod;       // e.g. Q1 / Annual / custom text
        private String description;
        private String preparedBy;
        private LocalDate reportDate;
        private String tags;
        private String visibilityType;     // ALL / OWNERS / TENANTS / BLOCKS / USERS
        private List<String> visibilityBlocks;
        private List<UUID> visibilityUserIds;
        private String status;             // DRAFT / PUBLISHED
        private OffsetDateTime publishAt;  // optional scheduled publish
    }

    @Data
    public static class NoticeUpsertRequest {
        @NotBlank
        private String title;
        private UUID categoryId;
        private String priority;           // NORMAL / IMPORTANT / URGENT
        private String bodyHtml;
        private String tags;
        private String visibilityType;
        private List<String> visibilityBlocks;
        private List<UUID> visibilityUserIds;
        private String status;
        private OffsetDateTime publishAt;
        private OffsetDateTime expiryAt;
        private boolean pinned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private UUID id;
        private String fileName;
        private Long fileSize;
        private String mimeType;
        private int versionNo;
        private boolean current;
        private OffsetDateTime uploadedAt;
        private String downloadUrl;
        private String previewUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentListItem {
        private UUID id;
        private String contentType;
        private String title;
        private String categoryName;
        private String subCategoryName;
        private String status;
        private String visibilityType;
        private boolean pinned;
        private String tags;
        private OffsetDateTime publishedOn;
        private OffsetDateTime publishAt;
        private OffsetDateTime expiryAt;
        private OffsetDateTime createdAt;
        private int fileCount;
        private int viewCount;
        private int downloadCount;
        private boolean unread;
        // type-specific convenience fields (nullable depending on contentType)
        private String financialYear;
        private String reportPeriod;
        private LocalDate reportDate;
        private String preparedBy;
        private String noticeNumber;
        private String priority;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDetail {
        private UUID id;
        private String contentType;
        private String title;
        private UUID categoryId;
        private String categoryName;
        private UUID subCategoryId;
        private String subCategoryName;
        private String description;
        private String bodyHtml;
        private String tags;
        private String status;
        private String visibilityType;
        private List<String> visibilityBlocks;
        private List<UUID> visibilityUserIds;
        private boolean pinned;
        private OffsetDateTime publishAt;
        private OffsetDateTime publishedOn;
        private OffsetDateTime expiryAt;
        private String createdByName;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private int viewCount;
        private int downloadCount;
        private List<FileInfo> files;
        private String financialYear;
        private String reportPeriod;
        private LocalDate reportDate;
        private String preparedBy;
        private String noticeNumber;
        private String priority;
        private Long readCount;
        private Long unreadCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkCategoryChangeRequest {
        private List<UUID> documentIds;
        private UUID categoryId;
    }

    @Data
    public static class BulkIdsRequest {
        private List<UUID> documentIds;
    }
}

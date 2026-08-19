// Author: deepak.maheshwari

package com.societyportal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class NotificationDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationItem {
        private UUID id;
        private String type;
        private String title;
        private String body;
        private String link;
        private boolean read;
        private OffsetDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCountResponse {
        private long unreadCount;
    }

    @Data
    public static class PreferencesRequest {
        private boolean emailReports;
        private boolean emailNotices;
        private boolean emailMinutes;
        private boolean emailTenders;
        private boolean smsReports;
        private boolean smsNotices;
        private boolean smsMinutes;
        private boolean smsTenders;
    }

    @Data
    public static class BroadcastRequest {
        @NotBlank
        private String title;
        @NotBlank
        private String message;
        @NotBlank
        private String audienceType; // ALL / BLOCK / RESIDENT_TYPE / USERS
        private List<String> blocks;
        private String residentType;
        private List<UUID> userIds;
        private boolean sendEmail;
        private boolean sendSms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationLogItem {
        private UUID id;
        private String title;
        private String body;
        private String channel;
        private String deliveryStatus;
        private OffsetDateTime sentAt;
        private String recipientName;
    }
}

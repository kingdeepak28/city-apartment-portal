// Author: deepak.maheshwari

package com.societyportal.backend.dto;

import com.societyportal.backend.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class UserDtos {

    @Data
    public static class UserSummary {
        private UUID id;
        private String name;
        private String flatNo;
        private String block;
        private String residentType;
        private String mobile;
        private String email;
        private String status;
        private OffsetDateTime registeredOn;
        private String proofFileUrl;
        private boolean overdue;
        private String approvedByName;
        private OffsetDateTime approvedOn;
        private OffsetDateTime lastLogin;

        public static UserSummary from(User u, String proofFileUrl, boolean overdue) {
            UserSummary s = new UserSummary();
            s.id = u.getId();
            s.name = u.getName();
            s.flatNo = u.getFlatNo();
            s.block = u.getBlock();
            s.residentType = u.getResidentType().name();
            s.mobile = u.getMobile();
            s.email = u.getEmail();
            s.status = u.getStatus().name();
            s.registeredOn = u.getRegisteredOn();
            s.proofFileUrl = proofFileUrl;
            s.overdue = overdue;
            s.approvedByName = u.getApprovedBy() != null ? u.getApprovedBy().getName() : null;
            s.approvedOn = u.getApprovedOn();
            s.lastLogin = u.getLastLogin();
            return s;
        }
    }

    @Data
    public static class RejectRequest {
        @NotBlank
        private String reason;
        private String remarks;
    }

    @Data
    public static class RequestInfoRequest {
        @NotBlank
        private String note;
    }

    @Data
    public static class BulkIdsRequest {
        private List<UUID> userIds;
    }

    @Data
    public static class BulkRejectRequest {
        private List<UUID> userIds;
        @NotBlank
        private String reason;
        private String remarks;
    }

    @Data
    public static class CreateUserRequest {
        @NotBlank
        private String fullName;
        @NotBlank
        private String flatNo;
        @NotBlank
        private String block;
        @NotBlank
        private String residentType;
        @NotBlank
        private String mobile;
        @NotBlank
        @Email
        private String email;
        private String password; // optional; generated if blank
    }

    @Data
    public static class UpdateStatusRequest {
        @NotBlank
        private String status; // ACTIVE / SUSPENDED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkImportResult {
        private int successCount;
        private List<String> errors;
    }
}

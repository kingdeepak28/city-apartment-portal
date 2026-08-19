// Author: deepak.maheshwari

package com.societyportal.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminAccountDtos {

    @Data
    public static class CreateAdminRequest {
        @NotBlank
        private String name;
        @NotBlank
        @Email
        private String email;
        private String mobile;
        @NotBlank
        private String role; // SUPER_ADMIN / ADMIN / UPLOADER
        private String password; // optional; generated if blank
    }

    @Data
    public static class UpdateAdminRoleRequest {
        @NotBlank
        private String role;
    }

    @Data
    public static class DemoteToUserRequest {
        @NotBlank
        private String flatNo;
        @NotBlank
        private String block;
        @NotBlank
        private String residentType; // OWNER / TENANT
        // Optional: only required if this admin account has no mobile number on file.
        private String mobile;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminSummary {
        private UUID id;
        private String name;
        private String email;
        private String mobile;
        private String role;
        private String status;
        private OffsetDateTime lastLogin;
        private OffsetDateTime createdAt;
    }
}

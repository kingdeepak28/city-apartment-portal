package com.societyportal.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProfileDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileResponse {
        private UUID id;
        private String name;
        private String email;
        private String mobile;
        private String flatNo;
        private String block;
        private String residentType;
        private OffsetDateTime approvedOn;
        private String photoUrl;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank
        private String name;
        private String alternateMobile;
    }

    @Data
    public static class CorrectionRequest {
        @NotBlank
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginHistoryItem {
        private OffsetDateTime timestamp;
        private String ip;
        private String status;
    }
}

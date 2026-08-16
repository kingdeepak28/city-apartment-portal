package com.societyportal.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RegistrationDtos {

    @Data
    public static class RegisterRequest {
        @NotBlank
        private String fullName;
        @NotBlank
        private String flatNo;
        @NotBlank
        private String block;
        @NotBlank
        private String residentType; // OWNER / TENANT
        @NotBlank
        private String mobile;
        @NotBlank
        @Email
        private String email;
        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class RegistrationSubmittedResponse {
        private UUID userId;
        private String status;
        private String message;
    }

    @Data
    @AllArgsConstructor
    public static class DuplicateCheckResponse {
        private boolean duplicate;
        private String message;
    }
}

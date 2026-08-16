package com.societyportal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

public class AuthDtos {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email or mobile is required")
        private String identifier;
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private UUID id;
        private String name;
        private String email;
        private String accountType;
        private String role;

        public LoginResponse(String token, UUID id, String name, String email, String accountType, String role) {
            this.token = token;
            this.id = id;
            this.name = name;
            this.email = email;
            this.accountType = accountType;
            this.role = role;
        }
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank
        private String identifier;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;
    }
}

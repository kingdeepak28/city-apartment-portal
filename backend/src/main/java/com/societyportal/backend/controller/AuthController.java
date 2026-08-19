// Author: deepak.maheshwari

package com.societyportal.backend.controller;

import com.societyportal.backend.dto.AuthDtos;
import com.societyportal.backend.dto.RegistrationDtos;
import com.societyportal.backend.security.AuthPrincipal;
import com.societyportal.backend.security.CurrentUser;
import com.societyportal.backend.service.AuthService;
import com.societyportal.backend.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return authService.login(req.getIdentifier(), req.getPassword());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest req) {
        authService.forgotPassword(req.getIdentifier());
        return ResponseEntity.ok(Map.of("message", "If an account exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest req) {
        authService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
        AuthPrincipal principal = CurrentUser.get();
        authService.changePassword(principal.getId(), principal.isAdmin(), req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // ---- Registration flow ----

    @PostMapping("/register/check-duplicate")
    public ResponseEntity<Map<String, Object>> checkDuplicate(@RequestBody Map<String, String> body) {
        registrationService.checkDuplicate(body.get("email"), body.get("mobile"), body.get("flatNo"), body.get("block"));
        return ResponseEntity.ok(Map.of("duplicate", false));
    }

    @PostMapping(value = "/register/submit", consumes = "multipart/form-data")
    public RegistrationDtos.RegistrationSubmittedResponse submit(
            @RequestPart("data") @Valid RegistrationDtos.RegisterRequest req,
            @RequestPart("proof") MultipartFile proof) {
        return registrationService.submit(req, proof);
    }
}

package com.societyportal.backend.controller;

import com.societyportal.backend.dto.AdminAccountDtos;
import com.societyportal.backend.service.AdminAccountService;
import com.societyportal.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Managing other admin accounts and their roles - restricted to the Super Admin (Access Control Matrix). */
@RestController
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;
    private final AuthService authService;

    @GetMapping
    public List<AdminAccountDtos.AdminSummary> list() {
        return adminAccountService.list();
    }

    @PostMapping
    public AdminAccountDtos.AdminSummary create(@Valid @RequestBody AdminAccountDtos.CreateAdminRequest req) {
        return adminAccountService.create(req);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<Map<String, String>> updateRole(@PathVariable UUID id, @Valid @RequestBody AdminAccountDtos.UpdateAdminRoleRequest req) {
        adminAccountService.updateRole(id, req.getRole());
        return ResponseEntity.ok(Map.of("message", "Role updated"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        adminAccountService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<Map<String, String>> unlock(@PathVariable UUID id) {
        authService.unlockAccount(id, true);
        return ResponseEntity.ok(Map.of("message", "Account unlocked"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        adminAccountService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Admin account deleted"));
    }
}

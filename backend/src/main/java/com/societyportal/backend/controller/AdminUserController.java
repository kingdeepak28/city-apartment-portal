package com.societyportal.backend.controller;

import com.societyportal.backend.domain.User;
import com.societyportal.backend.dto.UserDtos;
import com.societyportal.backend.service.AuthService;
import com.societyportal.backend.service.ExcelExportService;
import com.societyportal.backend.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final AuthService authService;
    private final ExcelExportService excelExportService;

    @GetMapping
    public Page<UserDtos.UserSummary> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String block,
            @RequestParam(required = false) String residentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userAdminService.list(status, block, residentType, from, to, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registeredOn")));
    }

    @GetMapping("/{id}")
    public UserDtos.UserSummary detail(@PathVariable UUID id) {
        User u = userAdminService.getOrThrow(id);
        return UserDtos.UserSummary.from(u, "/api/files/registration-proof/" + u.getId(), false);
    }

    @PostMapping
    public UserDtos.UserSummary create(@Valid @RequestBody UserDtos.CreateUserRequest req) {
        User u = userAdminService.createPreApproved(req);
        return UserDtos.UserSummary.from(u, null, false);
    }

    @PostMapping(value = "/bulk-import", consumes = "multipart/form-data")
    public UserDtos.BulkImportResult bulkImport(@RequestPart("file") MultipartFile file) {
        return userAdminService.bulkImport(file);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateStatus(@PathVariable UUID id, @Valid @RequestBody UserDtos.UpdateStatusRequest req) {
        userAdminService.updateStatus(id, req.getStatus());
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        userAdminService.delete(id);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<Map<String, String>> unlock(@PathVariable UUID id) {
        authService.unlockAccount(id, false);
        return ResponseEntity.ok(Map.of("message", "Account unlocked"));
    }

    @PostMapping("/{id}/trigger-password-reset")
    public ResponseEntity<Map<String, String>> triggerReset(@PathVariable UUID id) {
        authService.adminTriggerPasswordReset(id);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String block) {
        List<UserDtos.UserSummary> rows = userAdminService.list(status, block, null, null, null, null,
                PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "registeredOn"))).getContent();
        List<String> headers = List.of("Name", "Flat No", "Block", "Resident Type", "Mobile", "Email",
                "Status", "Registered On", "Approved By", "Last Login");
        List<List<Object>> data = rows.stream().<List<Object>>map(u -> List.of(
                nullSafe(u.getName()), nullSafe(u.getFlatNo()), nullSafe(u.getBlock()), nullSafe(u.getResidentType()),
                nullSafe(u.getMobile()), nullSafe(u.getEmail()), nullSafe(u.getStatus()),
                nullSafe(u.getRegisteredOn()), nullSafe(u.getApprovedByName()), nullSafe(u.getLastLogin())
        )).toList();
        byte[] xlsx = excelExportService.toXlsx("Users", headers, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(xlsx);
    }

    private String nullSafe(Object o) {
        return o != null ? o.toString() : "";
    }
}

package com.societyportal.backend.controller;

import com.societyportal.backend.dto.UserDtos;
import com.societyportal.backend.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final ApprovalService approvalService;

    @GetMapping
    public Page<UserDtos.UserSummary> queue(
            @RequestParam(required = false) String block,
            @RequestParam(required = false) String residentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return approvalService.pendingQueue(block, residentType, from, to, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registeredOn")));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> approve(@PathVariable java.util.UUID id) {
        approvalService.approve(id);
        return ResponseEntity.ok(Map.of("message", "Registration approved"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> reject(@PathVariable java.util.UUID id, @Valid @RequestBody UserDtos.RejectRequest req) {
        approvalService.reject(id, req.getReason(), req.getRemarks());
        return ResponseEntity.ok(Map.of("message", "Registration rejected"));
    }

    @PostMapping("/{id}/request-info")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> requestInfo(@PathVariable java.util.UUID id, @Valid @RequestBody UserDtos.RequestInfoRequest req) {
        approvalService.requestInfo(id, req.getNote());
        return ResponseEntity.ok(Map.of("message", "Information requested from applicant"));
    }

    @PostMapping("/bulk-approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> bulkApprove(@RequestBody UserDtos.BulkIdsRequest req) {
        approvalService.bulkApprove(req.getUserIds());
        return ResponseEntity.ok(Map.of("message", req.getUserIds().size() + " registration(s) approved"));
    }

    @PostMapping("/bulk-reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> bulkReject(@Valid @RequestBody UserDtos.BulkRejectRequest req) {
        approvalService.bulkReject(req.getUserIds(), req.getReason(), req.getRemarks());
        return ResponseEntity.ok(Map.of("message", req.getUserIds().size() + " registration(s) rejected"));
    }
}

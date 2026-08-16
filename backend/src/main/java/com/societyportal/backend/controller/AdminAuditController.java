package com.societyportal.backend.controller;

import com.societyportal.backend.domain.AuditLog;
import com.societyportal.backend.repository.AuditLogRepository;
import com.societyportal.backend.service.ExcelExportService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-log")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;
    private final ExcelExportService excelExportService;

    @GetMapping
    public Page<AuditLog> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditLogRepository.findAll(spec(module, action, from, to),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<AuditLog> rows = auditLogRepository.findAll(spec(module, null, from, to),
                PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "occurredAt"))).getContent();
        List<String> headers = List.of("Timestamp", "Actor", "Actor Type", "Module", "Action", "Record", "IP");
        List<List<Object>> data = rows.stream().<List<Object>>map(a -> List.of(
                String.valueOf(a.getOccurredAt()), a.getActorName() != null ? a.getActorName() : "",
                a.getActorType().name(), a.getModule(), a.getAction(),
                a.getRecordId() != null ? a.getRecordId() : "", a.getIp() != null ? a.getIp() : ""
        )).toList();
        byte[] xlsx = excelExportService.toXlsx("Audit Log", headers, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(xlsx);
    }

    private org.springframework.data.jpa.domain.Specification<AuditLog> spec(String module, String action, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (module != null && !module.isBlank()) predicates.add(cb.equal(root.get("module"), module));
            if (action != null && !action.isBlank()) predicates.add(cb.equal(root.get("action"), action));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from.atStartOfDay().atOffset(ZoneOffset.UTC)));
            if (to != null) predicates.add(cb.lessThan(root.get("occurredAt"), to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

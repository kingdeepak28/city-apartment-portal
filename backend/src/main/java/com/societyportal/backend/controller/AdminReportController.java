package com.societyportal.backend.controller;

import com.societyportal.backend.domain.Document;
import com.societyportal.backend.dto.DocumentDtos;
import com.societyportal.backend.service.DocumentService;
import com.societyportal.backend.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;
    private final DocumentService documentService;

    @GetMapping
    public Page<DocumentDtos.DocumentListItem> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reportService.listForAdmin(categoryId, status, keyword, from, to,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    public DocumentDtos.DocumentDetail detail(@PathVariable UUID id) {
        return reportService.detail(documentService.getOrThrow(id));
    }

    @GetMapping("/{id}/versions")
    public List<DocumentDtos.FileInfo> versions(@PathVariable UUID id) {
        return documentService.versionHistory(id);
    }

    @PostMapping
    public DocumentDtos.DocumentDetail create(@Valid @RequestBody DocumentDtos.ReportUpsertRequest req) {
        Document doc = reportService.create(req);
        return reportService.detail(doc);
    }

    @PutMapping("/{id}")
    public DocumentDtos.DocumentDetail update(@PathVariable UUID id, @Valid @RequestBody DocumentDtos.ReportUpsertRequest req) {
        Document doc = reportService.update(id, req);
        return reportService.detail(doc);
    }

    @PostMapping(value = "/{id}/files", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadFiles(@PathVariable UUID id, @RequestPart("files") List<MultipartFile> files) {
        documentService.addFiles(id, files, "reports");
        return ResponseEntity.ok(Map.of("message", files.size() + " file(s) uploaded"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> publish(@PathVariable UUID id) {
        documentService.publish(id);
        return ResponseEntity.ok(Map.of("message", "Published"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> archive(@PathVariable UUID id) {
        documentService.archive(id);
        return ResponseEntity.ok(Map.of("message", "Archived"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        documentService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Moved to recycle bin"));
    }

    @PostMapping("/bulk/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> bulkPublish(@RequestBody DocumentDtos.BulkIdsRequest req) {
        documentService.bulkPublish(req.getDocumentIds());
        return ResponseEntity.ok(Map.of("message", "Published"));
    }

    @PostMapping("/bulk/archive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> bulkArchive(@RequestBody DocumentDtos.BulkIdsRequest req) {
        documentService.bulkArchive(req.getDocumentIds());
        return ResponseEntity.ok(Map.of("message", "Archived"));
    }

    @PostMapping("/bulk/delete")
    public ResponseEntity<Map<String, String>> bulkDelete(@RequestBody DocumentDtos.BulkIdsRequest req) {
        documentService.bulkDelete(req.getDocumentIds());
        return ResponseEntity.ok(Map.of("message", "Moved to recycle bin"));
    }

    @PostMapping("/bulk/category")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> bulkCategory(@RequestBody DocumentDtos.BulkCategoryChangeRequest req) {
        documentService.bulkChangeCategory(req.getDocumentIds(), req.getCategoryId());
        return ResponseEntity.ok(Map.of("message", "Category updated"));
    }
}

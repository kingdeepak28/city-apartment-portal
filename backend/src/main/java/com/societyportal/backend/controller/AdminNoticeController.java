// Author: deepak.maheshwari

package com.societyportal.backend.controller;

import com.societyportal.backend.domain.Document;
import com.societyportal.backend.domain.User;
import com.societyportal.backend.dto.DocumentDtos;
import com.societyportal.backend.service.DocumentService;
import com.societyportal.backend.service.NoticeService;
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
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;
    private final DocumentService documentService;
    private final com.societyportal.backend.service.ExcelExportService excelExportService;

    @GetMapping
    public Page<DocumentDtos.DocumentListItem> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return noticeService.listForAdmin(categoryId, status, keyword, from, to,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    public DocumentDtos.DocumentDetail detail(@PathVariable UUID id) {
        return noticeService.detail(documentService.getOrThrow(id));
    }

    @GetMapping("/{id}/versions")
    public List<DocumentDtos.FileInfo> versions(@PathVariable UUID id) {
        return documentService.versionHistory(id);
    }

    @PostMapping
    public DocumentDtos.DocumentDetail create(@Valid @RequestBody DocumentDtos.NoticeUpsertRequest req) {
        Document doc = noticeService.create(req);
        return noticeService.detail(doc);
    }

    @PutMapping("/{id}")
    public DocumentDtos.DocumentDetail update(@PathVariable UUID id, @Valid @RequestBody DocumentDtos.NoticeUpsertRequest req) {
        Document doc = noticeService.update(id, req);
        return noticeService.detail(doc);
    }

    @PostMapping(value = "/{id}/files", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadFiles(@PathVariable UUID id, @RequestPart("files") List<MultipartFile> files) {
        documentService.addFiles(id, files, "notices");
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

    @GetMapping("/{id}/read-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Object> readReport(@PathVariable UUID id) {
        NoticeService.ReadReport report = noticeService.readReport(id);
        return Map.of(
                "audienceSize", report.audienceSize(),
                "readCount", report.readUsers().size(),
                "unreadCount", report.unreadUsers().size(),
                "readUsers", summaries(report.readUsers()),
                "unreadUsers", summaries(report.unreadUsers()));
    }

    @GetMapping("/{id}/read-report/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<byte[]> exportReadReport(@PathVariable UUID id) {
        NoticeService.ReadReport report = noticeService.readReport(id);
        List<String> headers = List.of("Name", "Flat No", "Block", "Email", "Mobile", "Status");
        List<List<Object>> rows = new java.util.ArrayList<>();
        report.readUsers().forEach(u -> rows.add(List.of(u.getName(), u.getFlatNo(), u.getBlock(), u.getEmail(), u.getMobile(), "Read")));
        report.unreadUsers().forEach(u -> rows.add(List.of(u.getName(), u.getFlatNo(), u.getBlock(), u.getEmail(), u.getMobile(), "Unread")));
        byte[] xlsx = excelExportService.toXlsx("Read Report", headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"notice-read-report.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(xlsx);
    }

    @PostMapping("/{id}/send-reminder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> sendReminder(@PathVariable UUID id) {
        noticeService.sendReminder(id);
        return ResponseEntity.ok(Map.of("message", "Reminder sent to users who have not read this notice"));
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

    private List<Map<String, String>> summaries(List<User> users) {
        return users.stream().map(u -> Map.of(
                "name", u.getName(), "flatNo", u.getFlatNo(), "block", u.getBlock(), "email", u.getEmail()
        )).toList();
    }
}

package com.societyportal.backend.controller;

import com.societyportal.backend.domain.Document;
import com.societyportal.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/recycle-bin")
@RequiredArgsConstructor
public class AdminRecycleBinController {

    private final DocumentService documentService;

    @GetMapping
    public Page<Item> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return documentService.recycleBin(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "deletedAt")))
                .map(d -> new Item(d.getId(), d.getContentType().name(), d.getTitle(), d.getDeletedAt(),
                        d.getDeletedAt() != null ? d.getDeletedAt().plusDays(30) : null));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Map<String, String>> restore(@PathVariable UUID id) {
        documentService.restore(id);
        return ResponseEntity.ok(Map.of("message", "Restored"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> permanentDelete(@PathVariable UUID id) {
        documentService.permanentDelete(id);
        return ResponseEntity.ok(Map.of("message", "Permanently deleted"));
    }

    public record Item(UUID id, String contentType, String title, OffsetDateTime deletedAt, OffsetDateTime purgeEligibleAfter) {
    }
}

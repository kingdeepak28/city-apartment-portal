// Author: deepak.maheshwari

package com.societyportal.backend.controller;

import com.societyportal.backend.domain.Document;
import com.societyportal.backend.domain.enums.AccessAction;
import com.societyportal.backend.domain.enums.CategoryType;
import com.societyportal.backend.dto.CategoryDtos;
import com.societyportal.backend.dto.DocumentDtos;
import com.societyportal.backend.service.CategoryService;
import com.societyportal.backend.service.DocumentService;
import com.societyportal.backend.service.FileStorageService;
import com.societyportal.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/member/reports")
@RequiredArgsConstructor
public class MemberReportController {

    private final ReportService reportService;
    private final DocumentService documentService;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    @GetMapping("/categories")
    public List<CategoryDtos.CategoryResponse> categories() {
        return categoryService.list(CategoryType.REPORT, true);
    }

    @GetMapping
    public Page<DocumentDtos.DocumentListItem> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "publishedOn") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Sort.Direction dir = "title".equals(sortBy) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return reportService.listForMember(categoryId, keyword, from, to, PageRequest.of(page, size, Sort.by(dir, sortBy)));
    }

    @GetMapping("/{id}")
    public DocumentDtos.DocumentDetail detail(@PathVariable UUID id) {
        Document doc = documentService.getPublishedForMember(id);
        documentService.recordAccess(id, AccessAction.VIEW);
        return reportService.detail(doc);
    }

    @GetMapping("/{id}/download-all")
    public ResponseEntity<byte[]> downloadAll(@PathVariable UUID id) throws IOException {
        Document doc = documentService.getPublishedForMember(id);
        documentService.recordAccess(id, AccessAction.DOWNLOAD);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (var file : documentService.currentFiles(id)) {
                zos.putNextEntry(new ZipEntry(file.getFileName()));
                zos.write(Files.readAllBytes(fileStorageService.resolve(file.getFilePath())));
                zos.closeEntry();
            }
            zos.finish();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getTitle() + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(baos.toByteArray());
        }
    }
}

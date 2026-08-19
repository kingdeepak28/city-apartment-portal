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
import com.societyportal.backend.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/member/notices")
@RequiredArgsConstructor
public class MemberNoticeController {

    private final NoticeService noticeService;
    private final DocumentService documentService;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    @GetMapping("/categories")
    public List<CategoryDtos.CategoryResponse> categories() {
        return categoryService.list(CategoryType.NOTICE, true);
    }

    @GetMapping
    public List<DocumentDtos.DocumentListItem> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return noticeService.listActiveForMember(categoryId, priority, keyword, unreadOnly,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedOn")));
    }

    @GetMapping("/archive")
    public List<DocumentDtos.DocumentListItem> archive(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return noticeService.archived(categoryId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expiryAt")));
    }

    @GetMapping("/{id}")
    public DocumentDtos.DocumentDetail detail(@PathVariable UUID id) {
        Document doc = documentService.getForMemberAllowingStatuses(id,
                java.util.Set.of(com.societyportal.backend.domain.enums.DocumentStatus.PUBLISHED,
                        com.societyportal.backend.domain.enums.DocumentStatus.ARCHIVED));
        documentService.recordAccess(id, AccessAction.VIEW);
        noticeService.markRead(id);
        return noticeService.detail(doc);
    }

    @GetMapping("/{id}/download-all")
    public ResponseEntity<byte[]> downloadAll(@PathVariable UUID id) throws IOException {
        Document doc = documentService.getForMemberAllowingStatuses(id,
                java.util.Set.of(com.societyportal.backend.domain.enums.DocumentStatus.PUBLISHED,
                        com.societyportal.backend.domain.enums.DocumentStatus.ARCHIVED));
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

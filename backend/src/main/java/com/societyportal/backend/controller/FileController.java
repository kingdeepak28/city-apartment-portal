package com.societyportal.backend.controller;

import com.societyportal.backend.domain.DocumentFile;
import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.AccessAction;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.DocumentFileRepository;
import com.societyportal.backend.repository.UserRepository;
import com.societyportal.backend.security.AuthPrincipal;
import com.societyportal.backend.security.CurrentUser;
import com.societyportal.backend.service.DocumentService;
import com.societyportal.backend.service.FileSignedUrlService;
import com.societyportal.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final DocumentFileRepository documentFileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FileSignedUrlService signedUrlService;
    private final DocumentService documentService;

    @GetMapping("/signed-url/{fileId}")
    public Map<String, String> signedUrl(@PathVariable UUID fileId) {
        DocumentFile file = documentFileRepository.findById(fileId).orElseThrow(() -> ApiException.notFound("File not found"));
        AuthPrincipal principal = CurrentUser.get();
        if (!principal.isAdmin()) {
            User user = userRepository.findById(principal.getId()).orElseThrow(() -> ApiException.notFound("User not found"));
            boolean statusOk = file.getDocument().getStatus() == com.societyportal.backend.domain.enums.DocumentStatus.PUBLISHED
                    || file.getDocument().getStatus() == com.societyportal.backend.domain.enums.DocumentStatus.ARCHIVED;
            if (!statusOk || !documentService.isVisibleToUser(file.getDocument(), user)) {
                throw ApiException.forbidden("You do not have access to this file");
            }
        }
        String token = signedUrlService.issueToken(fileId, principal.getId());
        return Map.of(
                "downloadUrl", "/api/files/download/" + fileId + "?token=" + token,
                "previewUrl", "/api/files/preview/" + fileId + "?token=" + token);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<FileSystemResource> download(@PathVariable UUID fileId, @RequestParam String token) {
        UUID verifiedFileId = signedUrlService.verifyAndGetFileId(token);
        if (!verifiedFileId.equals(fileId)) {
            throw ApiException.forbidden("Invalid download link");
        }
        DocumentFile file = documentFileRepository.findById(fileId).orElseThrow(() -> ApiException.notFound("File not found"));
        documentService.recordAccess(file.getDocument().getId(), AccessAction.DOWNLOAD);
        Path path = fileStorageService.resolve(file.getFilePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        file.getMimeType() != null ? file.getMimeType() : "application/octet-stream"))
                .body(new FileSystemResource(path));
    }

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<FileSystemResource> preview(@PathVariable UUID fileId, @RequestParam String token) {
        UUID verifiedFileId = signedUrlService.verifyAndGetFileId(token);
        if (!verifiedFileId.equals(fileId)) {
            throw ApiException.forbidden("Invalid preview link");
        }
        DocumentFile file = documentFileRepository.findById(fileId).orElseThrow(() -> ApiException.notFound("File not found"));
        documentService.recordAccess(file.getDocument().getId(), AccessAction.VIEW);
        Path path = fileStorageService.resolve(file.getFilePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        file.getMimeType() != null ? file.getMimeType() : "application/octet-stream"))
                .body(new FileSystemResource(path));
    }

    @GetMapping("/registration-proof/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','UPLOADER')")
    public ResponseEntity<FileSystemResource> registrationProof(@PathVariable UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getProofFilePath() == null) {
            throw ApiException.notFound("No proof document on file");
        }
        Path path = fileStorageService.resolve(user.getProofFilePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new FileSystemResource(path));
    }

    @GetMapping("/photo/{userId}")
    public ResponseEntity<FileSystemResource> photo(@PathVariable UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getPhotoPath() == null) {
            throw ApiException.notFound("No photo on file");
        }
        Path path = fileStorageService.resolve(user.getPhotoPath());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline").body(new FileSystemResource(path));
    }
}

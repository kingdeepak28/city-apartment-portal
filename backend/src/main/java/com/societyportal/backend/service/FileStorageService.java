package com.societyportal.backend.service;

import com.societyportal.backend.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded files on local disk, outside the servlet context, and
 * validates them by content signature (not just extension) before saving.
 * Swap the implementation for an S3/object-storage client without touching
 * callers - they only depend on this interface's contract.
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path root;
    private final long maxFileSizeBytes;
    private final Set<String> allowedExtensions;
    private final Tika tika = new Tika();

    public FileStorageService(
            @Value("${app.file-storage.root}") String root,
            @Value("${app.file-storage.max-file-size-mb}") long maxFileSizeMb,
            @Value("${app.file-storage.allowed-extensions}") String allowedExtensions) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
        this.allowedExtensions = Set.of(allowedExtensions.toLowerCase().split(","));
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialise file storage root: " + root, e);
        }
    }

    /** Validates extension, size and actual content signature, then stores the file under a random path. */
    public StoredFile store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("File is empty");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw ApiException.badRequest("File exceeds the maximum allowed size of "
                    + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(originalName).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw ApiException.badRequest("File type ." + extension + " is not allowed");
        }

        try (InputStream in = file.getInputStream()) {
            String detectedType = tika.detect(in, originalName);
            if (isSuspicious(detectedType, extension)) {
                throw ApiException.badRequest("File content does not match its extension (." + extension + ")");
            }
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read uploaded file");
        }

        String datedSubDir = subDir + "/" + LocalDate.now().getYear() + "/" + LocalDate.now().getMonthValue();
        Path targetDir = root.resolve(datedSubDir).normalize();
        try {
            Files.createDirectories(targetDir);
            String storedName = UUID.randomUUID() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path targetPath = targetDir.resolve(storedName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            String relativePath = root.relativize(targetPath).toString().replace('\\', '/');
            String mimeType = URLConnection.guessContentTypeFromName(originalName);
            return new StoredFile(originalName, relativePath, mimeType, file.getSize());
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }
    }

    public Path resolve(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw ApiException.badRequest("Invalid file path");
        }
        return resolved;
    }

    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            log.warn("Could not delete file {}", relativePath, e);
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }

    /** A crude but effective sanity check: a renamed .exe/.html masquerading as .pdf/.jpg gets rejected. */
    private boolean isSuspicious(String detectedType, String extension) {
        List<String> dangerous = List.of(
                "application/x-msdownload", "application/x-sh", "text/html",
                "application/x-executable", "application/javascript", "application/x-bat");
        return dangerous.stream().anyMatch(detectedType::contains);
    }

    public record StoredFile(String originalName, String relativePath, String mimeType, long size) {
    }
}

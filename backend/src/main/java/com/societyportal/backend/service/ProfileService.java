// Author: deepak.maheshwari

package com.societyportal.backend.service;

import com.societyportal.backend.domain.User;
import com.societyportal.backend.dto.ProfileDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.AdminUserRepository;
import com.societyportal.backend.repository.AuditLogRepository;
import com.societyportal.backend.repository.UserRepository;
import com.societyportal.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    // Stricter than the app-wide app.file-storage.max-file-size-mb default (25MB) - a profile
    // photo is a small headshot image, not a scanned document.
    private static final long PHOTO_MAX_SIZE_BYTES = 2L * 1024 * 1024;

    public ProfileDtos.ProfileResponse myProfile() {
        User user = userRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Profile not found"));
        return ProfileDtos.ProfileResponse.builder()
                .id(user.getId()).name(user.getName()).email(user.getEmail()).mobile(user.getMobile())
                .flatNo(user.getFlatNo()).block(user.getBlock()).residentType(user.getResidentType().name())
                .approvedOn(user.getApprovedOn())
                .photoUrl(user.getPhotoPath() != null ? "/api/files/photo/" + user.getId() : null)
                .build();
    }

    @Transactional
    public void updateProfile(ProfileDtos.UpdateProfileRequest req) {
        User user = userRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Profile not found"));
        user.setName(req.getName());
        userRepository.save(user);
        auditService.log("PROFILE", "UPDATE", user.getId().toString(), null, null);
    }

    @Transactional
    public void updatePhoto(MultipartFile photo) {
        User user = userRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Profile not found"));
        var stored = fileStorageService.store(photo, "profile-photos", PHOTO_MAX_SIZE_BYTES);
        user.setPhotoPath(stored.relativePath());
        userRepository.save(user);
    }

    @Transactional
    public void requestCorrection(String message) {
        User user = userRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Profile not found"));
        auditService.log("PROFILE", "CORRECTION_REQUEST", user.getId().toString(), null, message);
        // Surfaces to admins via the audit log / can be extended into a dedicated ticket queue.
    }

    /** FR-US-47: last 10 login sessions, sourced from the audit log's own LOGIN entries. */
    public List<ProfileDtos.LoginHistoryItem> loginHistory() {
        UUID userId = CurrentUser.get().getId();
        return auditLogRepository.findTop10ByModuleAndActionAndActorIdOrderByOccurredAtDesc("AUTH", "LOGIN", userId)
                .stream()
                .map(a -> ProfileDtos.LoginHistoryItem.builder()
                        .timestamp(a.getOccurredAt()).ip(a.getIp()).status("Success").build())
                .toList();
    }
}

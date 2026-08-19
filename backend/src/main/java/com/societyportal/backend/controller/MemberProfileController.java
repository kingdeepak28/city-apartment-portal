// Author: deepak.maheshwari

package com.societyportal.backend.controller;

import com.societyportal.backend.dto.ProfileDtos;
import com.societyportal.backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member/profile")
@RequiredArgsConstructor
public class MemberProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileDtos.ProfileResponse me() {
        return profileService.myProfile();
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> update(@Valid @RequestBody ProfileDtos.UpdateProfileRequest req) {
        profileService.updateProfile(req);
        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }

    @PostMapping(value = "/photo", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> updatePhoto(@RequestPart("photo") MultipartFile photo) {
        profileService.updatePhoto(photo);
        return ResponseEntity.ok(Map.of("message", "Photo updated"));
    }

    @PostMapping("/request-correction")
    public ResponseEntity<Map<String, String>> requestCorrection(@Valid @RequestBody ProfileDtos.CorrectionRequest req) {
        profileService.requestCorrection(req.getMessage());
        return ResponseEntity.ok(Map.of("message", "Correction request sent to the administrator"));
    }

    @GetMapping("/login-history")
    public List<ProfileDtos.LoginHistoryItem> loginHistory() {
        return profileService.loginHistory();
    }
}

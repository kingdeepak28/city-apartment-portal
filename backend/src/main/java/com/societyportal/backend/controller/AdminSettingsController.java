// Author: deepak.maheshwari

package com.societyportal.backend.controller;

import com.societyportal.backend.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final SettingService settingService;

    @GetMapping
    public Map<String, String> all() {
        return settingService.allSettings();
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> update(@RequestBody Map<String, String> updates) {
        settingService.update(updates);
        return ResponseEntity.ok(Map.of("message", "Settings updated"));
    }
}

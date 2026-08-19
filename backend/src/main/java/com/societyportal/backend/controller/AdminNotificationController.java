// Author: deepak.maheshwari

package com.societyportal.backend.controller;

import com.societyportal.backend.dto.NotificationDtos;
import com.societyportal.backend.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, String> broadcast(@Valid @RequestBody NotificationDtos.BroadcastRequest req) {
        UUID batchId = notificationService.broadcast(req);
        return Map.of("message", "Broadcast sent", "batchId", batchId.toString());
    }

    @GetMapping("/log")
    public Page<NotificationDtos.NotificationLogItem> log(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return notificationService.log(PageRequest.of(page, size));
    }

    @PostMapping("/log/{batchId}/resend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, String> resend(@PathVariable UUID batchId) {
        int count = notificationService.resendFailed(batchId);
        return Map.of("message", count + " failed notification(s) re-sent");
    }
}

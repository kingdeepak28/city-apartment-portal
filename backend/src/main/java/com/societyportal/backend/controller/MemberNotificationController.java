package com.societyportal.backend.controller;

import com.societyportal.backend.domain.User;
import com.societyportal.backend.dto.NotificationDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.UserRepository;
import com.societyportal.backend.security.CurrentUser;
import com.societyportal.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/member/notifications")
@RequiredArgsConstructor
public class MemberNotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public Page<NotificationDtos.NotificationItem> list(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.listForMember(CurrentUser.get().getId(), unreadOnly, PageRequest.of(page, size));
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCountResponse unreadCount() {
        return new NotificationDtos.UnreadCountResponse(notificationService.unreadCount(CurrentUser.get().getId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable UUID id) {
        notificationService.markRead(id, CurrentUser.get().getId());
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead() {
        notificationService.markAllRead(CurrentUser.get().getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        notificationService.delete(id, CurrentUser.get().getId());
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @GetMapping("/preferences")
    public Map<String, Boolean> preferences() {
        return notificationService.getPreferences(currentUser());
    }

    @PutMapping("/preferences")
    public ResponseEntity<Map<String, String>> updatePreferences(@RequestBody Map<String, Boolean> updates) {
        notificationService.updatePreferences(currentUser(), updates);
        return ResponseEntity.ok(Map.of("message", "Preferences updated"));
    }

    private User currentUser() {
        return userRepository.findById(CurrentUser.get().getId()).orElseThrow(() -> ApiException.notFound("User not found"));
    }
}

package com.societyportal.backend.controller;

import com.societyportal.backend.dto.DashboardDtos;
import com.societyportal.backend.service.DashboardService;
import com.societyportal.backend.service.NotificationService;
import com.societyportal.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final NotificationService notificationService;

    @GetMapping
    public DashboardDtos.AdminDashboardResponse dashboard() {
        return dashboardService.adminDashboard();
    }

    @GetMapping("/unread-notifications")
    public Map<String, Long> unreadNotifications() {
        return Map.of("unreadCount", notificationService.unreadCountAdmin(CurrentUser.get().getId()));
    }
}

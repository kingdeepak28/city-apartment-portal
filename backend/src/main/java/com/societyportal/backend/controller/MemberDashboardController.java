// Author: deepak.maheshwari

package com.societyportal.backend.controller;

import com.societyportal.backend.dto.DashboardDtos;
import com.societyportal.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/dashboard")
@RequiredArgsConstructor
public class MemberDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardDtos.MemberDashboardResponse dashboard() {
        return dashboardService.memberDashboard();
    }
}

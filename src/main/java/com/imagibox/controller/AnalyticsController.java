package com.imagibox.controller;

import com.imagibox.dto.response.DashboardResponseDto;
import com.imagibox.service.AnalyticsService;
import com.imagibox.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Parent dashboard analytics endpoints")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get dashboard analytics for parent")
    public ResponseEntity<DashboardResponseDto> getDashboard() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getDashboard(userId));
    }

    @GetMapping("/mood-distribution")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get mood distribution for a user")
    public ResponseEntity<Map<String, Long>> getMoodDistribution() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getMoodDistribution(userId));
    }
}

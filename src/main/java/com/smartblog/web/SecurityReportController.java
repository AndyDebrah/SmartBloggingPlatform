package com.smartblog.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartblog.auth.SecurityEventMetricsService;
import com.smartblog.core.dto.ApiResponse;

@RestController
@RequestMapping("/admin")
public class SecurityReportController {

    private final SecurityEventMetricsService securityEvents;

    public SecurityReportController(SecurityEventMetricsService securityEvents) {
        this.securityEvents = securityEvents;
    }

    @GetMapping("/security-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityReport() {
        return ResponseEntity.ok(ApiResponse.success("Security report generated", securityEvents.snapshot()));
    }
}

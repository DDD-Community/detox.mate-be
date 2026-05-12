package com.detoxmate.admin.screentimeocr.controller;

import com.detoxmate.admin.service.AdminAuthorizationService;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.dto.AdminScreenTimeOcrErrorReportListResponse;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateResponse;
import com.detoxmate.screentimeocr.service.ScreenTimeOcrErrorReportAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminScreenTimeOcrErrorReportController {

    private final ScreenTimeOcrErrorReportAdminService reportAdminService;
    private final AdminAuthorizationService adminAuthorizationService;

    @GetMapping("/admin/screen-time-ocr-error-reports")
    public AdminScreenTimeOcrErrorReportListResponse list(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
            @RequestParam(defaultValue = "PENDING") ScreenTimeOcrErrorReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        adminAuthorizationService.requireAdmin(adminToken);
        return reportAdminService.list(status, PageRequest.of(page, size));
    }

    @PatchMapping("/admin/screen-time-ocr-error-reports/{reportId}")
    public ScreenTimeOcrErrorReportUpdateResponse update(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
            @PathVariable Long reportId,
            @Valid @RequestBody ScreenTimeOcrErrorReportUpdateRequest request
    ) {
        String adminActor = adminAuthorizationService.requireAdmin(adminToken);
        return reportAdminService.update(adminActor, reportId, request);
    }
}

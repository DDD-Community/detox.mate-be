package com.detoxmate.admin.screentimeocr.controller;

import com.detoxmate.admin.service.AdminAuthorizationService;
import com.detoxmate.auth.CurrentUser;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminScreenTimeOcrErrorReportController {

    private final ScreenTimeOcrErrorReportAdminService reportAdminService;
    private final AdminAuthorizationService adminAuthorizationService;

    @GetMapping("/admin/screen-time-ocr-error-reports")
    public AdminScreenTimeOcrErrorReportListResponse list(
            CurrentUser currentUser,
            @RequestParam(defaultValue = "PENDING") ScreenTimeOcrErrorReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        adminAuthorizationService.requireAdmin(currentUser.id());
        return reportAdminService.list(status, PageRequest.of(page, size));
    }

    @PatchMapping("/admin/screen-time-ocr-error-reports/{reportId}")
    public ScreenTimeOcrErrorReportUpdateResponse update(
            CurrentUser currentUser,
            @PathVariable Long reportId,
            @Valid @RequestBody ScreenTimeOcrErrorReportUpdateRequest request
    ) {
        adminAuthorizationService.requireAdmin(currentUser.id());
        return reportAdminService.update(currentUser.id(), reportId, request);
    }
}

package com.detoxmate.screentimeocr.controller;

import com.detoxmate.auth.CurrentUser;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportCreateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportCreateResponse;
import com.detoxmate.screentimeocr.service.ScreenTimeOcrErrorReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ScreenTimeOcrErrorReportController {

    private final ScreenTimeOcrErrorReportService reportService;

    @PostMapping("/screen-time-ocr-error-reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenTimeOcrErrorReportCreateResponse create(
            CurrentUser currentUser,
            @Valid @RequestBody ScreenTimeOcrErrorReportCreateRequest request
    ) {
        return reportService.create(currentUser.id(), request);
    }
}

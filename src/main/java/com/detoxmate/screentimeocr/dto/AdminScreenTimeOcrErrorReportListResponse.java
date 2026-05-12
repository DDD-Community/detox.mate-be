package com.detoxmate.screentimeocr.dto;

import java.util.List;

public record AdminScreenTimeOcrErrorReportListResponse(
        List<AdminScreenTimeOcrErrorReportItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

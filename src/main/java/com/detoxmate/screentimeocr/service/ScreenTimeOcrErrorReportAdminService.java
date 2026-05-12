package com.detoxmate.screentimeocr.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReport;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.dto.AdminScreenTimeOcrErrorReportItemResponse;
import com.detoxmate.screentimeocr.dto.AdminScreenTimeOcrErrorReportListResponse;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateAction;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateResponse;
import com.detoxmate.screentimeocr.repository.ScreenTimeOcrErrorReportRepository;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScreenTimeOcrErrorReportAdminService {

    private static final String REPORT_NOT_FOUND_MESSAGE = "OCR 오류 신고를 찾을 수 없습니다.";
    private static final String REPORT_ALREADY_RESOLVED_MESSAGE = "이미 처리된 OCR 오류 신고입니다.";
    private static final String ACTION_REQUIRED_MESSAGE = "처리 동작은 필수입니다.";
    private static final String CORRECTED_TOTAL_USED_MINUTES_REQUIRED_MESSAGE = "수정 총 사용 시간은 필수입니다.";
    private static final String ACTIVITY_RECORD_REQUIRED_MESSAGE = "수정하려면 활동 기록이 연결되어 있어야 합니다.";
    private static final String ACTIVITY_RECORD_NOT_FOUND_MESSAGE = "활동 기록을 찾을 수 없습니다.";
    private static final String CHALLENGE_RECORD_NOT_FOUND_MESSAGE = "챌린지 기록을 찾을 수 없습니다.";

    private final ScreenTimeOcrErrorReportRepository reportRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminScreenTimeOcrErrorReportListResponse list(
            ScreenTimeOcrErrorReportStatus status,
            Pageable pageable
    ) {
        Page<ScreenTimeOcrErrorReport> reportPage = reportRepository.findAllByStatusOrderByCreatedAtAscIdAsc(
                status,
                pageable
        );

        return new AdminScreenTimeOcrErrorReportListResponse(
                reportPage.map(this::toItemResponse).toList(),
                reportPage.getNumber(),
                reportPage.getSize(),
                reportPage.getTotalElements(),
                reportPage.getTotalPages()
        );
    }

    @Transactional
    public ScreenTimeOcrErrorReportUpdateResponse update(
            Long adminUserId,
            Long reportId,
            ScreenTimeOcrErrorReportUpdateRequest request
    ) {
        ScreenTimeOcrErrorReport report = findReport(reportId);
        validatePending(report);

        switch (validatedAction(request)) {
            case CORRECT -> correct(adminUserId, report, request);
            case REJECT -> report.reject(adminUserId, now(), request.adminNote());
        }

        return toUpdateResponse(report);
    }

    private AdminScreenTimeOcrErrorReportItemResponse toItemResponse(ScreenTimeOcrErrorReport report) {
        return new AdminScreenTimeOcrErrorReportItemResponse(
                report.getId(),
                report.getUserId(),
                report.getUser().getPublicDisplayName(),
                report.getActivityRecordId(),
                report.getGroupChallengeParticipantId(),
                report.getRecordDate(),
                imageReadUrlBuilder.build(report.getImageObjectKey()),
                report.getOcrTotalUsedMinutes(),
                report.getCorrectedTotalUsedMinutes(),
                report.getStatus(),
                report.getAdminNote(),
                report.getResolvedByUserId(),
                report.getResolvedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private void correct(
            Long adminUserId,
            ScreenTimeOcrErrorReport report,
            ScreenTimeOcrErrorReportUpdateRequest request
    ) {
        validateCorrectRequest(report, request);
        ActivityRecord activityRecord = activityRecordRepository.findByIdWithDetails(report.getActivityRecordId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACTIVITY_RECORD_NOT_FOUND_MESSAGE));

        boolean allAchieved = activityRecord.correctTotalUsageMinutes(request.correctedTotalUsedMinutes());
        ChallengeRecord challengeRecord = challengeRecordRepository.findByActivityRecordId(activityRecord.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, CHALLENGE_RECORD_NOT_FOUND_MESSAGE));

        challengeRecord.correctCertificationResult(certificationResult(allAchieved));
        report.markCorrected(
                request.correctedTotalUsedMinutes(),
                adminUserId,
                now(),
                request.adminNote()
        );
    }

    private void validateCorrectRequest(
            ScreenTimeOcrErrorReport report,
            ScreenTimeOcrErrorReportUpdateRequest request
    ) {
        if (request.correctedTotalUsedMinutes() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CORRECTED_TOTAL_USED_MINUTES_REQUIRED_MESSAGE);
        }

        if (!report.hasActivityRecord()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ACTIVITY_RECORD_REQUIRED_MESSAGE);
        }
    }

    private ScreenTimeOcrErrorReport findReport(Long reportId) {
        return reportRepository.findWithUserAndActivityRecordById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, REPORT_NOT_FOUND_MESSAGE));
    }

    private void validatePending(ScreenTimeOcrErrorReport report) {
        if (!report.isPending()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, REPORT_ALREADY_RESOLVED_MESSAGE);
        }
    }

    private ScreenTimeOcrErrorReportUpdateAction validatedAction(ScreenTimeOcrErrorReportUpdateRequest request) {
        if (request.action() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ACTION_REQUIRED_MESSAGE);
        }

        return request.action();
    }

    private ChallengeRecordCertificationResult certificationResult(boolean allAchieved) {
        if (allAchieved) {
            return ChallengeRecordCertificationResult.SUCCESS;
        }
        return ChallengeRecordCertificationResult.FAIL;
    }

    private ScreenTimeOcrErrorReportUpdateResponse toUpdateResponse(ScreenTimeOcrErrorReport report) {
        return new ScreenTimeOcrErrorReportUpdateResponse(
                report.getId(),
                report.getStatus(),
                report.getOcrTotalUsedMinutes(),
                report.getCorrectedTotalUsedMinutes(),
                report.getAdminNote(),
                report.getResolvedByUserId(),
                report.getResolvedAt()
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}

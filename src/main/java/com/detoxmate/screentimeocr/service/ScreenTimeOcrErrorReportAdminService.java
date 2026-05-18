package com.detoxmate.screentimeocr.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReport;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.dto.AdminScreenTimeOcrErrorReportItemResponse;
import com.detoxmate.screentimeocr.dto.AdminScreenTimeOcrErrorReportListResponse;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportAdminListRow;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateAction;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateResponse;
import com.detoxmate.screentimeocr.repository.ScreenTimeOcrErrorReportRepository;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenTimeOcrErrorReportAdminService {

    private static final String REPORT_NOT_FOUND_MESSAGE = "OCR 오류 신고를 찾을 수 없습니다.";
    private static final String REPORT_ALREADY_RESOLVED_MESSAGE = "이미 처리된 OCR 오류 신고입니다.";
    private static final String ACTION_REQUIRED_MESSAGE = "처리 동작은 필수입니다.";
    private static final String CORRECTED_TOTAL_USED_MINUTES_REQUIRED_MESSAGE = "수정 총 사용 시간은 필수입니다.";
    private static final String ACTIVITY_RECORD_NOT_FOUND_MESSAGE = "활동 기록을 찾을 수 없습니다.";
    private static final String GROUP_MEMBER_NOT_FOUND_MESSAGE = "그룹 멤버를 찾을 수 없습니다.";
    private static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    private static final String TOTAL_USAGE_GOAL_NOT_FOUND_MESSAGE = "사용 가능한 총 사용 시간 목표가 없습니다.";

    private final ScreenTimeOcrErrorReportRepository reportRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ChallengeRecordService challengeRecordService;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminScreenTimeOcrErrorReportListResponse list(
            ScreenTimeOcrErrorReportStatus status,
            Pageable pageable
    ) {
        Page<ScreenTimeOcrErrorReportAdminListRow> reportPage = reportRepository.findAdminListRowsByStatus(
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
            Long reportId,
            ScreenTimeOcrErrorReportUpdateRequest request
    ) {
        ScreenTimeOcrErrorReport report = findReport(reportId);
        validatePending(report);

        switch (validatedAction(request)) {
            case CORRECT -> correct(report, request);
            case REJECT -> report.reject(now(), request.adminNote());
        }

        return toUpdateResponse(report);
    }

    private AdminScreenTimeOcrErrorReportItemResponse toItemResponse(ScreenTimeOcrErrorReportAdminListRow report) {
        return new AdminScreenTimeOcrErrorReportItemResponse(
                report.id(),
                report.userId(),
                report.userDisplayName(),
                report.activityRecordId(),
                report.groupChallengeParticipantId(),
                report.recordDate(),
                imageReadUrlBuilder.build(report.imageObjectKey()),
                report.ocrTotalUsedMinutes(),
                report.correctedTotalUsedMinutes(),
                report.status(),
                report.adminNote(),
                report.resolvedAt(),
                report.createdAt(),
                report.updatedAt()
        );
    }

    private void correct(
            ScreenTimeOcrErrorReport report,
            ScreenTimeOcrErrorReportUpdateRequest request
    ) {
        validateCorrectRequest(report, request);
        ChallengeRecord challengeRecord = findOrCreateChallengeRecord(report);
        ActivityRecord activityRecord = resolveActivityRecord(report, challengeRecord, request.correctedTotalUsedMinutes());

        boolean allAchieved = activityRecord.correctTotalUsageMinutes(request.correctedTotalUsedMinutes());
        applyChallengeRecord(challengeRecord, activityRecord, allAchieved);
        report.linkActivityRecord(activityRecord);
        report.markCorrected(
                request.correctedTotalUsedMinutes(),
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
    }

    private ActivityRecord findActivityRecord(Long activityRecordId) {
        return activityRecordRepository.findByIdWithDetails(activityRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACTIVITY_RECORD_NOT_FOUND_MESSAGE));
    }

    private ActivityRecord createActivityRecord(ScreenTimeOcrErrorReport report, Integer correctedTotalUsedMinutes) {
        GroupChallengeParticipant participant = report.getGroupChallengeParticipant();
        User user = findUser(participant);
        UserUsageGoalTime totalUsageGoal = findLatestTotalUsageGoal(user.getId(), report.getRecordDate());
        boolean achieved = correctedTotalUsedMinutes <= totalUsageGoal.getGoalMinutes();

        ActivityRecord activityRecord = ActivityRecord.create(
                user,
                participant,
                report.getImageObjectKey(),
                null
        );
        activityRecord.addDetail(totalUsageGoal, correctedTotalUsedMinutes, achieved);
        ActivityRecord savedActivityRecord = activityRecordRepository.saveAndFlush(activityRecord);
        report.linkActivityRecord(savedActivityRecord);
        return savedActivityRecord;
    }

    private User findUser(GroupChallengeParticipant participant) {
        GroupMember groupMember = groupMemberRepository.findById(participant.getGroupMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, GROUP_MEMBER_NOT_FOUND_MESSAGE));
        return userRepository.findById(groupMember.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE));
    }

    private UserUsageGoalTime findLatestTotalUsageGoal(Long userId, LocalDate recordDate) {
        return userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeInAndCreatedAtBefore(
                        userId,
                        List.of(UsageGoalTypeCode.TOTAL_USAGE),
                        recordDate.atStartOfDay()
                )
                .stream()
                .max(Comparator.comparing(UserUsageGoalTime::getCreatedAt))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, TOTAL_USAGE_GOAL_NOT_FOUND_MESSAGE));
    }

    private void applyChallengeRecord(
            ChallengeRecord challengeRecord,
            ActivityRecord activityRecord,
            boolean allAchieved
    ) {
        if (challengeRecord.isCertified()) {
            challengeRecord.correctCertificationResult(certificationResult(allAchieved));
            return;
        }

        challengeRecord.certify(
                activityRecord.getId(),
                activityRecord.getGroupChallengeParticipant().getId(),
                certificationResult(allAchieved)
        );
    }

    private ChallengeRecord findOrCreateChallengeRecord(ScreenTimeOcrErrorReport report) {
        GroupChallengeParticipant participant = report.getGroupChallengeParticipant();
        return challengeRecordService.create(
                participant.getGroupChallengeId(),
                participant.getId(),
                report.getRecordDate()
        );
    }

    private ActivityRecord resolveActivityRecord(
            ScreenTimeOcrErrorReport report,
            ChallengeRecord challengeRecord,
            Integer correctedTotalUsedMinutes
    ) {
        if (challengeRecord.getActivityRecordId() != null) {
            return findActivityRecord(challengeRecord.getActivityRecordId());
        }

        if (report.hasActivityRecord()) {
            return findActivityRecord(report.getActivityRecordId());
        }

        return createActivityRecord(report, correctedTotalUsedMinutes);
    }

    private ScreenTimeOcrErrorReport findReport(Long reportId) {
        return reportRepository.findWithActivityRecordById(reportId)
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
                report.getResolvedAt()
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}

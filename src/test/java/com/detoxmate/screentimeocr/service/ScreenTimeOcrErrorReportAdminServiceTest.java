package com.detoxmate.screentimeocr.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.domain.ChallengeRecordStatus;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReport;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateAction;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateResponse;
import com.detoxmate.screentimeocr.repository.ScreenTimeOcrErrorReportRepository;
import com.detoxmate.upload.config.StorageProperties;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScreenTimeOcrErrorReportAdminServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(
            LocalDateTime.of(2026, 5, 13, 10, 0).atZone(KST).toInstant(),
            KST
    );

    private final ScreenTimeOcrErrorReportRepository reportRepository = mock(ScreenTimeOcrErrorReportRepository.class);
    private final ActivityRecordRepository activityRecordRepository = mock(ActivityRecordRepository.class);
    private final ChallengeRecordRepository challengeRecordRepository = mock(ChallengeRecordRepository.class);
    private final ImageReadUrlBuilder imageReadUrlBuilder =
            new ImageReadUrlBuilder(new StorageProperties("https://example.com/media"));
    private final ScreenTimeOcrErrorReportAdminService adminService = new ScreenTimeOcrErrorReportAdminService(
            reportRepository,
            activityRecordRepository,
            challengeRecordRepository,
            imageReadUrlBuilder,
            CLOCK
    );

    @Test
    void CORRECT는_report와_TOTAL_USAGE_인증값과_challenge_record_상태를_수정한다() {
        User user = user(1L);
        ActivityRecord activityRecord = activityRecord(123L, user, 10L);
        ChallengeRecord challengeRecord = ChallengeRecord.create(200L, 10L, LocalDate.of(2026, 5, 12));
        challengeRecord.certify(123L, 10L, ChallengeRecordCertificationResult.FAIL);
        ScreenTimeOcrErrorReport report = report(555L, user, activityRecord);

        when(reportRepository.findWithUserAndActivityRecordById(555L)).thenReturn(Optional.of(report));
        when(activityRecordRepository.findByIdWithDetails(123L)).thenReturn(Optional.of(activityRecord));
        when(challengeRecordRepository.findByActivityRecordId(123L)).thenReturn(Optional.of(challengeRecord));

        ScreenTimeOcrErrorReportUpdateResponse response = adminService.update(
                "TEST_ADMIN",
                555L,
                new ScreenTimeOcrErrorReportUpdateRequest(
                        ScreenTimeOcrErrorReportUpdateAction.CORRECT,
                        100,
                        "스크린샷 기준 총 사용시간 1시간 40분"
                )
        );

        ActivityRecordDetail totalUsageDetail = activityRecord.getDetails().getFirst();
        assertThat(totalUsageDetail.getUseMinutes()).isEqualTo(100);
        assertThat(totalUsageDetail.isAchieved()).isTrue();
        assertThat(challengeRecord.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_SUCCESS);
        assertThat(report.getStatus()).isEqualTo(ScreenTimeOcrErrorReportStatus.CORRECTED);
        assertThat(response.correctedTotalUsedMinutes()).isEqualTo(100);
        assertThat(response.resolvedBy()).isEqualTo("TEST_ADMIN");
        assertThat(response.resolvedAt()).isEqualTo(LocalDateTime.of(2026, 5, 13, 10, 0));
    }

    @Test
    void REJECT는_report만_반려하고_인증값은_수정하지_않는다() {
        User user = user(1L);
        ActivityRecord activityRecord = activityRecord(123L, user, 10L);
        ScreenTimeOcrErrorReport report = report(555L, user, activityRecord);

        when(reportRepository.findWithUserAndActivityRecordById(555L)).thenReturn(Optional.of(report));

        ScreenTimeOcrErrorReportUpdateResponse response = adminService.update(
                "TEST_ADMIN",
                555L,
                new ScreenTimeOcrErrorReportUpdateRequest(
                        ScreenTimeOcrErrorReportUpdateAction.REJECT,
                        null,
                        "OCR 오류 아님"
                )
        );

        ActivityRecordDetail totalUsageDetail = activityRecord.getDetails().getFirst();
        assertThat(totalUsageDetail.getUseMinutes()).isEqualTo(180);
        assertThat(totalUsageDetail.isAchieved()).isFalse();
        assertThat(report.getStatus()).isEqualTo(ScreenTimeOcrErrorReportStatus.REJECTED);
        assertThat(response.status()).isEqualTo(ScreenTimeOcrErrorReportStatus.REJECTED);
    }

    private ScreenTimeOcrErrorReport report(Long id, User user, ActivityRecord activityRecord) {
        ScreenTimeOcrErrorReport report = ScreenTimeOcrErrorReport.create(
                user,
                activityRecord,
                10L,
                LocalDate.of(2026, 5, 12),
                "screen-time-ocr-reports/1/2026/05/sample.png",
                180
        );
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    private ActivityRecord activityRecord(Long id, User user, Long participantId) {
        GroupChallengeParticipant participant = GroupChallengeParticipant.join(100L, 200L);
        ReflectionTestUtils.setField(participant, "id", participantId);
        ActivityRecord activityRecord = ActivityRecord.create(user, participant, "activity-records/sample.png", null);
        ReflectionTestUtils.setField(activityRecord, "id", id);
        activityRecord.addDetail(userUsageGoalTime(user, UsageGoalTypeCode.TOTAL_USAGE, 120), 180, false);
        return activityRecord;
    }

    private UserUsageGoalTime userUsageGoalTime(User user, UsageGoalTypeCode code, int goalMinutes) {
        UsageGoalType usageGoalType = UsageGoalType.create(code.ordinal() + 1L, code);
        return UserUsageGoalTime.create(user, usageGoalType, goalMinutes);
    }

    private User user(Long id) {
        User user = User.createNew("tester");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

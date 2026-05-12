package com.detoxmate.screentimeocr.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.domain.ChallengeRecordStatus;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReport;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateAction;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportUpdateResponse;
import com.detoxmate.screentimeocr.repository.ScreenTimeOcrErrorReportRepository;
import com.detoxmate.upload.config.StorageProperties;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScreenTimeOcrErrorReportAdminServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(
            LocalDateTime.of(2026, 5, 13, 10, 0).atZone(KST).toInstant(),
            KST
    );

    private final ScreenTimeOcrErrorReportRepository reportRepository = mock(ScreenTimeOcrErrorReportRepository.class);
    private final ActivityRecordRepository activityRecordRepository = mock(ActivityRecordRepository.class);
    private final ChallengeRecordService challengeRecordService = mock(ChallengeRecordService.class);
    private final GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository = mock(UserUsageGoalTimeRepository.class);
    private final ImageReadUrlBuilder imageReadUrlBuilder =
            new ImageReadUrlBuilder(new StorageProperties("https://example.com/media"));
    private final ScreenTimeOcrErrorReportAdminService adminService = new ScreenTimeOcrErrorReportAdminService(
            reportRepository,
            activityRecordRepository,
            challengeRecordService,
            groupMemberRepository,
            userRepository,
            userUsageGoalTimeRepository,
            imageReadUrlBuilder,
            CLOCK
    );

    @Test
    void CORRECT는_report와_TOTAL_USAGE_인증값과_challenge_record_상태를_수정한다() {
        User user = user(1L);
        ActivityRecord activityRecord = activityRecord(123L, user, 10L);
        ChallengeRecord challengeRecord = ChallengeRecord.create(200L, 10L, LocalDate.of(2026, 5, 12));
        challengeRecord.certify(123L, 10L, ChallengeRecordCertificationResult.FAIL);
        ScreenTimeOcrErrorReport report = report(555L, activityRecord);

        when(reportRepository.findWithActivityRecordById(555L)).thenReturn(Optional.of(report));
        when(challengeRecordService.create(200L, 10L, LocalDate.of(2026, 5, 12))).thenReturn(challengeRecord);
        when(activityRecordRepository.findByIdWithDetails(123L)).thenReturn(Optional.of(activityRecord));

        ScreenTimeOcrErrorReportUpdateResponse response = adminService.update(
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
        assertThat(response.resolvedAt()).isEqualTo(LocalDateTime.of(2026, 5, 13, 10, 0));
    }

    @Test
    void CORRECT는_report에_activity_record_id가_없어도_참가자와_날짜의_기존_인증기록을_수정한다() {
        User user = user(1L);
        GroupChallengeParticipant participant = participant(10L, 100L, 200L);
        ActivityRecord activityRecord = ActivityRecord.create(user, participant, "activity-records/sample.png", null);
        ReflectionTestUtils.setField(activityRecord, "id", 123L);
        activityRecord.addDetail(userUsageGoalTime(user, UsageGoalTypeCode.TOTAL_USAGE, 120), 180, false);
        ChallengeRecord challengeRecord = ChallengeRecord.create(200L, 10L, LocalDate.of(2026, 5, 12));
        challengeRecord.certify(123L, 10L, ChallengeRecordCertificationResult.FAIL);
        ScreenTimeOcrErrorReport report = report(555L, participant, null);

        when(reportRepository.findWithActivityRecordById(555L)).thenReturn(Optional.of(report));
        when(challengeRecordService.create(200L, 10L, LocalDate.of(2026, 5, 12))).thenReturn(challengeRecord);
        when(activityRecordRepository.findByIdWithDetails(123L)).thenReturn(Optional.of(activityRecord));

        adminService.update(
                555L,
                new ScreenTimeOcrErrorReportUpdateRequest(
                        ScreenTimeOcrErrorReportUpdateAction.CORRECT,
                        100,
                        "스크린샷 기준 총 사용시간 1시간 40분"
                )
        );

        ActivityRecordDetail totalUsageDetail = activityRecord.getDetails().getFirst();
        assertThat(report.getActivityRecordId()).isEqualTo(123L);
        assertThat(totalUsageDetail.getUseMinutes()).isEqualTo(100);
        assertThat(totalUsageDetail.isAchieved()).isTrue();
        assertThat(challengeRecord.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_SUCCESS);
        verify(activityRecordRepository, never()).saveAndFlush(any(ActivityRecord.class));
    }

    @Test
    void CORRECT는_activity_record_id가_없는_report를_참가자와_날짜로_인증한다() {
        User user = user(1L);
        GroupChallengeParticipant participant = participant(10L, 100L, 200L);
        GroupMember groupMember = GroupMember.createMember(user.getId(), 300L);
        UserUsageGoalTime totalUsageGoal = userUsageGoalTime(user, UsageGoalTypeCode.TOTAL_USAGE, 120);
        ChallengeRecord challengeRecord = ChallengeRecord.create(200L, 10L, LocalDate.of(2026, 5, 12));
        ScreenTimeOcrErrorReport report = report(555L, participant, null);

        when(reportRepository.findWithActivityRecordById(555L)).thenReturn(Optional.of(report));
        when(challengeRecordService.create(200L, 10L, LocalDate.of(2026, 5, 12))).thenReturn(challengeRecord);
        when(groupMemberRepository.findById(100L)).thenReturn(Optional.of(groupMember));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userUsageGoalTimeRepository.findAllByUser_IdAndUsageGoalType_CodeInAndCreatedAtBefore(
                1L,
                List.of(UsageGoalTypeCode.TOTAL_USAGE),
                LocalDate.of(2026, 5, 12).atStartOfDay()
        )).thenReturn(List.of(totalUsageGoal));
        when(activityRecordRepository.saveAndFlush(any(ActivityRecord.class))).thenAnswer(invocation -> {
            ActivityRecord activityRecord = invocation.getArgument(0);
            ReflectionTestUtils.setField(activityRecord, "id", 123L);
            return activityRecord;
        });

        ScreenTimeOcrErrorReportUpdateResponse response = adminService.update(
                555L,
                new ScreenTimeOcrErrorReportUpdateRequest(
                        ScreenTimeOcrErrorReportUpdateAction.CORRECT,
                        100,
                        "스크린샷 기준 총 사용시간 1시간 40분"
                )
        );

        assertThat(report.getActivityRecordId()).isEqualTo(123L);
        assertThat(report.getStatus()).isEqualTo(ScreenTimeOcrErrorReportStatus.CORRECTED);
        assertThat(challengeRecord.getActivityRecordId()).isEqualTo(123L);
        assertThat(challengeRecord.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_SUCCESS);
        assertThat(response.correctedTotalUsedMinutes()).isEqualTo(100);
    }

    @Test
    void REJECT는_report만_반려하고_인증값은_수정하지_않는다() {
        User user = user(1L);
        ActivityRecord activityRecord = activityRecord(123L, user, 10L);
        ScreenTimeOcrErrorReport report = report(555L, activityRecord);

        when(reportRepository.findWithActivityRecordById(555L)).thenReturn(Optional.of(report));

        ScreenTimeOcrErrorReportUpdateResponse response = adminService.update(
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

    private ScreenTimeOcrErrorReport report(Long id, ActivityRecord activityRecord) {
        return report(id, activityRecord.getGroupChallengeParticipant(), activityRecord);
    }

    private ScreenTimeOcrErrorReport report(
            Long id,
            GroupChallengeParticipant participant,
            ActivityRecord activityRecord
    ) {
        ScreenTimeOcrErrorReport report = ScreenTimeOcrErrorReport.create(
                participant,
                activityRecord,
                LocalDate.of(2026, 5, 12),
                "screen-time-ocr-reports/1/2026/05/sample.png",
                180
        );
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    private ActivityRecord activityRecord(Long id, User user, Long participantId) {
        GroupChallengeParticipant participant = participant(participantId, 100L, 200L);
        ActivityRecord activityRecord = ActivityRecord.create(user, participant, "activity-records/sample.png", null);
        ReflectionTestUtils.setField(activityRecord, "id", id);
        activityRecord.addDetail(userUsageGoalTime(user, UsageGoalTypeCode.TOTAL_USAGE, 120), 180, false);
        return activityRecord;
    }

    private GroupChallengeParticipant participant(Long id, Long groupMemberId, Long groupChallengeId) {
        GroupChallengeParticipant participant = GroupChallengeParticipant.join(groupMemberId, groupChallengeId);
        ReflectionTestUtils.setField(participant, "id", id);
        return participant;
    }

    private UserUsageGoalTime userUsageGoalTime(User user, UsageGoalTypeCode code, int goalMinutes) {
        UsageGoalType usageGoalType = UsageGoalType.create(code.ordinal() + 1L, code);
        UserUsageGoalTime userUsageGoalTime = UserUsageGoalTime.create(user, usageGoalType, goalMinutes);
        ReflectionTestUtils.setField(userUsageGoalTime, "createdAt", LocalDateTime.of(2026, 5, 11, 9, 0));
        return userUsageGoalTime;
    }

    private User user(Long id) {
        User user = User.createNew("tester");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

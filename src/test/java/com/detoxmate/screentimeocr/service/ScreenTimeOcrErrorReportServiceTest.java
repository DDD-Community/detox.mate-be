package com.detoxmate.screentimeocr.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReport;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReportStatus;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportCreateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportCreateResponse;
import com.detoxmate.screentimeocr.repository.ScreenTimeOcrErrorReportRepository;
import com.detoxmate.upload.service.UploadObjectKeyValidator;
import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScreenTimeOcrErrorReportServiceTest {

    private final ScreenTimeOcrErrorReportRepository reportRepository = mock(ScreenTimeOcrErrorReportRepository.class);
    private final ActivityRecordRepository activityRecordRepository = mock(ActivityRecordRepository.class);
    private final GroupChallengeParticipantRepository participantRepository =
            mock(GroupChallengeParticipantRepository.class);
    private final UploadObjectKeyValidator uploadObjectKeyValidator = new UploadObjectKeyValidator();
    private final ScreenTimeOcrErrorReportService reportService = new ScreenTimeOcrErrorReportService(
            reportRepository,
            activityRecordRepository,
            participantRepository,
            uploadObjectKeyValidator
    );

    @Test
    void report를_PENDING_상태로_저장한다() {
        User user = user(1L);
        ActivityRecord activityRecord = activityRecord(123L, user, 10L);
        ScreenTimeOcrErrorReportCreateRequest request = createRequest();

        when(activityRecordRepository.findByIdWithDetails(123L)).thenReturn(Optional.of(activityRecord));
        when(participantRepository.existsActiveByIdAndUserId(10L, 1L)).thenReturn(true);
        when(participantRepository.findById(10L)).thenReturn(Optional.of(activityRecord.getGroupChallengeParticipant()));
        when(reportRepository.save(any(ScreenTimeOcrErrorReport.class))).thenAnswer(invocation -> {
            ScreenTimeOcrErrorReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 555L);
            ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 5, 12, 21, 31));
            return report;
        });

        ScreenTimeOcrErrorReportCreateResponse response = reportService.create(1L, request);

        assertThat(response.id()).isEqualTo(555L);
        assertThat(response.status()).isEqualTo(ScreenTimeOcrErrorReportStatus.PENDING);
        verify(reportRepository).save(any(ScreenTimeOcrErrorReport.class));
    }

    @Test
    void 현재_유저_소유가_아닌_imageObjectKey는_거부한다() {
        ScreenTimeOcrErrorReportCreateRequest request = new ScreenTimeOcrErrorReportCreateRequest(
                123L,
                10L,
                LocalDate.of(2026, 5, 12),
                "screen-time-ocr-reports/2/2026/05/sample.png",
                180
        );

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(ResponseStatusException.class);

        verify(reportRepository, never()).save(any());
    }

    private ScreenTimeOcrErrorReportCreateRequest createRequest() {
        return new ScreenTimeOcrErrorReportCreateRequest(
                123L,
                10L,
                LocalDate.of(2026, 5, 12),
                "screen-time-ocr-reports/1/2026/05/sample.png",
                180
        );
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

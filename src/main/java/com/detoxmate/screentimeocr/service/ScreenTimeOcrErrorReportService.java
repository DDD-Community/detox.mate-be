package com.detoxmate.screentimeocr.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.screentimeocr.domain.ScreenTimeOcrErrorReport;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportCreateRequest;
import com.detoxmate.screentimeocr.dto.ScreenTimeOcrErrorReportCreateResponse;
import com.detoxmate.screentimeocr.repository.ScreenTimeOcrErrorReportRepository;
import com.detoxmate.upload.dto.UploadPurpose;
import com.detoxmate.upload.service.UploadObjectKeyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScreenTimeOcrErrorReportService {

    private static final String IMAGE_OBJECT_KEY_NOT_OWNED_MESSAGE = "신고 이미지 object key를 사용할 수 없습니다.";
    private static final String ACTIVITY_RECORD_NOT_FOUND_MESSAGE = "활동 기록을 찾을 수 없습니다.";
    private static final String ACTIVITY_RECORD_ACCESS_DENIED_MESSAGE = "요청한 활동 기록에 접근할 수 없습니다.";
    private static final String PARTICIPANT_NOT_FOUND_MESSAGE = "그룹 챌린지 참여를 찾을 수 없습니다.";
    private static final String PARTICIPANT_ACCESS_DENIED_MESSAGE = "요청한 그룹 챌린지 참여에 접근할 수 없습니다.";
    private static final String ACTIVITY_RECORD_PARTICIPANT_MISMATCH_MESSAGE =
            "활동 기록과 그룹 챌린지 참여가 일치하지 않습니다.";

    private final ScreenTimeOcrErrorReportRepository reportRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository;
    private final UploadObjectKeyValidator uploadObjectKeyValidator;

    @Transactional
    public ScreenTimeOcrErrorReportCreateResponse create(
            Long userId,
            ScreenTimeOcrErrorReportCreateRequest request
    ) {
        validateImageObjectKey(userId, request.imageObjectKey());
        GroupChallengeParticipant groupChallengeParticipant = findGroupChallengeParticipant(
                userId,
                request.groupChallengeParticipantId()
        );
        ActivityRecord activityRecord = findActivityRecord(userId, request.activityRecordId());
        validateActivityRecordParticipant(groupChallengeParticipant, activityRecord);

        ScreenTimeOcrErrorReport report = ScreenTimeOcrErrorReport.create(
                groupChallengeParticipant,
                activityRecord,
                request.recordDate(),
                request.imageObjectKey(),
                request.ocrTotalUsedMinutes()
        );

        ScreenTimeOcrErrorReport savedReport = reportRepository.save(report);
        return new ScreenTimeOcrErrorReportCreateResponse(
                savedReport.getId(),
                savedReport.getStatus(),
                savedReport.getCreatedAt()
        );
    }

    private void validateImageObjectKey(Long userId, String imageObjectKey) {
        if (!uploadObjectKeyValidator.isOwnedBy(
                userId,
                UploadPurpose.SCREEN_TIME_OCR_REPORT_IMAGE,
                imageObjectKey
        )) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, IMAGE_OBJECT_KEY_NOT_OWNED_MESSAGE);
        }
    }

    private GroupChallengeParticipant findGroupChallengeParticipant(Long userId, Long groupChallengeParticipantId) {
        validateParticipantAccess(userId, groupChallengeParticipantId);
        return groupChallengeParticipantRepository.findById(groupChallengeParticipantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PARTICIPANT_NOT_FOUND_MESSAGE));
    }

    private ActivityRecord findActivityRecord(Long userId, Long activityRecordId) {
        if (activityRecordId == null) {
            return null;
        }

        ActivityRecord activityRecord = activityRecordRepository.findByIdWithDetails(activityRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACTIVITY_RECORD_NOT_FOUND_MESSAGE));

        if (!Objects.equals(activityRecord.getUser().getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ACTIVITY_RECORD_ACCESS_DENIED_MESSAGE);
        }

        return activityRecord;
    }

    private void validateActivityRecordParticipant(
            GroupChallengeParticipant groupChallengeParticipant,
            ActivityRecord activityRecord
    ) {
        if (activityRecord == null) {
            return;
        }

        if (!Objects.equals(
                groupChallengeParticipant.getId(),
                activityRecord.getGroupChallengeParticipant().getId()
        )) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ACTIVITY_RECORD_PARTICIPANT_MISMATCH_MESSAGE);
        }
    }

    private void validateParticipantAccess(Long userId, Long groupChallengeParticipantId) {
        if (!groupChallengeParticipantRepository.existsActiveByIdAndUserId(groupChallengeParticipantId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, PARTICIPANT_ACCESS_DENIED_MESSAGE);
        }
    }
}

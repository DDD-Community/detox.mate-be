package com.detoxmate.challengerecord.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecord.ChallengeRecordErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(
        name = "challenge_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_challenge_record_participant_date",
                columnNames = {"group_challenge_id", "group_challenge_participant_id", "record_date"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_record_id")
    private Long id;

    @Column(name = "group_challenge_id", nullable = false)
    private Long groupChallengeId;

    @Column(name = "group_challenge_participant_id", nullable = false)
    private Long groupChallengeParticipantId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "activity_record_id")
    private Long activityRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChallengeRecordStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ChallengeRecord(Long groupChallengeId, Long groupChallengeParticipantId, LocalDate recordDate) {
        this.groupChallengeId = groupChallengeId;
        this.groupChallengeParticipantId = groupChallengeParticipantId;
        this.recordDate = recordDate;
        this.activityRecordId = null;
        this.status = ChallengeRecordStatus.BEFORE_RECORD;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static ChallengeRecord create(Long groupChallengeId,
                                         Long groupChallengeParticipantId,
                                         LocalDate recordDate) {
        validateCreate(groupChallengeId, groupChallengeParticipantId, recordDate);
        return new ChallengeRecord(groupChallengeId, groupChallengeParticipantId, recordDate);
    }

    public void certify(Long activityRecordId,
                        Long activityRecordParticipantId,
                        ChallengeRecordCertificationResult result) {
        validateCertification(activityRecordId, activityRecordParticipantId, result);

        this.activityRecordId = activityRecordId;
        applyCertificationResult(result);
        this.updatedAt = LocalDateTime.now();
    }

    public void correctCertificationResult(ChallengeRecordCertificationResult result) {
        validateCorrection(result);
        applyCertificationResult(result);
        this.updatedAt = LocalDateTime.now();
    }

    private void applyCertificationResult(ChallengeRecordCertificationResult result) {
        if(result == ChallengeRecordCertificationResult.SUCCESS){
            this.status = ChallengeRecordStatus.AFTER_RECORD_SUCCESS;
        }
        if(result == ChallengeRecordCertificationResult.FAIL){
            this.status = ChallengeRecordStatus.AFTER_RECORD_FAIL;
        }
    }

    public boolean isCertified() {
        return status == ChallengeRecordStatus.AFTER_RECORD_SUCCESS
                || status == ChallengeRecordStatus.AFTER_RECORD_FAIL;
    }

    public boolean isBeforeRecord() {
        return status == ChallengeRecordStatus.BEFORE_RECORD;
    }

    public boolean isCertificationSucceeded() {
        return status == ChallengeRecordStatus.AFTER_RECORD_SUCCESS;
    }

    public boolean isToday(LocalDate today) {
        return Objects.equals(recordDate, today);
    }

    private static void validateCreate(Long groupChallengeId,
                                       Long groupChallengeParticipantId,
                                       LocalDate recordDate) {
        if (groupChallengeId == null) {
            throw new CustomException(ChallengeRecordErrorCode.GROUP_CHALLENGE_REQUIRED);
        }

        if (groupChallengeParticipantId == null) {
            throw new CustomException(ChallengeRecordErrorCode.GROUP_CHALLENGE_PARTICIPANT_REQUIRED);
        }

        if (recordDate == null) {
            throw new CustomException(ChallengeRecordErrorCode.RECORD_DATE_REQUIRED);
        }
    }

    private void validateCertification(Long activityRecordId,
                                       Long activityRecordParticipantId,
                                       ChallengeRecordCertificationResult result) {

        if (activityRecordId == null) {
            throw new CustomException(ChallengeRecordErrorCode.ACTIVITY_RECORD_REQUIRED);
        }

        if (!Objects.equals(groupChallengeParticipantId, activityRecordParticipantId)) {
            throw new CustomException(ChallengeRecordErrorCode.ACTIVITY_RECORD_PARTICIPANT_MISMATCH);
        }

        if (result == null) {
            throw new CustomException(ChallengeRecordErrorCode.CERTIFICATION_RESULT_REQUIRED);
        }

        if (isCertified()) {
            throw new CustomException(ChallengeRecordErrorCode.CHALLENGE_RECORD_ALREADY_CERTIFIED);
        }
    }

    private void validateCorrection(ChallengeRecordCertificationResult result) {
        if (result == null) {
            throw new CustomException(ChallengeRecordErrorCode.CERTIFICATION_RESULT_REQUIRED);
        }

        if (!isCertified()) {
            throw new CustomException(ChallengeRecordErrorCode.ACTIVITY_RECORD_REQUIRED);
        }
    }

    public ChallengeRecordCertificationResult getCertificationResult() {
        if (status == ChallengeRecordStatus.AFTER_RECORD_SUCCESS) {
            return ChallengeRecordCertificationResult.SUCCESS;
        }

        if (status == ChallengeRecordStatus.AFTER_RECORD_FAIL) {
            return ChallengeRecordCertificationResult.FAIL;
        }

        return null;
    }

}

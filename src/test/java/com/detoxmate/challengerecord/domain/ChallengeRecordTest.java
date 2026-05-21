package com.detoxmate.challengerecord.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecord.ChallengeRecordErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ChallengeRecordTest {

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long GROUP_CHALLENGE_PARTICIPANT_ID = 20L;
    private static final Long OTHER_GROUP_CHALLENGE_PARTICIPANT_ID = 30L;
    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 5, 1);

    @Test
    @DisplayName("챌린지 기록은 인증 전 상태로 생성된다")
    void create_initializesBeforeRecordStatus() {
        ChallengeRecord record = ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );

        assertThat(record.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(record.getGroupChallengeParticipantId()).isEqualTo(GROUP_CHALLENGE_PARTICIPANT_ID);
        assertThat(record.getRecordDate()).isEqualTo(RECORD_DATE);
        assertThat(record.getActivityRecordId()).isNull();
        assertThat(record.getStatus()).isEqualTo(ChallengeRecordStatus.BEFORE_RECORD);
        assertThat(record.isBeforeRecord()).isTrue();
        assertThat(record.isCertified()).isFalse();
        assertThat(record.getCreatedAt()).isNotNull();
        assertThat(record.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("목표 달성 인증을 연결하면 성공 인증 상태가 된다")
    void certify_successChangesStatusToAfterRecordSuccess() {
        ChallengeRecord record = createRecord();

        record.certify(
                ACTIVITY_RECORD_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.SUCCESS
        );

        assertThat(record.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(record.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_SUCCESS);
        assertThat(record.isCertified()).isTrue();
        assertThat(record.isBeforeRecord()).isFalse();
        assertThat(record.isCertificationSucceeded()).isTrue();
    }

    @Test
    @DisplayName("목표 미달 인증을 연결하면 실패 인증 상태가 된다")
    void certify_failChangesStatusToAfterRecordFail() {
        ChallengeRecord record = createRecord();

        record.certify(
                ACTIVITY_RECORD_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.FAIL
        );

        assertThat(record.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(record.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_FAIL);
        assertThat(record.isCertified()).isTrue();
        assertThat(record.isBeforeRecord()).isFalse();
        assertThat(record.isCertificationSucceeded()).isFalse();
    }

    @Test
    @DisplayName("기록 날짜가 오늘이면 true를 반환한다")
    void isToday_returnsTrueWhenRecordDateEqualsToday() {
        ChallengeRecord record = createRecord();

        assertThat(record.isToday(RECORD_DATE)).isTrue();
    }

    @Test
    @DisplayName("기록 날짜가 오늘이 아니면 false를 반환한다")
    void isToday_returnsFalseWhenRecordDateDoesNotEqualToday() {
        ChallengeRecord record = createRecord();

        assertThat(record.isToday(RECORD_DATE.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("그룹 챌린지가 없으면 생성할 수 없다")
    void create_failsWhenGroupChallengeIsNull() {
        assertThatThrownBy(() -> ChallengeRecord.create(
                null,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordErrorCode.GROUP_CHALLENGE_REQUIRED);
    }

    @Test
    @DisplayName("챌린지 참여자가 없으면 생성할 수 없다")
    void create_failsWhenParticipantIsNull() {
        assertThatThrownBy(() -> ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                null,
                RECORD_DATE
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordErrorCode.GROUP_CHALLENGE_PARTICIPANT_REQUIRED);
    }

    @Test
    @DisplayName("기록 날짜가 없으면 생성할 수 없다")
    void create_failsWhenRecordDateIsNull() {
        assertThatThrownBy(() -> ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordErrorCode.RECORD_DATE_REQUIRED);
    }

    @Test
    @DisplayName("인증 기록이 없으면 인증 완료 처리할 수 없다")
    void certify_failsWhenActivityRecordIsNull() {
        ChallengeRecord record = createRecord();

        assertThatThrownBy(() -> record.certify(
                null,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.SUCCESS
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordErrorCode.ACTIVITY_RECORD_REQUIRED);
    }

    @Test
    @DisplayName("인증 결과가 없으면 인증 완료 처리할 수 없다")
    void certify_failsWhenCertificationResultIsNull() {
        ChallengeRecord record = createRecord();

        assertThatThrownBy(() -> record.certify(
                ACTIVITY_RECORD_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordErrorCode.CERTIFICATION_RESULT_REQUIRED);
    }

    @Test
    @DisplayName("챌린지 기록과 인증 기록의 참여자가 다르면 인증 완료 처리할 수 없다")
    void certify_failsWhenParticipantDoesNotMatch() {
        ChallengeRecord record = createRecord();

        assertThatThrownBy(() -> record.certify(
                ACTIVITY_RECORD_ID,
                OTHER_GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.SUCCESS
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordErrorCode.ACTIVITY_RECORD_PARTICIPANT_MISMATCH);
    }

    @Test
    @DisplayName("이미 인증된 챌린지 기록은 다시 인증 완료 처리할 수 없다")
    void certify_failsWhenAlreadyCertified() {
        ChallengeRecord record = createRecord();
        record.certify(
                ACTIVITY_RECORD_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.SUCCESS
        );

        assertThatThrownBy(() -> record.certify(
                200L,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.FAIL
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordErrorCode.CHALLENGE_RECORD_ALREADY_CERTIFIED);
    }

    @Test
    @DisplayName("admin 보정은 이미 인증된 챌린지 기록의 성공/실패 상태를 다시 설정한다")
    void correctCertificationResult_changesCertifiedStatus() {
        ChallengeRecord record = createRecord();
        record.certify(
                ACTIVITY_RECORD_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.FAIL
        );

        record.correctCertificationResult(ChallengeRecordCertificationResult.SUCCESS);

        assertThat(record.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(record.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_SUCCESS);
        assertThat(record.isCertificationSucceeded()).isTrue();
    }

    private ChallengeRecord createRecord() {
        return ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );
    }
}

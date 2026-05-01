package com.detoxmate.challengerecord.service;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.domain.ChallengeRecordStatus;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.challengerecord.ChallengeRecordErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChallengeRecordServiceTest {

    @Autowired
    ChallengeRecordService challengeRecordService;

    @Autowired
    ChallengeRecordRepository challengeRecordRepository;

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long GROUP_CHALLENGE_PARTICIPANT_ID = 20L;
    private static final Long OTHER_GROUP_CHALLENGE_PARTICIPANT_ID = 30L;
    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 5, 1);

    @Test
    @DisplayName("챌린지 기록을 생성하면 BEFORE_RECORD 상태로 저장된다")
    void create_persistsBeforeRecord() {
        // when
        ChallengeRecord created = challengeRecordService.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );

        // then
        ChallengeRecord found = challengeRecordRepository.findById(created.getId()).orElseThrow();

        assertThat(found.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(found.getGroupChallengeParticipantId()).isEqualTo(GROUP_CHALLENGE_PARTICIPANT_ID);
        assertThat(found.getRecordDate()).isEqualTo(RECORD_DATE);
        assertThat(found.getActivityRecordId()).isNull();
        assertThat(found.getStatus()).isEqualTo(ChallengeRecordStatus.BEFORE_RECORD);
    }

    @Test
    @DisplayName("같은 그룹 챌린지, 참여자, 날짜의 챌린지 기록이 이미 있으면 기존 기록을 반환한다")
    void create_returnsExistingRecordWhenAlreadyExists() {
        // given
        ChallengeRecord existing = challengeRecordService.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );

        // when
        ChallengeRecord created = challengeRecordService.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );

        // then
        assertThat(created.getId()).isEqualTo(existing.getId());
        assertThat(challengeRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 날짜라도 참여자가 다르면 다른 챌린지 기록을 생성한다")
    void create_persistsDifferentRecordWhenParticipantIsDifferent() {
        // given
        ChallengeRecord first = challengeRecordService.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );

        // when
        ChallengeRecord second = challengeRecordService.create(
                GROUP_CHALLENGE_ID,
                OTHER_GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );

        // then
        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(challengeRecordRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("챌린지 기록에 성공 인증 기록을 연결한다")
    void certify_successConnectsActivityRecord() {
        // given
        ChallengeRecord record = challengeRecordService.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );

        // when
        challengeRecordService.certify(
                record.getId(),
                ACTIVITY_RECORD_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.SUCCESS
        );

        // then
        ChallengeRecord found = challengeRecordRepository.findById(record.getId()).orElseThrow();

        assertThat(found.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(found.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_SUCCESS);
    }

    @Test
    @DisplayName("챌린지 기록에 실패 인증 기록을 연결한다")
    void certify_failConnectsActivityRecord() {
        // given
        ChallengeRecord record = challengeRecordService.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );

        // when
        challengeRecordService.certify(
                record.getId(),
                ACTIVITY_RECORD_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.FAIL
        );

        // then
        ChallengeRecord found = challengeRecordRepository.findById(record.getId()).orElseThrow();

        assertThat(found.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(found.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_FAIL);
    }

    @Test
    @DisplayName("존재하지 않는 챌린지 기록에 인증 기록을 연결할 수 없다")
    void certify_throwsExceptionWhenChallengeRecordDoesNotExist() {
        // when & then
        assertThatThrownBy(() -> challengeRecordService.certify(
                999L,
                ACTIVITY_RECORD_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.SUCCESS
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordErrorCode.CHALLENGE_RECORD_NOT_FOUND);
    }

    @Test
    @DisplayName("챌린지 기록과 인증 기록의 참여자가 다르면 인증 연결에 실패한다")
    void certify_throwsExceptionWhenParticipantDoesNotMatch() {
        // given
        ChallengeRecord record = challengeRecordService.create(
                GROUP_CHALLENGE_ID,
                GROUP_CHALLENGE_PARTICIPANT_ID,
                RECORD_DATE
        );

        // when & then
        assertThatThrownBy(() -> challengeRecordService.certify(
                record.getId(),
                ACTIVITY_RECORD_ID,
                OTHER_GROUP_CHALLENGE_PARTICIPANT_ID,
                ChallengeRecordCertificationResult.SUCCESS
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChallengeRecordErrorCode.ACTIVITY_RECORD_PARTICIPANT_MISMATCH);
    }

}

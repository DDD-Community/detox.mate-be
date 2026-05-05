package com.detoxmate.challengerecord.repository;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.domain.ChallengeRecordStatus;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ActiveProfiles("test")
class ChallengeRecordRepositoryTest {

    @Autowired
    private ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupChallengeParticipantRepository participantRepository;


    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;

    private static final Long PARTICIPANT_ID = 100L;
    private static final Long OTHER_PARTICIPANT_ID = 200L;

    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 5, 1);
    private static final LocalDate OTHER_RECORD_DATE = LocalDate.of(2026, 5, 2);

    @Test
    @DisplayName("챌린지 기록을 저장하면 ID가 부여된다")
    void saveChallengeRecord_assignsId() {
        // given
        ChallengeRecord record = ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                PARTICIPANT_ID,
                RECORD_DATE
        );

        // when
        ChallengeRecord saved = challengeRecordRepository.save(record);

        // then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("그룹 챌린지, 참여자, 날짜로 챌린지 기록을 조회한다")
    void findByParticipantDate_returnsMatchingRecord() {
        // given
        ChallengeRecord record = challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, PARTICIPANT_ID, RECORD_DATE)
        );

        // when
        Optional<ChallengeRecord> found = challengeRecordRepository.findByParticipantDate(
                GROUP_CHALLENGE_ID,
                PARTICIPANT_ID,
                RECORD_DATE
        );

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(record.getId());
    }

    @Test
    @DisplayName("같은 참여자와 날짜라도 그룹 챌린지가 다르면 조회되지 않는다")
    void findByParticipantDate_isolatesByGroupChallenge() {
        // given
        challengeRecordRepository.save(
                ChallengeRecord.create(OTHER_GROUP_CHALLENGE_ID, PARTICIPANT_ID, RECORD_DATE)
        );

        // when
        Optional<ChallengeRecord> found = challengeRecordRepository.findByParticipantDate(
                GROUP_CHALLENGE_ID,
                PARTICIPANT_ID,
                RECORD_DATE
        );

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("같은 그룹 챌린지와 날짜라도 참여자가 다르면 조회되지 않는다")
    void findByParticipantDate_isolatesByParticipant() {
        // given
        challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, OTHER_PARTICIPANT_ID, RECORD_DATE)
        );

        // when
        Optional<ChallengeRecord> found = challengeRecordRepository.findByParticipantDate(
                GROUP_CHALLENGE_ID,
                PARTICIPANT_ID,
                RECORD_DATE
        );

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("같은 그룹 챌린지와 참여자라도 날짜가 다르면 조회되지 않는다")
    void findByParticipantDate_isolatesByDate() {
        // given
        challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, PARTICIPANT_ID, OTHER_RECORD_DATE)
        );

        // when
        Optional<ChallengeRecord> found = challengeRecordRepository.findByParticipantDate(
                GROUP_CHALLENGE_ID,
                PARTICIPANT_ID,
                RECORD_DATE
        );

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("그룹 챌린지와 날짜로 해당 날짜의 모든 챌린지 기록을 ID 오름차순으로 조회한다")
    void findAllByGroupChallengeDate_returnsRecordsInGroupChallengeDate() {
        // given
        ChallengeRecord first = challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, PARTICIPANT_ID, RECORD_DATE)
        );
        ChallengeRecord second = challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, OTHER_PARTICIPANT_ID, RECORD_DATE)
        );
        challengeRecordRepository.save(
                ChallengeRecord.create(OTHER_GROUP_CHALLENGE_ID, PARTICIPANT_ID, RECORD_DATE)
        );
        challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, PARTICIPANT_ID, OTHER_RECORD_DATE)
        );

        // when
        List<ChallengeRecord> records = challengeRecordRepository.findAllByGroupChallengeDate(
                GROUP_CHALLENGE_ID,
                RECORD_DATE
        );

        // then
        assertThat(records)
                .extracting(ChallengeRecord::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("인증 완료된 챌린지 기록의 상태와 activityRecordId가 저장된다")
    void saveChallengeRecord_persistsCertification() {
        // given
        ChallengeRecord record = ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                PARTICIPANT_ID,
                RECORD_DATE
        );
        record.certify(1000L, PARTICIPANT_ID, ChallengeRecordCertificationResult.SUCCESS);

        // when
        ChallengeRecord saved = challengeRecordRepository.saveAndFlush(record);

        // then
        ChallengeRecord found = challengeRecordRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getActivityRecordId()).isEqualTo(1000L);
        assertThat(found.getStatus()).isEqualTo(ChallengeRecordStatus.AFTER_RECORD_SUCCESS);
    }

    @Test
    @DisplayName("그룹 챌린지와 날짜로 챌린지 기록을 사용자 이름 오름차순으로 조회한다")
    void findAllByGroupChallengeDateOrderByDisplayName_returnsRecordsOrderedByDisplayName() {
        // given
        User xeulbn = userRepository.save(User.createNew("슬빈"));
        User gorapaduck = userRepository.save(User.createNew("고라파덕"));
        User jammanbo = userRepository.save(User.createNew("잠만보"));

        GroupMember xeulbnMember = groupMemberRepository.save(GroupMember.createMember(xeulbn.getId(), 1L));
        GroupMember gorapaduckMember = groupMemberRepository.save(GroupMember.createMember(gorapaduck.getId(), 1L));
        GroupMember jammanboMember = groupMemberRepository.save(GroupMember.createMember(jammanbo.getId(), 1L));

        GroupChallengeParticipant xeulbnParticipant = participantRepository.save(
                GroupChallengeParticipant.join(xeulbnMember.getId(), GROUP_CHALLENGE_ID)
        );
        GroupChallengeParticipant gorapaduckParticipant = participantRepository.save(
                GroupChallengeParticipant.join(gorapaduckMember.getId(), GROUP_CHALLENGE_ID)
        );
        GroupChallengeParticipant jammanboParticipant = participantRepository.save(
                GroupChallengeParticipant.join(jammanboMember.getId(), GROUP_CHALLENGE_ID)
        );

        ChallengeRecord xeulbnRecord = challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, xeulbnParticipant.getId(), RECORD_DATE)
        );
        ChallengeRecord gorapaduckRecord = challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, gorapaduckParticipant.getId(), RECORD_DATE)
        );
        ChallengeRecord jammanboRecord = challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, jammanboParticipant.getId(), RECORD_DATE)
        );

        // when
        List<ChallengeRecord> records = challengeRecordRepository.findAllByGroupChallengeDateOrderByDisplayName(
                GROUP_CHALLENGE_ID,
                RECORD_DATE
        );

        // then
        assertThat(records)
                .extracting(ChallengeRecord::getId)
                .containsExactly(gorapaduckRecord.getId(), xeulbnRecord.getId(), jammanboRecord.getId());
    }


}

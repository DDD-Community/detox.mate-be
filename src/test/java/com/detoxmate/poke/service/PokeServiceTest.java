package com.detoxmate.poke.service;

import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.UsageGoalTypeRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.poke.PokeErrorCode;
import com.detoxmate.notification.event.PokeCreatedEvent;
import com.detoxmate.notification.event.PokeGoalSettingReminderEvent;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.repository.PokeRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RecordApplicationEvents
class PokeServiceTest {

    @Autowired
    PokeService pokeService;

    @Autowired
    PokeRepository pokeRepository;

    @Autowired
    ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    ChallengeRecordStatusCountRepository statusCountRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UsageGoalTypeRepository usageGoalTypeRepository;

    @Autowired
    UserUsageGoalTimeRepository userUsageGoalTimeRepository;

    @Autowired
    ApplicationEvents applicationEvents;

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long OTHER_PARTICIPANT_ID = 21L;
    private static final Long ACTIVITY_RECORD_ID = 100L;

    private static final Long SENDER_USER_ID = 1L;
    private static final Long RECEIVER_USER_ID = 2L;
    private static final Long OTHER_RECEIVER_USER_ID = 3L;

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate YESTERDAY = LocalDate.now().minusDays(1);

    @Test
    @DisplayName("오늘 인증 전 챌린지 기록에는 콕 찌르기를 할 수 있다")
    void poke_createsPokeWhenChallengeRecordIsBeforeRecordAndToday() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(TODAY);
        saveStatusCount(challengeRecord.getId());

        // when
        pokeService.poke(challengeRecord.getId(), RECEIVER_USER_ID, SENDER_USER_ID);

        // then
        Poke saved = pokeRepository.findAll().get(0);

        assertThat(saved.getChallengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(saved.getSenderUserId()).isEqualTo(SENDER_USER_ID);
        assertThat(saved.getReceiverUserId()).isEqualTo(RECEIVER_USER_ID);
        assertThat(saved.getPokeDate()).isEqualTo(TODAY);

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getPokeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("목표를 설정한 사용자를 콕 찌르면 일반 콕 찔림 이벤트를 발행한다")
    void poke_publishesPokeCreatedEventWhenReceiverHasGoal() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(TODAY);
        saveStatusCount(challengeRecord.getId());
        User sender = saveUser("슬빈");
        User receiver = saveUser("지민");
        saveGoal(receiver);

        // when
        pokeService.poke(challengeRecord.getId(), receiver.getId(), sender.getId());

        // then
        assertThat(applicationEvents.stream(PokeCreatedEvent.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.challengeRecordId()).isEqualTo(challengeRecord.getId());
                    assertThat(event.senderUserId()).isEqualTo(sender.getId());
                    assertThat(event.receiverUserId()).isEqualTo(receiver.getId());
                });
        assertThat(applicationEvents.stream(PokeGoalSettingReminderEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("목표를 설정하지 않은 사용자를 콕 찌르면 목표 설정 재촉 이벤트를 발행한다")
    void poke_publishesGoalSettingReminderEventWhenReceiverHasNoGoal() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(TODAY);
        saveStatusCount(challengeRecord.getId());
        User sender = saveUser("슬빈");
        User receiver = saveUser("지민");

        // when
        pokeService.poke(challengeRecord.getId(), receiver.getId(), sender.getId());

        // then
        assertThat(applicationEvents.stream(PokeGoalSettingReminderEvent.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.challengeRecordId()).isEqualTo(challengeRecord.getId());
                    assertThat(event.senderUserId()).isEqualTo(sender.getId());
                    assertThat(event.receiverUserId()).isEqualTo(receiver.getId());
                });
        assertThat(applicationEvents.stream(PokeCreatedEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("인증 후 챌린지 기록에는 콕 찌르기를 할 수 없다")
    void poke_throwsExceptionWhenChallengeRecordIsCertified() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord(TODAY);
        saveStatusCount(challengeRecord.getId());

        // when & then
        assertThatThrownBy(() -> pokeService.poke(challengeRecord.getId(), RECEIVER_USER_ID, SENDER_USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_NOT_ALLOWED_AFTER_RECORD);

        assertThat(pokeRepository.count()).isZero();
    }

    @Test
    @DisplayName("어제 인증 전 챌린지 기록에는 콕 찌르기를 할 수 없다")
    void poke_throwsExceptionWhenChallengeRecordIsNotToday() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(YESTERDAY);
        saveStatusCount(challengeRecord.getId());

        // when & then
        assertThatThrownBy(() -> pokeService.poke(challengeRecord.getId(), RECEIVER_USER_ID, SENDER_USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_ONLY_TODAY_ALLOWED);

        assertThat(pokeRepository.count()).isZero();
    }

    @Test
    @DisplayName("같은 sender는 같은 챌린지 기록의 같은 receiver를 중복으로 찌를 수 없다")
    void poke_throwsExceptionWhenAlreadyPokedSameReceiver() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(TODAY);
        saveStatusCount(challengeRecord.getId());

        pokeService.poke(challengeRecord.getId(), RECEIVER_USER_ID, SENDER_USER_ID);

        // when & then
        assertThatThrownBy(() -> pokeService.poke(challengeRecord.getId(), RECEIVER_USER_ID, SENDER_USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_ALREADY_EXISTS);

        assertThat(pokeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 sender는 같은 챌린지 기록에서 다른 receiver를 찌를 수 있다")
    void poke_allowsDifferentReceiverInSameChallengeRecord() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(TODAY);
        saveStatusCount(challengeRecord.getId());

        // when
        pokeService.poke(challengeRecord.getId(), RECEIVER_USER_ID, SENDER_USER_ID);
        pokeService.poke(challengeRecord.getId(), OTHER_RECEIVER_USER_ID, SENDER_USER_ID);

        // then
        assertThat(pokeRepository.findAllByChallengeRecordOrderByLatest(challengeRecord.getId()))
                .extracting(Poke::getReceiverUserId)
                .containsExactlyInAnyOrder(RECEIVER_USER_ID, OTHER_RECEIVER_USER_ID);

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getPokeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("자기 자신을 콕 찌를 수 없다")
    void poke_throwsExceptionWhenSenderAndReceiverAreSame() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(TODAY);
        saveStatusCount(challengeRecord.getId());

        // when & then
        assertThatThrownBy(() -> pokeService.poke(challengeRecord.getId(), SENDER_USER_ID, SENDER_USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PokeErrorCode.POKE_SELF_NOT_ALLOWED);

        assertThat(pokeRepository.count()).isZero();
    }

    @Test
    @DisplayName("챌린지 기록의 콕 찌르기 목록을 최신순으로 조회한다")
    void getPokesForChallengeRecord_returnsPokesOrderByLatest() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(TODAY);

        Poke old = pokeRepository.save(
                Poke.create(
                        challengeRecord.getId(),
                        SENDER_USER_ID,
                        RECEIVER_USER_ID,
                        TODAY.minusDays(1)
                )
        );
        Poke firstToday = pokeRepository.save(
                Poke.create(
                        challengeRecord.getId(),
                        4L,
                        RECEIVER_USER_ID,
                        TODAY
                )
        );
        Poke secondToday = pokeRepository.save(
                Poke.create(
                        challengeRecord.getId(),
                        5L,
                        RECEIVER_USER_ID,
                        TODAY
                )
        );

        ChallengeRecord otherChallengeRecord = saveBeforeRecord(TODAY, OTHER_PARTICIPANT_ID);
        pokeRepository.save(
                Poke.create(
                        otherChallengeRecord.getId(),
                        6L,
                        RECEIVER_USER_ID,
                        TODAY.plusDays(1)
                )
        );

        // when
        List<Poke> pokes = pokeService.getPokesForChallengeRecord(challengeRecord.getId());

        // then
        assertThat(pokes)
                .extracting(Poke::getId)
                .containsExactly(secondToday.getId(), firstToday.getId(), old.getId());
    }

    @Test
    @DisplayName("여러 챌린지 기록에서 현재 사용자가 찌른 콕만 조회한다")
    void getPokesByChallengeRecordsAndSender_returnsPokesForSender() {
        // given
        ChallengeRecord firstRecord = saveBeforeRecord(TODAY);
        ChallengeRecord secondRecord = saveBeforeRecord(TODAY, OTHER_PARTICIPANT_ID);

        Poke first = pokeRepository.save(
                Poke.create(firstRecord.getId(), SENDER_USER_ID, RECEIVER_USER_ID, TODAY)
        );
        Poke second = pokeRepository.save(
                Poke.create(secondRecord.getId(), SENDER_USER_ID, OTHER_RECEIVER_USER_ID, TODAY)
        );
        pokeRepository.save(
                Poke.create(firstRecord.getId(), OTHER_RECEIVER_USER_ID, RECEIVER_USER_ID, TODAY)
        );

        // when
        List<Poke> pokes = pokeService.getPokesByChallengeRecordsAndSender(
                List.of(firstRecord.getId(), secondRecord.getId()),
                SENDER_USER_ID
        );

        // then
        assertThat(pokes)
                .extracting(Poke::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }


    private ChallengeRecord saveBeforeRecord(LocalDate recordDate) {
        return saveBeforeRecord(recordDate, PARTICIPANT_ID);
    }

    private ChallengeRecord saveBeforeRecord(LocalDate recordDate, Long participantId) {
        return challengeRecordRepository.save(
                ChallengeRecord.create(
                        GROUP_CHALLENGE_ID,
                        participantId,
                        recordDate
                )
        );
    }

    private ChallengeRecord saveCertifiedRecord(LocalDate recordDate) {
        ChallengeRecord challengeRecord = ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                PARTICIPANT_ID,
                recordDate
        );

        challengeRecord.certify(
                ACTIVITY_RECORD_ID,
                PARTICIPANT_ID,
                ChallengeRecordCertificationResult.SUCCESS
        );

        return challengeRecordRepository.save(challengeRecord);
    }

    private ChallengeRecordStatusCount saveStatusCount(Long challengeRecordId) {
        return statusCountRepository.save(
                ChallengeRecordStatusCount.create(challengeRecordId)
        );
    }

    private User saveUser(String displayName) {
        return userRepository.save(User.createNew(displayName));
    }

    private UserUsageGoalTime saveGoal(User user) {
        UsageGoalType usageGoalType = usageGoalTypeRepository.findByCode(UsageGoalTypeCode.TOTAL_USAGE)
                .orElseGet(() -> usageGoalTypeRepository.save(UsageGoalType.create(1L, UsageGoalTypeCode.TOTAL_USAGE)));
        return userUsageGoalTimeRepository.save(
                UserUsageGoalTime.create(user, usageGoalType, 60)
        );
    }

}

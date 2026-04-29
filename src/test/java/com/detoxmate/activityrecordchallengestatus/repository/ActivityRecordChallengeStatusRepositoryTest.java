package com.detoxmate.activityrecordchallengestatus.repository;

import com.detoxmate.activityrecordchallengestatus.domain.ActivityRecordChallengeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ActivityRecordChallengeStatusRepositoryTest {

    @Autowired
    private ActivityRecordChallengeStatusRepository statusRepository;

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;

    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final Long OTHER_ACTIVITY_RECORD_ID = 200L;

    @Test
    @DisplayName("상태를 저장하면 ID가 부여된다")
    void saveStatus_assignsId() {
        // given
        ActivityRecordChallengeStatus status = ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // when
        ActivityRecordChallengeStatus saved = statusRepository.save(status);

        // then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("그룹 챌린지와 activity record 조합으로 상태를 조회한다")
    void findByChallengeRecord_returnsMatchingStatus() {
        // given
        ActivityRecordChallengeStatus status = statusRepository.save(
                ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID)
        );

        // when
        Optional<ActivityRecordChallengeStatus> found = statusRepository.findByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(status.getId());
    }

    @Test
    @DisplayName("같은 activity record라도 그룹 챌린지가 다르면 다른 상태로 조회된다")
    void findByChallengeRecord_isolatesByGroupChallenge() {
        // given
        ActivityRecordChallengeStatus target = statusRepository.save(ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID));
        statusRepository.save(ActivityRecordChallengeStatus.create(OTHER_GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID));

        // when
        Optional<ActivityRecordChallengeStatus> found = statusRepository.findByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(target.getId());
    }

    @Test
    @DisplayName("같은 그룹 챌린지라도 activity record가 다르면 다른 상태로 조회된다")
    void findByChallengeRecord_isolatesByActivityRecord() {
        // given
        ActivityRecordChallengeStatus target = statusRepository.save(ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID));
        statusRepository.save(ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, OTHER_ACTIVITY_RECORD_ID));

        // when
        Optional<ActivityRecordChallengeStatus> found = statusRepository.findByChallengeRecord(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID
        );

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(target.getId());
    }

    @Test
    @DisplayName("해당 조합의 상태가 없으면 Optional.empty를 반환한다")
    void findByChallengeRecord_returnsEmptyWhenStatusDoesNotExist() {
        // when
        Optional<ActivityRecordChallengeStatus> found = statusRepository.findByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("증가된 카운트 값이 저장된다")
    void saveStatus_persistsCounts() {
        // given
        ActivityRecordChallengeStatus status = ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);
        status.increaseCommentCount();
        status.increaseReactionCount();
        status.increasePokeCount();

        ActivityRecordChallengeStatus saved = statusRepository.saveAndFlush(status);

        // when
        ActivityRecordChallengeStatus found = statusRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getCommentCount()).isEqualTo(1);
        assertThat(found.getReactionCount()).isEqualTo(1);
        assertThat(found.getPokeCount()).isEqualTo(1);
    }

}

package com.detoxmate.activityrecordchallengestatus.service;

import com.detoxmate.activityrecordchallengestatus.domain.ActivityRecordChallengeStatus;
import com.detoxmate.activityrecordchallengestatus.repository.ActivityRecordChallengeStatusRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.feed.FeedErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ActivityRecordChallengeStatusServiceTest {

    @Autowired
    ActivityRecordChallengeStatusService statusService;

    @Autowired
    ActivityRecordChallengeStatusRepository statusRepository;

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;

    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final Long OTHER_ACTIVITY_RECORD_ID = 200L;

    @Test
    @DisplayName("ChallengeRecord 상태를 생성하면 카운트가 0인 상태로 저장된다")
    void create_persistsStatusWithZeroCounts() {
        // when
        ActivityRecordChallengeStatus created = statusService.create(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID
        );

        // then
        ActivityRecordChallengeStatus found = statusRepository.findById(created.getId()).orElseThrow();

        assertThat(found.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(found.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(found.getCommentCount()).isZero();
        assertThat(found.getReactionCount()).isZero();
        assertThat(found.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("ChallengeRecord의 댓글 수를 1 증가시킨다")
    void increaseCommentCount_increasesCommentCount() {
        // given
        ActivityRecordChallengeStatus status = saveStatus(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // when
        statusService.increaseCommentCount(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // then
        ActivityRecordChallengeStatus found = statusRepository.findById(status.getId()).orElseThrow();

        assertThat(found.getCommentCount()).isEqualTo(1);
        assertThat(found.getReactionCount()).isZero();
        assertThat(found.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("ChallengeRecord의 리액션 수를 1 증가시킨다")
    void increaseReactionCount_increasesReactionCount() {
        // given
        ActivityRecordChallengeStatus status = saveStatus(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // when
        statusService.increaseReactionCount(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // then
        ActivityRecordChallengeStatus found = statusRepository.findById(status.getId()).orElseThrow();

        assertThat(found.getCommentCount()).isZero();
        assertThat(found.getReactionCount()).isEqualTo(1);
        assertThat(found.getPokeCount()).isZero();
    }

    @Test
    @DisplayName("ChallengeRecord의 찌르기 수를 1 증가시킨다")
    void increasePokeCount_increasesPokeCount() {
        // given
        ActivityRecordChallengeStatus status = saveStatus(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // when
        statusService.increasePokeCount(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // then
        ActivityRecordChallengeStatus found = statusRepository.findById(status.getId()).orElseThrow();

        assertThat(found.getCommentCount()).isZero();
        assertThat(found.getReactionCount()).isZero();
        assertThat(found.getPokeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 activity record라도 다른 group challenge의 상태는 증가하지 않는다")
    void increaseCommentCount_doesNotAffectOtherGroupChallenge() {
        // given
        ActivityRecordChallengeStatus target = saveStatus(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);
        ActivityRecordChallengeStatus other = saveStatus(OTHER_GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // when
        statusService.increaseCommentCount(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // then
        ActivityRecordChallengeStatus foundTarget = statusRepository.findById(target.getId()).orElseThrow();
        ActivityRecordChallengeStatus foundOther = statusRepository.findById(other.getId()).orElseThrow();

        assertThat(foundTarget.getCommentCount()).isEqualTo(1);
        assertThat(foundOther.getCommentCount()).isZero();
    }

    @Test
    @DisplayName("같은 group challenge라도 다른 activity record의 상태는 증가하지 않는다")
    void increaseCommentCount_doesNotAffectOtherActivityRecord() {
        // given
        ActivityRecordChallengeStatus target = saveStatus(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);
        ActivityRecordChallengeStatus other = saveStatus(GROUP_CHALLENGE_ID, OTHER_ACTIVITY_RECORD_ID);

        // when
        statusService.increaseCommentCount(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID);

        // then
        ActivityRecordChallengeStatus foundTarget = statusRepository.findById(target.getId()).orElseThrow();
        ActivityRecordChallengeStatus foundOther = statusRepository.findById(other.getId()).orElseThrow();

        assertThat(foundTarget.getCommentCount()).isEqualTo(1);
        assertThat(foundOther.getCommentCount()).isZero();
    }

    @Test
    @DisplayName("ChallengeRecord 상태가 없으면 카운트를 증가시킬 수 없다")
    void increaseCommentCount_throwsExceptionWhenStatusDoesNotExist() {
        // when & then
        assertThatThrownBy(() -> statusService.increaseCommentCount(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.ACTIVITY_RECORD_CHALLENGE_STATUS_NOT_FOUND);
    }

    private ActivityRecordChallengeStatus saveStatus(Long groupChallengeId, Long activityRecordId) {
        return statusRepository.save(ActivityRecordChallengeStatus.create(groupChallengeId, activityRecordId));
    }

}

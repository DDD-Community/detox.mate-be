package com.detoxmate.reaction.service;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.reaction.ReactionErrorCode;
import com.detoxmate.reaction.domain.Reaction;
import com.detoxmate.reaction.domain.ReactionBody;
import com.detoxmate.reaction.dto.request.CreateReactionRequest;
import com.detoxmate.reaction.dto.response.ReactionResponse;
import com.detoxmate.reaction.repository.ReactionRepository;
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
class ReactionServiceTest {

    @Autowired
    ReactionService reactionService;

    @Autowired
    ReactionRepository reactionRepository;

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;

    private static final Long ACTIVITY_RECORD_ID = 100L;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Test
    @DisplayName("리액션이 정상 작성되면 해당 ChallengeRecord에 저장된다")
    void create_persistsReactionInChallengeRecord() {
        // given
        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        // when
        ReactionResponse response = reactionService.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, request, USER_ID);

        // then
        assertThat(response.reactionId()).isNotNull();
        assertThat(response.groupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(response.stampId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.reactionBody()).isEqualTo("CLAP");
        assertThat(response.createdAt()).isNotNull();

        Reaction saved = reactionRepository.findById(response.reactionId()).orElseThrow();

        assertThat(saved.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(saved.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getBody()).isEqualTo(ReactionBody.CLAP);
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("같은 인증글이라도 그룹이 다르면 리액션은 분리된다")
    void create_separatesReactionByGroupChallengeId() {
        // given
        CreateReactionRequest request = new CreateReactionRequest("CHEER");

        // when
        ReactionResponse first = reactionService.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, request, USER_ID);
        ReactionResponse second = reactionService.create(OTHER_GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, request, OTHER_USER_ID);

        // then
        assertThat(reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID))
                .extracting(Reaction::getId)
                .containsExactly(first.reactionId());

        assertThat(reactionRepository.findActiveByChallengeRecord(OTHER_GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID))
                .extracting(Reaction::getId)
                .containsExactly(second.reactionId());
    }

    @Test
    @DisplayName("같은 사용자가 같은 ChallengeRecord에 같은 body 리액션을 중복으로 남길 수 없다")
    void create_throwsExceptionWhenSameUserAlreadyHasSameActiveReaction() {
        // given
        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        reactionService.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, request, USER_ID);

        // when & then
        assertThatThrownBy(() -> reactionService.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, request, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_ALREADY_EXISTS);

        assertThat(reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID))
                .hasSize(1);
    }

    @Test
    @DisplayName("같은 사용자가 같은 ChallengeRecord에 다른 body 리액션은 남길 수 있다")
    void create_allowsDifferentBodyReactionInSameChallengeRecord() {
        // given
        reactionService.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, new CreateReactionRequest("CLAP"), USER_ID);

        // when
        ReactionResponse response = reactionService.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, new CreateReactionRequest("HAMMER"), USER_ID);

        // then
        assertThat(response.reactionBody()).isEqualTo("HAMMER");

        assertThat(reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID))
                .extracting(Reaction::getBody)
                .containsExactly(ReactionBody.CLAP, ReactionBody.HAMMER);
    }

    @Test
    @DisplayName("삭제된 같은 body 리액션은 다시 남길 수 있다")
    void create_allowsSameBodyReactionAfterDeleted() {
        // given
        ReactionResponse first = reactionService.create(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                new CreateReactionRequest("CLAP"),
                USER_ID
        );

        reactionService.delete(GROUP_CHALLENGE_ID, first.reactionId(), USER_ID);

        // when
        ReactionResponse second = reactionService.create(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                new CreateReactionRequest("CLAP"),
                USER_ID
        );

        // then
        assertThat(second.reactionId()).isNotEqualTo(first.reactionId());
        assertThat(second.reactionBody()).isEqualTo("CLAP");

        assertThat(reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID))
                .extracting(Reaction::getId)
                .containsExactly(second.reactionId());
    }

    @Test
    @DisplayName("리액션 삭제 요청을 받으면 작성자의 리액션이 삭제 상태가 된다")
    void delete_marksReactionDeletedWhenAuthorRequests() {
        // given
        Reaction saved = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, USER_ID, ReactionBody.CLAP)
        );

        // when
        reactionService.delete(GROUP_CHALLENGE_ID, saved.getId(), USER_ID);

        // then
        Reaction found = reactionRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID))
                .isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 리액션 삭제 요청은 실패한다")
    void delete_throwsExceptionWhenReactionDoesNotExist() {
        // when & then
        assertThatThrownBy(() -> reactionService.delete(GROUP_CHALLENGE_ID, 999L, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 group challenge의 리액션 삭제 요청은 실패하고 기존 리액션은 유지된다")
    void delete_throwsExceptionWhenReactionBelongsToOtherGroupChallenge() {
        // given
        Reaction saved = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, OTHER_GROUP_CHALLENGE_ID, USER_ID, ReactionBody.CLAP)
        );

        // when & then
        assertThatThrownBy(() -> reactionService.delete(GROUP_CHALLENGE_ID, saved.getId(), USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_CHALLENGE_RECORD_MISMATCH);

        Reaction found = reactionRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("작성자가 아닌 사용자의 리액션 삭제 요청은 실패하고 기존 리액션은 유지된다")
    void delete_throwsExceptionWhenUserIsNotAuthor() {
        // given
        Reaction saved = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, USER_ID, ReactionBody.CLAP)
        );

        // when & then
        assertThatThrownBy(() -> reactionService.delete(GROUP_CHALLENGE_ID, saved.getId(), OTHER_USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_DELETE_FORBIDDEN);

        Reaction found = reactionRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("이미 삭제된 리액션 삭제 요청은 실패한다")
    void delete_throwsExceptionWhenReactionAlreadyDeleted() {
        // given
        Reaction saved = reactionRepository.save(
                Reaction.create(ACTIVITY_RECORD_ID, GROUP_CHALLENGE_ID, USER_ID, ReactionBody.CLAP)
        );
        saved.deleteBy(USER_ID);

        // when & then
        assertThatThrownBy(() -> reactionService.delete(GROUP_CHALLENGE_ID, saved.getId(), USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_ALREADY_DELETED);
    }

}

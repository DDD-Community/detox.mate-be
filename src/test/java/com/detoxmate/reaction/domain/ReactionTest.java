package com.detoxmate.reaction.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.reaction.ReactionErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ReactionTest {

    private static final Long CHALLENGE_RECORD_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;

    @Test
    @DisplayName("리액션을 생성하면 챌린지 기록, 작성자, 리액션 종류가 저장되고 삭제 상태가 아니다")
    void create_initializesReaction() {
        // when
        Reaction reaction = Reaction.create(
                CHALLENGE_RECORD_ID,
                USER_ID,
                ReactionBody.CLAP
        );

        // then
        assertThat(reaction.getChallengeRecordId()).isEqualTo(CHALLENGE_RECORD_ID);
        assertThat(reaction.getUserId()).isEqualTo(USER_ID);
        assertThat(reaction.getBody()).isEqualTo(ReactionBody.CLAP);
        assertThat(reaction.isDeleted()).isFalse();
        assertThat(reaction.getCreatedAt()).isNotNull();
        assertThat(reaction.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("챌린지 기록 ID 없이 리액션을 만들 수 없다")
    void create_throwsExceptionWhenChallengeRecordIdIsNull() {
        // when & then
        assertThatThrownBy(() -> Reaction.create(null, USER_ID, ReactionBody.CLAP))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_CHALLENGE_RECORD_REQUIRED);
    }

    @Test
    @DisplayName("작성자 없이 리액션을 만들 수 없다")
    void create_throwsExceptionWhenUserIdIsNull() {
        // when & then
        assertThatThrownBy(() -> Reaction.create(CHALLENGE_RECORD_ID, null, ReactionBody.CLAP))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_USER_REQUIRED);
    }

    @Test
    @DisplayName("리액션 종류 없이 리액션을 만들 수 없다")
    void create_throwsExceptionWhenBodyIsNull() {
        // when & then
        assertThatThrownBy(() -> Reaction.create(CHALLENGE_RECORD_ID, USER_ID, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_BODY_REQUIRED);
    }

    @Test
    @DisplayName("작성자가 삭제하면 리액션은 삭제 상태가 된다")
    void deleteBy_marksReactionDeleted() {
        // given
        Reaction reaction = Reaction.create(CHALLENGE_RECORD_ID, USER_ID, ReactionBody.CLAP);

        // when
        reaction.deleteBy(USER_ID);

        // then
        assertThat(reaction.isDeleted()).isTrue();
        assertThat(reaction.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("작성자가 아닌 사용자는 리액션을 삭제할 수 없다")
    void deleteBy_throwsExceptionWhenUserIsNotAuthor() {
        // given
        Reaction reaction = Reaction.create(CHALLENGE_RECORD_ID, USER_ID, ReactionBody.CLAP);

        // when & then
        assertThatThrownBy(() -> reaction.deleteBy(OTHER_USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_DELETE_FORBIDDEN);

        assertThat(reaction.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("이미 삭제된 리액션은 다시 삭제할 수 없다")
    void deleteBy_throwsExceptionWhenAlreadyDeleted() {
        // given
        Reaction reaction = Reaction.create(CHALLENGE_RECORD_ID, USER_ID, ReactionBody.CLAP);
        reaction.deleteBy(USER_ID);

        // when & then
        assertThatThrownBy(() -> reaction.deleteBy(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_ALREADY_DELETED);
    }
}

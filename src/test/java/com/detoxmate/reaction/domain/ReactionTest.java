package com.detoxmate.reaction.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.reaction.ReactionErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ReactionTest {

    private static final Long ANY_ACTIVITY_RECORD_ID = 100L;
    private static final Long ANY_GROUP_CHALLENGE_ID = 10L;
    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Test
    @DisplayName("리액션을 생성하면 작성자와 연관 ID, 리액션 본문이 저장된다")
    void createReaction_savesAuthorRelatedIdsAndBody() {
        //given & when
        Reaction reaction = Reaction.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, AUTHOR_ID, ReactionBody.CLAP);

        //then
        assertThat(reaction.getActivityRecordId()).isEqualTo(ANY_ACTIVITY_RECORD_ID);
        assertThat(reaction.getGroupChallengeId()).isEqualTo(ANY_GROUP_CHALLENGE_ID);
        assertThat(reaction.getUserId()).isEqualTo(AUTHOR_ID);
        assertThat(reaction.getBody()).isEqualTo(ReactionBody.CLAP);
    }

    @Test
    @DisplayName("리액션을 생성하면 생성 시간이 설정되고 삭제 상태가 아니다")
    void createReaction_initializesStatus() {
        //given & when
        Reaction reaction = Reaction.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, AUTHOR_ID, ReactionBody.CLAP);

        //then
        assertThat(reaction.getCreatedAt()).isNotNull();
        assertThat(reaction.getUpdatedAt()).isNull();
        assertThat(reaction.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("리액션 본문이 null이면 생성할 수 없다")
    void createReaction_failsWhenBodyIsNull() {
        //when & then
        assertThatThrownBy(() -> Reaction.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, AUTHOR_ID, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_BODY_REQUIRED);
    }

    @Test
    @DisplayName("작성자가 리액션을 삭제하면 삭제 상태가 된다")
    void deleteReaction_marksDeleted() {
        //given
        Reaction reaction = Reaction.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, AUTHOR_ID, ReactionBody.CLAP);

        //when
        reaction.deleteBy(AUTHOR_ID);

        //then
        assertThat(reaction.isDeleted()).isTrue();
        assertThat(reaction.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("작성자가 아니면 리액션을 삭제할 수 없다")
    void deleteReaction_failsWhenUserIsNotAuthor() {
        //given & when
        Reaction reaction = Reaction.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, AUTHOR_ID, ReactionBody.CLAP);

        //then
        assertThatThrownBy(() -> reaction.deleteBy(OTHER_USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_DELETE_FORBIDDEN);
    }

    @Test
    @DisplayName("이미 삭제된 리액션은 다시 삭제할 수 없다")
    void deleteReaction_failsWhenAlreadyDeleted() {
        //given
        Reaction reaction = Reaction.create(ANY_ACTIVITY_RECORD_ID, ANY_GROUP_CHALLENGE_ID, AUTHOR_ID, ReactionBody.CLAP);

        //when
        reaction.deleteBy(AUTHOR_ID);

        //then
        assertThatThrownBy(() -> reaction.deleteBy(AUTHOR_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_ALREADY_DELETED);
    }
}

package com.detoxmate.reaction.service;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReactionServiceTest {

    @Autowired
    ReactionService reactionService;

    @Autowired
    ReactionRepository reactionRepository;

    @Autowired
    ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    ChallengeRecordStatusCountRepository statusCountRepository;

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 5, 1);

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Test
    @DisplayName("인증 후 챌린지 기록에는 리액션을 남길 수 있다")
    void create_persistsReactionWhenChallengeRecordIsCertified() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        // when
        ReactionResponse response = reactionService.create(
                challengeRecord.getId(),
                request,
                USER_ID
        );

        // then
        assertThat(response.reactionId()).isNotNull();
        assertThat(response.challengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.reactionBody()).isEqualTo("CLAP");
        assertThat(response.createdAt()).isNotNull();

        Reaction saved = reactionRepository.findById(response.reactionId()).orElseThrow();

        assertThat(saved.getChallengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getBody()).isEqualTo(ReactionBody.CLAP);
        assertThat(saved.isDeleted()).isFalse();

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getReactionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("인증 전 챌린지 기록에는 리액션을 남길 수 없다")
    void create_throwsExceptionWhenChallengeRecordIsBeforeRecord() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();
        saveStatusCount(challengeRecord.getId());

        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        // when & then
        assertThatThrownBy(() -> reactionService.create(challengeRecord.getId(), request, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_NOT_ALLOWED_BEFORE_RECORD);

        assertThat(reactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("같은 사용자는 같은 챌린지 기록에 같은 리액션을 중복으로 남길 수 없다")
    void create_throwsExceptionWhenSameBodyAlreadyExists() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        reactionService.create(
                challengeRecord.getId(),
                new CreateReactionRequest("CLAP"),
                USER_ID
        );

        // when & then
        assertThatThrownBy(() -> reactionService.create(
                challengeRecord.getId(),
                new CreateReactionRequest("CLAP"),
                USER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_ALREADY_EXISTS);

        assertThat(reactionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 사용자는 같은 챌린지 기록에 서로 다른 리액션을 남길 수 있다")
    void create_allowsDifferentBodiesBySameUser() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        // when
        ReactionResponse first = reactionService.create(
                challengeRecord.getId(),
                new CreateReactionRequest("CLAP"),
                USER_ID
        );
        ReactionResponse second = reactionService.create(
                challengeRecord.getId(),
                new CreateReactionRequest("MUSCLE"),
                USER_ID
        );

        // then
        assertThat(reactionRepository.findActiveByChallengeRecord(challengeRecord.getId()))
                .extracting(Reaction::getId)
                .containsExactly(first.reactionId(), second.reactionId());

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getReactionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("삭제한 리액션과 같은 리액션은 다시 남길 수 있다")
    void create_allowsSameBodyAfterDeleted() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        ReactionResponse first = reactionService.create(
                challengeRecord.getId(),
                new CreateReactionRequest("CLAP"),
                USER_ID
        );

        reactionService.delete(challengeRecord.getId(), first.reactionId(), USER_ID);

        // when
        ReactionResponse second = reactionService.create(
                challengeRecord.getId(),
                new CreateReactionRequest("CLAP"),
                USER_ID
        );

        // then
        assertThat(second.reactionId()).isNotEqualTo(first.reactionId());

        assertThat(reactionRepository.findActiveByChallengeRecord(challengeRecord.getId()))
                .extracting(Reaction::getId)
                .containsExactly(second.reactionId());
    }

    @Test
    @DisplayName("작성자는 자신의 리액션을 삭제할 수 있다")
    void delete_marksReactionDeletedWhenAuthorRequests() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        ReactionResponse response = reactionService.create(
                challengeRecord.getId(),
                new CreateReactionRequest("CLAP"),
                USER_ID
        );

        // when
        reactionService.delete(challengeRecord.getId(), response.reactionId(), USER_ID);

        // then
        Reaction found = reactionRepository.findById(response.reactionId()).orElseThrow();

        assertThat(found.isDeleted()).isTrue();
        assertThat(reactionRepository.findActiveByChallengeRecord(challengeRecord.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("다른 사용자는 리액션을 삭제할 수 없다")
    void delete_throwsExceptionWhenUserIsNotAuthor() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        ReactionResponse response = reactionService.create(
                challengeRecord.getId(),
                new CreateReactionRequest("CLAP"),
                USER_ID
        );

        // when & then
        assertThatThrownBy(() -> reactionService.delete(
                challengeRecord.getId(),
                response.reactionId(),
                OTHER_USER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_DELETE_FORBIDDEN);

        Reaction found = reactionRepository.findById(response.reactionId()).orElseThrow();

        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("다른 챌린지 기록의 리액션 삭제 요청은 실패한다")
    void delete_throwsExceptionWhenReactionBelongsToOtherChallengeRecord() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        ChallengeRecord otherChallengeRecord = challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, 99L, RECORD_DATE)
        );
        otherChallengeRecord.certify(
                200L,
                99L,
                ChallengeRecordCertificationResult.SUCCESS
        );
        saveStatusCount(otherChallengeRecord.getId());

        ReactionResponse response = reactionService.create(
                otherChallengeRecord.getId(),
                new CreateReactionRequest("CLAP"),
                USER_ID
        );

        // when & then
        assertThatThrownBy(() -> reactionService.delete(
                challengeRecord.getId(),
                response.reactionId(),
                USER_ID
        ))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ReactionErrorCode.REACTION_CHALLENGE_RECORD_MISMATCH);
    }

    private ChallengeRecord saveBeforeRecord() {
        return challengeRecordRepository.save(
                ChallengeRecord.create(
                        GROUP_CHALLENGE_ID,
                        PARTICIPANT_ID,
                        RECORD_DATE
                )
        );
    }

    private ChallengeRecord saveCertifiedRecord() {
        ChallengeRecord challengeRecord = ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                PARTICIPANT_ID,
                RECORD_DATE
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

}

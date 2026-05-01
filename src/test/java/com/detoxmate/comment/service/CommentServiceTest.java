package com.detoxmate.comment.service;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.domain.CommentStatus;
import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.dto.response.CommentListResponse;
import com.detoxmate.comment.dto.response.CommentResponse;
import com.detoxmate.comment.repository.CommentRepository;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceTest {

    @Autowired
    CommentService commentService;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    ChallengeRecordStatusCountRepository statusCountRepository;

    @MockitoBean
    UserService userService;

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String VALID_BODY = "화이팅!";
    private static final int DEFAULT_SIZE = 20;
    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 5, 1);

    @BeforeEach
    void setUp() {
        given(userService.getProfilesByIds(anySet())).willAnswer(inv -> {
            Set<Long> ids = inv.getArgument(0);
            return ids.stream().collect(Collectors.toMap(
                    id -> id,
                    id -> new MyProfileResponse(id, "닉네임" + id, "https://img/" + id)
            ));
        });
    }

    @Test
    @DisplayName("인증 전 챌린지 기록에 댓글을 작성하면 BEFORE_RECORD 댓글로 저장된다")
    void create_persistsBeforeRecordCommentWhenChallengeRecordIsBeforeRecord() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();
        saveStatusCount(challengeRecord.getId());

        CreateCommentRequest request = new CreateCommentRequest(VALID_BODY);

        // when
        CommentResponse response = commentService.create(
                challengeRecord.getId(),
                request,
                USER_ID
        );

        // then
        Comment saved = commentRepository.findById(response.commentId()).orElseThrow();

        assertThat(saved.getChallengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getCommentBody()).isEqualTo(VALID_BODY);
        assertThat(saved.getCommentStatus()).isEqualTo(CommentStatus.BEFORE_RECORD);

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getBeforeCommentCount()).isEqualTo(1);
        assertThat(statusCount.getAfterCommentCount()).isZero();
    }

    @Test
    @DisplayName("인증 후 챌린지 기록에 댓글을 작성하면 AFTER_RECORD 댓글로 저장된다")
    void create_persistsAfterRecordCommentWhenChallengeRecordIsCertified() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        CreateCommentRequest request = new CreateCommentRequest(VALID_BODY);

        // when
        CommentResponse response = commentService.create(
                challengeRecord.getId(),
                request,
                USER_ID
        );

        // then
        Comment saved = commentRepository.findById(response.commentId()).orElseThrow();

        assertThat(saved.getCommentStatus()).isEqualTo(CommentStatus.AFTER_RECORD);

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getBeforeCommentCount()).isZero();
        assertThat(statusCount.getAfterCommentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("인증 전 상세 댓글 목록은 BEFORE_RECORD 댓글만 반환한다")
    void list_returnsBeforeRecordCommentsWhenChallengeRecordIsBeforeRecord() {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();

        Comment before = commentRepository.save(Comment.create(
                challengeRecord.getId(),
                USER_ID,
                "인증 전 댓글",
                CommentStatus.BEFORE_RECORD
        ));
        commentRepository.save(Comment.create(
                challengeRecord.getId(),
                USER_ID,
                "인증 후 댓글",
                CommentStatus.AFTER_RECORD
        ));

        // when
        CommentListResponse response = commentService.list(
                challengeRecord.getId(),
                null,
                DEFAULT_SIZE
        );

        // then
        assertThat(response.items())
                .extracting("commentId")
                .containsExactly(before.getId());
        assertThat(response.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("인증 후 상세 댓글 목록은 AFTER_RECORD 댓글만 반환한다")
    void list_returnsAfterRecordCommentsWhenChallengeRecordIsCertified() {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();

        commentRepository.save(Comment.create(
                challengeRecord.getId(),
                USER_ID,
                "인증 전 댓글",
                CommentStatus.BEFORE_RECORD
        ));
        Comment after = commentRepository.save(Comment.create(
                challengeRecord.getId(),
                USER_ID,
                "인증 후 댓글",
                CommentStatus.AFTER_RECORD
        ));

        // when
        CommentListResponse response = commentService.list(
                challengeRecord.getId(),
                null,
                DEFAULT_SIZE
        );

        // then
        assertThat(response.items())
                .extracting("commentId")
                .containsExactly(after.getId());
        assertThat(response.totalCount()).isEqualTo(1);
    }

    private ChallengeRecord saveBeforeRecord() {
        return challengeRecordRepository.save(
                ChallengeRecord.create(GROUP_CHALLENGE_ID, PARTICIPANT_ID, RECORD_DATE)
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

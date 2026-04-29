package com.detoxmate.comment.service;

import com.detoxmate.activityrecordchallengestatus.domain.ActivityRecordChallengeStatus;
import com.detoxmate.activityrecordchallengestatus.repository.ActivityRecordChallengeStatusRepository;
import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.dto.response.CommentListResponse;
import com.detoxmate.comment.dto.response.CommentResponse;
import com.detoxmate.comment.repository.CommentRepository;
import com.detoxmate.common.exception.CustomException;
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

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceTest {

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;

    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final Long OTHER_ACTIVITY_RECORD_ID = 999L;

    private static final Long USER_ID = 1L;
    private static final String VALID_BODY = "화이팅!";
    private static final int DEFAULT_SIZE = 20;

    @Autowired
    CommentService commentService;
    @Autowired
    CommentRepository commentRepository;
    @Autowired
    ActivityRecordChallengeStatusRepository statusRepository;

    @MockitoBean
    UserService userService;

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
    @DisplayName("댓글이 정상 작성되면 해당 인증글의 그룹 댓글에 저장된다")
    void create_persistsCommentInGroup() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(VALID_BODY);

        // when
        CommentResponse response = commentService.create(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                request,
                USER_ID
        );

        // then
        assertThat(response.commentId()).isNotNull();
        assertThat(response.groupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(response.activityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.commentBody()).isEqualTo(VALID_BODY);
        assertThat(response.createdAt()).isNotNull();

        Comment saved = commentRepository.findById(response.commentId()).orElseThrow();

        assertThat(saved.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(saved.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getCommentBody()).isEqualTo(VALID_BODY);
    }

    @Test
    @DisplayName("본문이 null이면 예외가 발생하고 DB에는 아무것도 저장되지 않는다")
    void create_throwsExceptionWhenBodyIsNull() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(null);

        // when & then
        assertThatThrownBy(() -> commentService.create(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                request,
                USER_ID
        )).isInstanceOf(CustomException.class);

        assertThat(commentRepository.count()).isZero();
    }

    @Test
    @DisplayName("댓글이 없으면 빈 목록과 totalCount=0, nextCursor=null을 반환한다")
    void list_returnsEmptyResponseWhenNoComments() {
        // when
        CommentListResponse response = commentService.list(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                null,
                DEFAULT_SIZE
        );

        // then
        assertThat(response.items()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("같은 인증글이라도 그룹이 다르면 댓글은 분리된다")
    void list_separatesCommentThreadByGroupChallengeId() {
        // given
        Comment comment1 = saveCommentInDefault(1L, "수능방 댓글1");
        Comment comment2 = saveCommentInDefault(2L, "수능방 댓글2");

        saveCommentInOtherGroup(3L, "직장인방 댓글");
        saveCommentInOtherGroup(4L, "운동방 댓글");

        // when
        CommentListResponse response = commentService.list(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                null,
                DEFAULT_SIZE
        );

        // then
        assertThat(response.items())
                .extracting("commentId")
                .containsExactly(comment1.getId(), comment2.getId());

        assertThat(response.totalCount()).isEqualTo(2L);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("같은 그룹이라도 인증글이 다르면 댓글은 분리된다")
    void list_separatesCommentThreadByActivityRecordId() {
        // given
        Comment comment1 = saveCommentInDefault(1L, "오늘 인증 댓글1");
        Comment comment2 = saveCommentInDefault(2L, "오늘 인증 댓글2");

        saveCommentInOtherActivityRecord(3L, "다른 인증글 댓글");

        // when
        CommentListResponse response = commentService.list(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                null,
                DEFAULT_SIZE
        );

        // then
        assertThat(response.items())
                .extracting("commentId")
                .containsExactly(comment1.getId(), comment2.getId());

        assertThat(response.totalCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("댓글이 size보다 적으면 모두 반환되고 nextCursor는 null이다")
    void list_returnsAllAndNullCursorWhenLessThanSize() {
        // given
        saveCommentInDefault(1L, "댓글1");
        saveCommentInDefault(2L, "댓글2");

        saveCommentInOtherGroup(3L, "다른 그룹 댓글");
        saveCommentInOtherActivityRecord(4L, "다른 인증글 댓글");

        // when
        CommentListResponse response = commentService.list(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                null,
                DEFAULT_SIZE
        );

        // then
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalCount()).isEqualTo(2L);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("댓글이 정확히 size개면 nextCursor는 null이다")
    void list_returnsNullCursorWhenExactlySize() {
        // given
        int size = 3;

        saveCommentInDefault(1L, "댓글1");
        saveCommentInDefault(2L, "댓글2");
        saveCommentInDefault(3L, "댓글3");

        // when
        CommentListResponse response = commentService.list(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                null,
                size
        );

        // then
        assertThat(response.items()).hasSize(3);
        assertThat(response.totalCount()).isEqualTo(3L);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("댓글이 size보다 많으면 size개만 반환되고 nextCursor는 마지막 항목의 ID이다")
    void list_returnsCursorWhenMoreThanSize() {
        // given
        int size = 2;

        Comment comment1 = saveCommentInDefault(1L, "댓글1");
        Comment comment2 = saveCommentInDefault(2L, "댓글2");
        saveCommentInDefault(3L, "댓글3");

        // when
        CommentListResponse response = commentService.list(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                null,
                size
        );

        // then
        assertThat(response.items()).hasSize(2);
        assertThat(response.items())
                .extracting("commentId")
                .containsExactly(comment1.getId(), comment2.getId());

        assertThat(response.totalCount()).isEqualTo(3L);
        assertThat(response.nextCursor()).isEqualTo(String.valueOf(comment2.getId()));
    }

    @Test
    @DisplayName("cursor가 주어지면 같은 댓글창에서 해당 ID 이후 댓글만 반환된다")
    void list_returnsCommentsAfterCursorInSameThread() {
        // given
        Comment comment1 = saveCommentInDefault(1L, "댓글1");
        Comment comment2 = saveCommentInDefault(2L, "댓글2");
        Comment comment3 = saveCommentInDefault(3L, "댓글3");

        saveCommentInOtherGroup(4L, "다른 그룹 댓글");

        // when
        CommentListResponse response = commentService.list(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                String.valueOf(comment1.getId()),
                DEFAULT_SIZE
        );

        // then
        assertThat(response.items())
                .extracting("commentId")
                .containsExactly(comment2.getId(), comment3.getId());

        assertThat(response.totalCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("각 댓글 항목에 UserService로부터 가져온 작성자 정보가 매핑된다")
    void list_mapsAuthorInfoToEachItem() {
        // given
        saveCommentInDefault(100L, "댓글1");
        saveCommentInDefault(200L, "댓글2");

        // when
        CommentListResponse response = commentService.list(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                null,
                DEFAULT_SIZE
        );

        // then
        assertThat(response.items())
                .extracting(item -> item.author().userId())
                .containsExactly(100L, 200L);

        assertThat(response.items())
                .extracting(item -> item.author().displayName())
                .containsExactly("닉네임100", "닉네임200");

        assertThat(response.items())
                .extracting(item -> item.author().profileImageUrl())
                .containsExactly("https://img/100", "https://img/200");
    }

    @Test
    @DisplayName("totalCount는 요청한 그룹과 인증글 조합의 전체 댓글 수만 반환한다")
    void list_returnsTotalCountOfRequestedInChallengeGroupAndActivityRecordOnly() {
        // given
        for (int i=1; i<=5; i++) {
            saveCommentInDefault((long) i, "댓글" + i);
        }

        saveCommentInOtherGroup(10L, "다른 그룹 댓글");
        saveCommentInOtherActivityRecord(11L, "다른 인증글 댓글");

        // when
        CommentListResponse response = commentService.list(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                null,
                DEFAULT_SIZE
        );

        // then
        assertThat(response.totalCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("댓글이 작성되면 해당 ChallengeRecord의 댓글 수가 1 증가한다")
    void create_increasesCommentCount() {
        // given
        statusRepository.save(ActivityRecordChallengeStatus.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID));

        CreateCommentRequest request = new CreateCommentRequest(VALID_BODY);

        // when
        commentService.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, request, USER_ID);

        // then
        ActivityRecordChallengeStatus status = statusRepository
                .findByChallengeRecord(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID)
                .orElseThrow();

        assertThat(status.getCommentCount()).isEqualTo(1);
        assertThat(status.getReactionCount()).isZero();
        assertThat(status.getPokeCount()).isZero();
    }

    private Comment saveCommentInDefault(Long userId, String body) {
        return saveComment(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, userId, body);
    }

    private Comment saveCommentInOtherGroup(Long userId, String body) {
        return saveComment(OTHER_GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, userId, body);
    }

    private Comment saveCommentInOtherActivityRecord(Long userId, String body) {
        return saveComment(GROUP_CHALLENGE_ID, OTHER_ACTIVITY_RECORD_ID, userId, body);
    }

    private Comment saveComment(Long groupChallengeId, Long activityRecordId, Long userId, String body) {
        return commentRepository.save(
                Comment.create(activityRecordId, groupChallengeId, userId, body)
        );
    }

}

package com.detoxmate.comment.controller;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.repository.CommentRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class CommentControllerTest {

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;
    private static final Long STAMP_ID = 100L;
    private static final Long OTHER_STAMP_ID = 999L;

    private static final String COMMENTS_URL = "/group-challenges/{gcId}/stamps/{sId}/comments";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    CommentRepository commentRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long currentUserId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.createNew("xeulbn"));
        currentUserId = user.getId();
    }

    @Test
    @DisplayName("POST /comments — 댓글이 DB에 저장되고 201과 응답을 반환한다")
    void create_persistsCommentAndReturns201() throws Exception {
        // given
        var request = new CreateCommentRequest("화이팅!");

        // when & then
        mockMvc.perform(post(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").exists())
                .andExpect(jsonPath("$.commentBody").value("화이팅!"))
                .andExpect(jsonPath("$.userId").value(currentUserId))
                .andExpect(jsonPath("$.groupChallengeId").value(GROUP_CHALLENGE_ID))
                .andExpect(jsonPath("$.activityRecordId").value(STAMP_ID));

        // DB 상태 검증
        assertThat(commentRepository.count()).isEqualTo(1);
        Comment saved = commentRepository.findAll().get(0);
        assertThat(saved.getUserId()).isEqualTo(currentUserId);
        assertThat(saved.getCommentBody()).isEqualTo("화이팅!");
        assertThat(saved.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(saved.getActivityRecordId()).isEqualTo(STAMP_ID);
    }

    @Test
    @DisplayName("POST /comments — 본문이 빈 문자열이면 400을 반환하고 DB에 저장되지 않는다")
    void create_returns400WhenBodyIsBlank() throws Exception {
        String body = """
                { "commentBody": "" }
                """;

        mockMvc.perform(post(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(commentRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /comments — 본문 필드가 누락되면 400을 반환한다")
    void create_returns400WhenBodyIsMissing() throws Exception {
        String body = "{}";

        mockMvc.perform(post(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(commentRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /comments — 인증되지 않은 요청은 401을 반환한다")
    void create_unauthenticated_returns401() throws Exception {
        var request = new CreateCommentRequest("화이팅!");

        mockMvc.perform(post(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        // Authorization 헤더 없음
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(commentRepository.count()).isZero();
    }

    // ===== GET =====

    @Test
    @DisplayName("GET /comments — 댓글이 없으면 200과 빈 목록을 반환한다")
    void getComments_returnsEmptyWhenNoComments() throws Exception {
        mockMvc.perform(get(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("GET /comments — 같은 그룹/인증글의 댓글만 반환되고 다른 그룹은 격리된다")
    void getComments_isolatesByGroupAndActivity() throws Exception {
        // given
        commentRepository.save(Comment.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, "수능방 댓글1"));
        commentRepository.save(Comment.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, "수능방 댓글2"));
        commentRepository.save(Comment.create(STAMP_ID, OTHER_GROUP_CHALLENGE_ID, currentUserId, "직장인방 댓글"));
        commentRepository.save(Comment.create(OTHER_STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, "다른 인증글 댓글"));

        // when & then
        mockMvc.perform(get(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].commentBody").value("수능방 댓글1"))
                .andExpect(jsonPath("$.items[1].commentBody").value("수능방 댓글2"));
    }

    @Test
    @DisplayName("GET /comments — 사이즈보다 댓글이 많으면 nextCursor에 마지막 항목 ID가 채워진다")
    void getComments_returnsNextCursorWhenMoreThanSize() throws Exception {
        // given
        commentRepository.save(Comment.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, "댓글1"));
        Comment c2 = commentRepository.save(Comment.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, "댓글2"));
        commentRepository.save(Comment.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, "댓글3"));

        // when & then
        mockMvc.perform(get(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").value(String.valueOf(c2.getId())));
    }

    @Test
    @DisplayName("GET /comments — cursor 이후 댓글만 반환된다")
    void getComments_returnsCommentsAfterCursor() throws Exception {
        // given
        Comment c1 = commentRepository.save(Comment.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, "댓글1"));
        Comment c2 = commentRepository.save(Comment.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, "댓글2"));
        Comment c3 = commentRepository.save(Comment.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, "댓글3"));

        // when & then
        mockMvc.perform(get(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .param("cursor", String.valueOf(c1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].commentId").value(c2.getId()))
                .andExpect(jsonPath("$.items[1].commentId").value(c3.getId()));
    }

    @Test
    @DisplayName("GET /comments — 응답 항목에 작성자 정보가 포함된다")
    void getComments_includesAuthorInfo() throws Exception {
        // given — 다른 사용자가 댓글 작성
        User commenter = userRepository.save(User.createNew("commenter"));
        commentRepository.save(Comment.create(STAMP_ID, GROUP_CHALLENGE_ID, commenter.getId(), "댓글"));

        // when & then
        mockMvc.perform(get(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].author.userId").value(commenter.getId()))
                .andExpect(jsonPath("$.items[0].author.displayName").value("commenter"));
    }

    @Test
    @DisplayName("GET /comments — 인증되지 않은 요청은 401을 반환한다")
    void getComments_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(COMMENTS_URL, GROUP_CHALLENGE_ID, STAMP_ID))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }


}

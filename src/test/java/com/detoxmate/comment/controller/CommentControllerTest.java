package com.detoxmate.comment.controller;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.domain.CommentStatus;
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

import java.time.LocalDate;

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

    private static final String COMMENTS_URL = "/challenge-records/{challengeRecordId}/comments";

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 5, 1);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    ChallengeRecordStatusCountRepository statusCountRepository;

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
    @DisplayName("POST /comments — 인증 전 챌린지 기록에 댓글이 저장되고 201과 응답을 반환한다")
    void create_beforeRecord_persistsCommentAndReturns201() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();
        saveStatusCount(challengeRecord.getId());

        CreateCommentRequest request = new CreateCommentRequest("화이팅!");

        // when & then
        mockMvc.perform(post(COMMENTS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").exists())
                .andExpect(jsonPath("$.challengeRecordId").value(challengeRecord.getId()))
                .andExpect(jsonPath("$.commentBody").value("화이팅!"))
                .andExpect(jsonPath("$.userId").value(currentUserId))
                .andExpect(jsonPath("$.createdAt").exists());

        assertThat(commentRepository.count()).isEqualTo(1);

        Comment saved = commentRepository.findAll().get(0);
        assertThat(saved.getChallengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(saved.getUserId()).isEqualTo(currentUserId);
        assertThat(saved.getCommentBody()).isEqualTo("화이팅!");
        assertThat(saved.getCommentStatus()).isEqualTo(CommentStatus.BEFORE_RECORD);

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getBeforeCommentCount()).isEqualTo(1);
        assertThat(statusCount.getAfterCommentCount()).isZero();
    }

    @Test
    @DisplayName("POST /comments — 인증 후 챌린지 기록에 댓글이 저장되면 AFTER_RECORD 댓글로 저장된다")
    void create_afterRecord_persistsAfterRecordComment() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        CreateCommentRequest request = new CreateCommentRequest("인증 후 댓글");

        // when & then
        mockMvc.perform(post(COMMENTS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.challengeRecordId").value(challengeRecord.getId()))
                .andExpect(jsonPath("$.commentBody").value("인증 후 댓글"));

        Comment saved = commentRepository.findAll().get(0);
        assertThat(saved.getCommentStatus()).isEqualTo(CommentStatus.AFTER_RECORD);

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getBeforeCommentCount()).isZero();
        assertThat(statusCount.getAfterCommentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /comments — 본문이 빈 문자열이면 400을 반환하고 DB에 저장되지 않는다")
    void create_returns400WhenBodyIsBlank() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();
        saveStatusCount(challengeRecord.getId());

        String body = """
                { "commentBody": "" }
                """;

        // when & then
        mockMvc.perform(post(COMMENTS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(commentRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /comments — 본문 필드가 누락되면 400을 반환한다")
    void create_returns400WhenBodyIsMissing() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();
        saveStatusCount(challengeRecord.getId());

        String body = "{}";

        // when & then
        mockMvc.perform(post(COMMENTS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(commentRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /comments — 인증되지 않은 요청은 401을 반환한다")
    void create_unauthenticated_returns401() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();
        CreateCommentRequest request = new CreateCommentRequest("화이팅!");

        // when & then
        mockMvc.perform(post(COMMENTS_URL, challengeRecord.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(commentRepository.count()).isZero();
    }

    @Test
    @DisplayName("GET /comments — 인증 전 챌린지 기록이면 인증 전 댓글만 반환한다")
    void getComments_beforeRecord_returnsOnlyBeforeRecordComments() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();

        Comment before1 = commentRepository.save(Comment.create(
                challengeRecord.getId(),
                currentUserId,
                "인증 전 댓글1",
                CommentStatus.BEFORE_RECORD
        ));
        Comment before2 = commentRepository.save(Comment.create(
                challengeRecord.getId(),
                currentUserId,
                "인증 전 댓글2",
                CommentStatus.BEFORE_RECORD
        ));
        commentRepository.save(Comment.create(
                challengeRecord.getId(),
                currentUserId,
                "인증 후 댓글",
                CommentStatus.AFTER_RECORD
        ));

        // when & then
        mockMvc.perform(get(COMMENTS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].commentId").value(before1.getId()))
                .andExpect(jsonPath("$.items[1].commentId").value(before2.getId()));
    }

    @Test
    @DisplayName("GET /comments — 인증 후 챌린지 기록이면 인증 후 댓글만 반환한다")
    void getComments_afterRecord_returnsOnlyAfterRecordComments() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();

        commentRepository.save(Comment.create(
                challengeRecord.getId(),
                currentUserId,
                "인증 전 댓글",
                CommentStatus.BEFORE_RECORD
        ));
        Comment after = commentRepository.save(Comment.create(
                challengeRecord.getId(),
                currentUserId,
                "인증 후 댓글",
                CommentStatus.AFTER_RECORD
        ));

        // when & then
        mockMvc.perform(get(COMMENTS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].commentId").value(after.getId()));
    }

    @Test
    @DisplayName("GET /comments — cursor 이후 댓글만 반환된다")
    void getComments_returnsCommentsAfterCursor() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();

        Comment c1 = commentRepository.save(Comment.create(
                challengeRecord.getId(),
                currentUserId,
                "댓글1",
                CommentStatus.BEFORE_RECORD
        ));
        Comment c2 = commentRepository.save(Comment.create(
                challengeRecord.getId(),
                currentUserId,
                "댓글2",
                CommentStatus.BEFORE_RECORD
        ));
        Comment c3 = commentRepository.save(Comment.create(
                challengeRecord.getId(),
                currentUserId,
                "댓글3",
                CommentStatus.BEFORE_RECORD
        ));

        // when & then
        mockMvc.perform(get(COMMENTS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .param("cursor", String.valueOf(c1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].commentId").value(c2.getId()))
                .andExpect(jsonPath("$.items[1].commentId").value(c3.getId()));
    }

    @Test
    @DisplayName("GET /comments — 인증되지 않은 요청은 401을 반환한다")
    void getComments_unauthenticated_returns401() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();

        // when & then
        mockMvc.perform(get(COMMENTS_URL, challengeRecord.getId()))
                .andExpect(status().isUnauthorized());
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

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

}

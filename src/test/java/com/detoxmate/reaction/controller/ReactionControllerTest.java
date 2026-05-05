package com.detoxmate.reaction.controller;


import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.reaction.domain.Reaction;
import com.detoxmate.reaction.domain.ReactionBody;
import com.detoxmate.reaction.dto.request.CreateReactionRequest;
import com.detoxmate.reaction.repository.ReactionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class ReactionControllerTest {

    private static final String REACTIONS_URL = "/challenge-records/{challengeRecordId}/reactions";
    private static final String REACTION_URL = "/challenge-records/{challengeRecordId}/reactions/{reactionId}";

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 5, 1);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ReactionRepository reactionRepository;

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
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        currentUserId = userRepository.save(User.createNew("xeulbn")).getId();
        otherUserId = userRepository.save(User.createNew("other")).getId();
    }

    @Test
    @DisplayName("POST /reactions — 인증 후 챌린지 기록에 리액션을 생성하면 201과 응답을 반환한다")
    void create_certifiedRecord_returns201() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        // when & then
        mockMvc.perform(post(REACTIONS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reactionId").exists())
                .andExpect(jsonPath("$.challengeRecordId").value(challengeRecord.getId()))
                .andExpect(jsonPath("$.userId").value(currentUserId))
                .andExpect(jsonPath("$.reactionBody").value("CLAP"))
                .andExpect(jsonPath("$.createdAt").exists());

        Reaction saved = reactionRepository.findAll().get(0);

        assertThat(saved.getChallengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(saved.getUserId()).isEqualTo(currentUserId);
        assertThat(saved.getBody()).isEqualTo(ReactionBody.CLAP);
        assertThat(saved.isDeleted()).isFalse();

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getReactionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /reactions — 인증 전 챌린지 기록에는 리액션을 남길 수 없다")
    void create_beforeRecord_returns400() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord();
        saveStatusCount(challengeRecord.getId());

        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        // when & then
        mockMvc.perform(post(REACTIONS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REACTION_NOT_ALLOWED_BEFORE_RECORD"));

        assertThat(reactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /reactions — 같은 리액션이 이미 있으면 400을 반환한다")
    void create_duplicateActiveReaction_returns400() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        reactionRepository.save(
                Reaction.create(challengeRecord.getId(), currentUserId, ReactionBody.CLAP)
        );

        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        // when & then
        mockMvc.perform(post(REACTIONS_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REACTION_ALREADY_EXISTS"));

        assertThat(reactionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /reactions — 인증되지 않은 요청은 401을 반환한다")
    void create_unauthenticated_returns401() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        // when & then
        mockMvc.perform(post(REACTIONS_URL, challengeRecord.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(reactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("DELETE /reactions/{reactionId} — 작성자가 삭제하면 204를 반환하고 삭제 상태가 된다")
    void delete_author_returns204() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();
        saveStatusCount(challengeRecord.getId());

        Reaction reaction = reactionRepository.save(
                Reaction.create(challengeRecord.getId(), currentUserId, ReactionBody.CLAP)
        );

        // when & then
        mockMvc.perform(delete(REACTION_URL, challengeRecord.getId(), reaction.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isNoContent());

        Reaction found = reactionRepository.findById(reaction.getId()).orElseThrow();

        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("DELETE /reactions/{reactionId} — 존재하지 않는 리액션이면 404를 반환한다")
    void delete_notFound_returns404() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();

        // when & then
        mockMvc.perform(delete(REACTION_URL, challengeRecord.getId(), 999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REACTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /reactions/{reactionId} — 다른 챌린지 기록의 리액션이면 400을 반환한다")
    void delete_otherChallengeRecord_returns400() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();

        ChallengeRecord otherChallengeRecord = ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                99L,
                RECORD_DATE
        );
        otherChallengeRecord.certify(
                200L,
                99L,
                ChallengeRecordCertificationResult.SUCCESS
        );
        challengeRecordRepository.save(otherChallengeRecord);

        Reaction reaction = reactionRepository.save(
                Reaction.create(otherChallengeRecord.getId(), currentUserId, ReactionBody.CLAP)
        );

        // when & then
        mockMvc.perform(delete(REACTION_URL, challengeRecord.getId(), reaction.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REACTION_CHALLENGE_RECORD_MISMATCH"));

        Reaction found = reactionRepository.findById(reaction.getId()).orElseThrow();
        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("DELETE /reactions/{reactionId} — 작성자가 아니면 403을 반환한다")
    void delete_notAuthor_returns403() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord();

        Reaction reaction = reactionRepository.save(
                Reaction.create(challengeRecord.getId(), currentUserId, ReactionBody.CLAP)
        );

        // when & then
        mockMvc.perform(delete(REACTION_URL, challengeRecord.getId(), reaction.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REACTION_DELETE_FORBIDDEN"));

        Reaction found = reactionRepository.findById(reaction.getId()).orElseThrow();
        assertThat(found.isDeleted()).isFalse();
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

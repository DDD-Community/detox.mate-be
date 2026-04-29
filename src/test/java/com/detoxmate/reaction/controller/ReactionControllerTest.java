package com.detoxmate.reaction.controller;


import com.detoxmate.auth.JwtTokenProvider;
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

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long OTHER_GROUP_CHALLENGE_ID = 20L;
    private static final Long STAMP_ID = 100L;

    private static final String REACTIONS_URL =
            "/group-challenges/{gcId}/activity-records/{activityRecordId}/reactions";

    private static final String REACTION_URL =
            "/group-challenges/{gcId}/reactions/{reactionId}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ReactionRepository reactionRepository;

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
    @DisplayName("POST /reactions — 리액션이 DB에 저장되고 201과 응답을 반환한다")
    void create_persistsReactionAndReturns201() throws Exception {
        // given
        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        // when & then
        mockMvc.perform(post(REACTIONS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reactionId").exists())
                .andExpect(jsonPath("$.groupChallengeId").value(GROUP_CHALLENGE_ID))
                .andExpect(jsonPath("$.stampId").value(STAMP_ID))
                .andExpect(jsonPath("$.userId").value(currentUserId))
                .andExpect(jsonPath("$.reactionBody").value("CLAP"))
                .andExpect(jsonPath("$.createdAt").exists());

        assertThat(reactionRepository.count()).isEqualTo(1);

        Reaction saved = reactionRepository.findAll().get(0);
        assertThat(saved.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(saved.getActivityRecordId()).isEqualTo(STAMP_ID);
        assertThat(saved.getUserId()).isEqualTo(currentUserId);
        assertThat(saved.getBody()).isEqualTo(ReactionBody.CLAP);
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("POST /reactions — reactionCode가 빈 문자열이면 400을 반환하고 DB에 저장되지 않는다")
    void create_returns400WhenReactionCodeIsBlank() throws Exception {
        //given
        String body = """
                { "reactionCode": "" }
                """;

        //when & then
        mockMvc.perform(post(REACTIONS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(reactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /reactions — reactionCode 필드가 누락되면 400을 반환하고 DB에 저장되지 않는다")
    void create_returns400WhenReactionCodeIsMissing() throws Exception {
        //given
        String body = "{}";

        //when & then
        mockMvc.perform(post(REACTIONS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(reactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /reactions — 인증되지 않은 요청은 401을 반환한다")
    void create_unauthenticated_returns401() throws Exception {
        //given
        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        //when & then
        mockMvc.perform(post(REACTIONS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(reactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /reactions — 같은 사용자가 같은 ChallengeRecord에 같은 body를 중복으로 남기면 400을 반환한다")
    void create_duplicateActiveReaction_returns400() throws Exception {
        // given
        reactionRepository.save(
                Reaction.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, ReactionBody.CLAP)
        );

        CreateReactionRequest request = new CreateReactionRequest("CLAP");

        // when & then
        mockMvc.perform(post(REACTIONS_URL, GROUP_CHALLENGE_ID, STAMP_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, STAMP_ID))
                .hasSize(1);
    }

    @Test
    @DisplayName("DELETE /reactions/{reactionId} — 작성자가 삭제하면 204를 반환하고 리액션은 삭제 상태가 된다")
    void delete_marksReactionDeletedAndReturns204() throws Exception {
        // given
        Reaction reaction = reactionRepository.save(
                Reaction.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, ReactionBody.CLAP)
        );

        // when & then
        mockMvc.perform(delete(REACTION_URL, GROUP_CHALLENGE_ID, reaction.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isNoContent());

        Reaction found = reactionRepository.findById(reaction.getId()).orElseThrow();

        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(reactionRepository.findActiveByChallengeRecord(GROUP_CHALLENGE_ID, STAMP_ID))
                .isEmpty();
    }

    @Test
    @DisplayName("DELETE /reactions/{reactionId} — 존재하지 않는 리액션이면 404를 반환한다")
    void delete_returns404WhenReactionDoesNotExist() throws Exception {
        mockMvc.perform(delete(REACTION_URL, GROUP_CHALLENGE_ID, 999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /reactions/{reactionId} — 다른 group challenge의 리액션이면 400을 반환하고 기존 리액션은 유지된다")
    void delete_returns400WhenReactionBelongsToOtherGroupChallenge() throws Exception {
        // given
        Reaction reaction = reactionRepository.save(
                Reaction.create(STAMP_ID, OTHER_GROUP_CHALLENGE_ID, currentUserId, ReactionBody.CLAP)
        );

        // when & then
        mockMvc.perform(delete(REACTION_URL, GROUP_CHALLENGE_ID, reaction.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isBadRequest());

        Reaction found = reactionRepository.findById(reaction.getId()).orElseThrow();

        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("DELETE /reactions/{reactionId} — 작성자가 아니면 403을 반환하고 기존 리액션은 유지된다")
    void delete_returns403WhenUserIsNotAuthor() throws Exception {
        // given
        Reaction reaction = reactionRepository.save(
                Reaction.create(STAMP_ID, GROUP_CHALLENGE_ID, otherUserId, ReactionBody.CLAP)
        );

        // when & then
        mockMvc.perform(delete(REACTION_URL, GROUP_CHALLENGE_ID, reaction.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isForbidden());

        Reaction found = reactionRepository.findById(reaction.getId()).orElseThrow();

        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("DELETE /reactions/{reactionId} — 인증되지 않은 요청은 401을 반환한다")
    void delete_unauthenticated_returns401() throws Exception {
        Reaction reaction = reactionRepository.save(
                Reaction.create(STAMP_ID, GROUP_CHALLENGE_ID, currentUserId, ReactionBody.CLAP)
        );

        mockMvc.perform(delete(REACTION_URL, GROUP_CHALLENGE_ID, reaction.getId()))
                .andExpect(status().isUnauthorized());

        Reaction found = reactionRepository.findById(reaction.getId()).orElseThrow();

        assertThat(found.isDeleted()).isFalse();
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

}

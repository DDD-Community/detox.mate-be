package com.detoxmate.poke.controller;

import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.repository.PokeRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class PokeControllerTest {

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long ACTIVITY_RECORD_ID = 100L;
    private static final String POKE_URL = "/group-challenges/{groupChallengeId}/activity-records/{activityRecordId}/members/{targetUserId}/poke";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PokeRepository pokeRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    private Long currentUserId;
    private Long targetUserId;
    private Long otherTargetUserId;

    @BeforeEach
    void setUp() {
        currentUserId = userRepository.save(User.createNew("sender")).getId();
        targetUserId = userRepository.save(User.createNew("receiver")).getId();
        otherTargetUserId = userRepository.save(User.createNew("other")).getId();
    }

    @Test
    @DisplayName("POST /poke — 찌르기가 DB에 저장되고 204를 반환한다")
    void poke_persistsPokeAndReturns204() throws Exception {
        // given

        // when & then
        mockMvc.perform(post(POKE_URL, GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, targetUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isNoContent());

        assertThat(pokeRepository.count()).isEqualTo(1);

        Poke saved = pokeRepository.findAll().get(0);
        assertThat(saved.getGroupChallengeId()).isEqualTo(GROUP_CHALLENGE_ID);
        assertThat(saved.getActivityRecordId()).isEqualTo(ACTIVITY_RECORD_ID);
        assertThat(saved.getSenderUserId()).isEqualTo(currentUserId);
        assertThat(saved.getReceiverUserId()).isEqualTo(targetUserId);
        assertThat(saved.getPokeDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /poke — 같은 사용자가 같은 대상에게 같은 날짜에 중복으로 찌르면 400을 반환한다")
    void poke_duplicateToday_returns400() throws Exception {
        // given
        pokeRepository.save(Poke.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, currentUserId, targetUserId, LocalDate.now()));

        // when & then
        mockMvc.perform(post(POKE_URL, GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, targetUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isBadRequest());

        assertThat(pokeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /poke — 같은 사용자가 다른 대상에게는 찌를 수 있다")
    void poke_allowsDifferentTargetUser() throws Exception {
        // given
        pokeRepository.save(Poke.create(GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, currentUserId, targetUserId, LocalDate.now()));

        // when & then
        mockMvc.perform(post(POKE_URL, GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, otherTargetUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isNoContent());

        assertThat(pokeRepository.count()).isEqualTo(2);
        assertThat(pokeRepository.existsPoke(
                GROUP_CHALLENGE_ID,
                ACTIVITY_RECORD_ID,
                currentUserId,
                otherTargetUserId,
                LocalDate.now()
        )).isTrue();
    }

    @Test
    @DisplayName("POST /poke — 자기 자신을 찌르면 400을 반환하고 DB에 저장되지 않는다")
    void poke_self_returns400() throws Exception {
        mockMvc.perform(post(POKE_URL, GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, currentUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUserId)))
                .andExpect(status().isBadRequest());

        assertThat(pokeRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /poke — 인증되지 않은 요청은 401을 반환한다")
    void poke_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(POKE_URL, GROUP_CHALLENGE_ID, ACTIVITY_RECORD_ID, targetUserId))
                .andExpect(status().isUnauthorized());

        assertThat(pokeRepository.count()).isZero();
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }


}

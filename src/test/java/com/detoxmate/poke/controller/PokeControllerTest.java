package com.detoxmate.poke.controller;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class PokeControllerTest {

    private static final String POKE_URL = "/challenge-records/{challengeRecordId}/pokes/{receiverUserId}";

    private static final Long GROUP_CHALLENGE_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long ACTIVITY_RECORD_ID = 100L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PokeRepository pokeRepository;

    @Autowired
    ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    ChallengeRecordStatusCountRepository statusCountRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    private Long senderUserId;
    private Long receiverUserId;

    @BeforeEach
    void setUp() {
        senderUserId = userRepository.save(User.createNew("sender")).getId();
        receiverUserId = userRepository.save(User.createNew("receiver")).getId();
    }

    @Test
    @DisplayName("POST /pokes — 오늘 인증 전 챌린지 기록에서 콕 찌르기에 성공하면 204를 반환한다")
    void poke_beforeRecordToday_returns204() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(LocalDate.now());
        saveStatusCount(challengeRecord.getId());

        // when & then
        mockMvc.perform(post(POKE_URL, challengeRecord.getId(), receiverUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId)))
                .andExpect(status().isNoContent());

        Poke saved = pokeRepository.findAll().get(0);

        assertThat(saved.getChallengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(saved.getSenderUserId()).isEqualTo(senderUserId);
        assertThat(saved.getReceiverUserId()).isEqualTo(receiverUserId);
        assertThat(saved.getPokeDate()).isEqualTo(LocalDate.now());

        ChallengeRecordStatusCount statusCount = statusCountRepository
                .findByChallengeRecordId(challengeRecord.getId())
                .orElseThrow();

        assertThat(statusCount.getPokeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /pokes — 인증 후 챌린지 기록은 콕 찌르기를 할 수 없다")
    void poke_afterRecord_returns400() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveCertifiedRecord(LocalDate.now());
        saveStatusCount(challengeRecord.getId());

        // when & then
        mockMvc.perform(post(POKE_URL, challengeRecord.getId(), receiverUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POKE_NOT_ALLOWED_AFTER_RECORD"));

        assertThat(pokeRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /pokes — 오늘이 아닌 챌린지 기록은 콕 찌르기를 할 수 없다")
    void poke_notToday_returns400() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(LocalDate.now().minusDays(1));
        saveStatusCount(challengeRecord.getId());

        // when & then
        mockMvc.perform(post(POKE_URL, challengeRecord.getId(), receiverUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POKE_ONLY_TODAY_ALLOWED"));

        assertThat(pokeRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /pokes — 같은 대상을 이미 찔렀으면 400을 반환한다")
    void poke_duplicate_returns400() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(LocalDate.now());
        saveStatusCount(challengeRecord.getId());

        pokeRepository.save(
                Poke.create(
                        challengeRecord.getId(),
                        senderUserId,
                        receiverUserId,
                        LocalDate.now()
                )
        );

        // when & then
        mockMvc.perform(post(POKE_URL, challengeRecord.getId(), receiverUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POKE_ALREADY_EXISTS"));

        assertThat(pokeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /pokes — 자기 자신을 찌르면 400을 반환한다")
    void poke_self_returns400() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(LocalDate.now());
        saveStatusCount(challengeRecord.getId());

        // when & then
        mockMvc.perform(post(POKE_URL, challengeRecord.getId(), senderUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POKE_SELF_NOT_ALLOWED"));

        assertThat(pokeRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /pokes — 인증되지 않은 요청은 401을 반환한다")
    void poke_unauthenticated_returns401() throws Exception {
        // given
        ChallengeRecord challengeRecord = saveBeforeRecord(LocalDate.now());

        // when & then
        mockMvc.perform(post(POKE_URL, challengeRecord.getId(), receiverUserId))
                .andExpect(status().isUnauthorized());

        assertThat(pokeRepository.count()).isZero();
    }

    private ChallengeRecord saveBeforeRecord(LocalDate recordDate) {
        return challengeRecordRepository.save(
                ChallengeRecord.create(
                        GROUP_CHALLENGE_ID,
                        PARTICIPANT_ID,
                        recordDate
                )
        );
    }

    private ChallengeRecord saveCertifiedRecord(LocalDate recordDate) {
        ChallengeRecord challengeRecord = ChallengeRecord.create(
                GROUP_CHALLENGE_ID,
                PARTICIPANT_ID,
                recordDate
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

package com.detoxmate.notification.controller;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.notification.domain.DevicePlatform;
import com.detoxmate.notification.domain.FcmToken;
import com.detoxmate.notification.dto.RegisterFcmTokenRequest;
import com.detoxmate.notification.dto.RemoveFcmTokenRequest;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class FcmTokenControllerTest {

    private static final String TOKENS_URL = "/notifications/tokens";
    private static final Long TEST_USER_ID = 1L;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    FcmTokenRepository fcmTokenRepository;
    @Autowired
    JwtTokenProvider jwtTokenProvider;


    @Test
    @DisplayName("POST /tokens - 토큰이 DB에 저장된다")
    void register_savesToken()throws Exception {
        // given
        var request = new RegisterFcmTokenRequest("test-token-abc", DevicePlatform.IOS);

        // when & then
        mockMvc.perform(post(TOKENS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 실제 DB 상태로 검증
        FcmToken saved = fcmTokenRepository.findByToken("test-token-abc").orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.IOS);

    }

    @Test
    @DisplayName("POST /tokens - 같은 토큰 재등록 시 userId가 갱신된다")
    void register_replacesExistingToken() throws Exception{
        // given: 다른 유저가 이미 이 토큰을 등록한 상태
        fcmTokenRepository.save(FcmToken.create(99L, "shared-token", DevicePlatform.IOS));

        var request = new RegisterFcmTokenRequest("shared-token", DevicePlatform.IOS);

        // when
        mockMvc.perform(post(TOKENS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // then: 기존 건 delete + 새 건 insert
        FcmToken saved = fcmTokenRepository.findByToken("shared-token").orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(fcmTokenRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST /tokens — token 필드 누락 시 400")
    void register_missingToken_returns400() throws Exception {
        String body = """
                { "platform": "IOS" }
                """;

        mockMvc.perform(post(TOKENS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(fcmTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST /tokens — platform 필드 누락 시 400")
    void register_missingPlatform_returns400() throws Exception {
        String body = """
                { "token": "device-token-abc" }
                """;

        mockMvc.perform(post(TOKENS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(fcmTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("DELETE /tokens — 토큰이 DB에서 삭제된다")
    void remove_deletesToken() throws Exception {
        // given
        fcmTokenRepository.save(FcmToken.create(TEST_USER_ID, "to-delete", DevicePlatform.IOS));
        var request = new RemoveFcmTokenRequest("to-delete");

        // when & then
        mockMvc.perform(delete(TOKENS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        assertThat(fcmTokenRepository.findByToken("to-delete")).isEmpty();
    }

    @Test
    @DisplayName("인증되지 않은 요청은 401")
    void register_unauthenticated_returns401() throws Exception {
        //given
        var request = new RegisterFcmTokenRequest("device-token-abc", DevicePlatform.IOS);

        //when & then
        mockMvc.perform(post(TOKENS_URL)
                        // Authorization 헤더 없음
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }


}
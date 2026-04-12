package com.detoxmate.user.controller;

import com.detoxmate.auth.dto.KakaoSocialLoginResponse;
import com.detoxmate.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void providerAccessToken이_없으면_400_에러를_반환한다() throws Exception {
        // given
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(post("/auth/social/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(authService);
    }

    @Test
    void providerAccessToken이_있으면_로그인_응답을_반환한다() throws Exception {
        // given
        AuthService authService = mock(AuthService.class);
        when(authService.loginWithKakao("kakao-access-token")).thenReturn(new KakaoSocialLoginResponse(
                1L,
                "카카오닉네임",
                null,
                "service-access-token",
                3600L,
                false
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(post("/auth/social/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "providerAccessToken": "kakao-access-token"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.displayName").value("카카오닉네임"))
                .andExpect(jsonPath("$.accessToken").value("service-access-token"))
                .andExpect(jsonPath("$.accessTokenExpiresIn").value(3600))
                .andExpect(jsonPath("$.isNewUser").value(false));
    }

    @Test
    void providerAccessToken이_공백이면_400_에러를_반환한다() throws Exception {
        // given
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(post("/auth/social/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "providerAccessToken": "   "
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(authService);
    }
}

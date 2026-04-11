package com.detoxmate.user.controller;

import com.detoxmate.auth.dto.KakaoSocialLoginResponse;
import com.detoxmate.user.service.AuthService;
import com.detoxmate.user.service.KakaoRestApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    @Test
    void postSocialKakaoWithoutProviderAccessTokenReturnsBadRequest() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(new FakeAuthService())).build();

        mockMvc.perform(post("/auth/social/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postSocialKakaoWithProviderAccessTokenReturnsLoginResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(new FakeAuthService())).build();

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

    private static class FakeAuthService extends AuthService {

        FakeAuthService() {
            super(new KakaoRestApiClient());
        }

        @Override
        public KakaoSocialLoginResponse loginWithKakao(String providerAccessToken) {
            return new KakaoSocialLoginResponse(
                    1L,
                    "카카오닉네임",
                    "service-access-token",
                    3600L,
                    false
            );
        }
    }
}

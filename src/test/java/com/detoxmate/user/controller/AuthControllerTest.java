package com.detoxmate.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    @Test
    void postSocialKakaoWithoutProviderAccessTokenReturnsBadRequest() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController()).build();

        mockMvc.perform(post("/auth/social/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

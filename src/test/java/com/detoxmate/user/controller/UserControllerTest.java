package com.detoxmate.user.controller;

import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    @Test
    void Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        // given
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(mock(UserService.class)))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void Authorization_헤더가_있으면_유저_정보를_반환한다() throws Exception {
        // given
        UserService userService = mock(UserService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "카카오닉네임"));

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.displayName").value("카카오닉네임"));
    }

    @Test
    void 잘못된_JWT이면_401_에러를_반환한다() throws Exception {
        // given
        UserService userService = mock(UserService.class);
        when(userService.getMe("invalid-token")).thenThrow(new JwtException("invalid jwt"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 만료된_JWT이면_401_에러를_반환한다() throws Exception {
        // given
        UserService userService = mock(UserService.class);
        when(userService.getMe("expired-token")).thenThrow(new JwtException("expired jwt"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 토큰은_유효하지만_유저가_없으면_401_에러를_반환한다() throws Exception {
        // given
        UserService userService = mock(UserService.class);
        when(userService.getMe("missing-user-token")).thenThrow(new NoSuchElementException());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer missing-user-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 유효한_Authorization_헤더가_있으면_회원_탈퇴에_성공한다() throws Exception {
        // given
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(mock(UserService.class)))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(delete("/users/me").header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}

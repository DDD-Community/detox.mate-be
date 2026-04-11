package com.detoxmate.user.controller;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.repository.UserRepository;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    private static final String JWT_SECRET = "this-is-a-very-long-secret-key-for-temp-auth";

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
        UserService userService = new UserService(mock(UserRepository.class), new JwtTokenProvider(JWT_SECRET, 3600L));
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
        JwtTokenProvider expiredJwtTokenProvider = new JwtTokenProvider(JWT_SECRET, -1L);
        String expiredAccessToken = expiredJwtTokenProvider.createAccessToken(1L);
        UserService userService = new UserService(mock(UserRepository.class), expiredJwtTokenProvider);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + expiredAccessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 토큰은_유효하지만_유저가_없으면_401_에러를_반환한다() throws Exception {
        // given
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, 3600L);
        String accessToken = jwtTokenProvider.createAccessToken(999L);
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        UserService userService = new UserService(userRepository, jwtTokenProvider);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .build();

        // when & then
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }
}

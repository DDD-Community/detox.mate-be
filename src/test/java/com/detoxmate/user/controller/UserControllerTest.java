package com.detoxmate.user.controller;

import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    @Test
    void Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        // given
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(mock(UserService.class))).build();

        // when & then
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }
}

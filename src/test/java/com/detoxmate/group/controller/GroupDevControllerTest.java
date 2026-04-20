package com.detoxmate.group.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.group.service.GroupService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class GroupDevControllerTest {

    private GroupService groupService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        groupService = mock(GroupService.class);
        userService = mock(UserService.class);
        when(userService.getMe("access-token"))
                .thenReturn(new MyProfileResponse(1L, "지민", "https://..."));
        mockMvc = MockMvcBuilders.standaloneSetup(new GroupDevController(groupService))
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .setControllerAdvice(new com.detoxmate.common.error.GlobalExceptionHandler())
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 테스트를_위해_특정_그룹을_삭제할_수_있다() throws Exception {
        mockMvc.perform(delete("/groups/{id}", 1L)
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(result -> verify(groupService).deleteGroup(1L));
    }

    @Test
    void 그룹_삭제_요청에_Authorization_헤더가_없으면_401_에러를_반환한다() throws Exception {
        mockMvc.perform(delete("/groups/{id}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }
}

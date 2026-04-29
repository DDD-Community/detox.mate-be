package com.detoxmate.docs.poke.controller;

import com.detoxmate.auth.CurrentUserResolver;

import com.detoxmate.poke.controller.PokeController;
import com.detoxmate.poke.service.PokeService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
public class PokeControllerDocsTest {

    private MockMvc mockMvc;
    private PokeService pokeService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        pokeService = mock(PokeService.class);
        UserService userService = mock(UserService.class);

        given(userService.getMe("access-token"))
                .willReturn(new MyProfileResponse(1L, "테스트유저", "https://example.com/profile.png"));

        PokeController controller = new PokeController(pokeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 콕_찌르기() throws Exception {
        willDoNothing().given(pokeService).poke(eq(1L), eq(101L), eq(11L), eq(1L));

        mockMvc.perform(post("/group-challenges/{groupChallengeId}/activity-records/{activityRecordId}/members/{targetUserId}/poke", 1L, 101L, 11L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("group-challenges/poke-create",
                        preprocessRequest(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")
                        ),
                        pathParameters(
                                parameterWithName("groupChallengeId").description("챌린지 ID"),
                                parameterWithName("activityRecordId").description("인증글 ID"),
                                parameterWithName("targetUserId").description("콕 찌를 대상 유저 ID")
                        ),
                        resource(builder()
                                .tag("Poke")
                                .summary("콕 찌르기")
                                .description("같은 챌린지의 NOT_YET 상태 멤버에게 콕 알림을 보낸다. 동일 대상 일 1회 제한.")
                                .build()
                        )));
    }
}

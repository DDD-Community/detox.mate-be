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
                .willReturn(new MyProfileResponse(1L, "테스트유저", "https://example.com/profile.png", true));

        PokeController controller = new PokeController(pokeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 콕_찌르기() throws Exception {
        willDoNothing().given(pokeService).poke(eq(1L), eq(11L), eq(1L));

        mockMvc.perform(post("/challenge-records/{challengeRecordId}/pokes/{receiverUserId}", 1L, 11L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("challenge-records/poke-create",
                        preprocessRequest(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")
                        ),
                        pathParameters(
                                parameterWithName("challengeRecordId").description("챌린지 기록 ID"),
                                parameterWithName("receiverUserId").description("콕 찌르기 대상 유저 ID")
                        ),
                        resource(builder()
                                .tag("Poke")
                                .summary("콕 찌르기")
                                .description("오늘 인증 전 챌린지 기록에서 대상 유저를 콕 찌른다. 같은 대상에게는 한 번만 콕 찌르기 가능하다.")
                                .build()
                        )));
    }
}

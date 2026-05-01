package com.detoxmate.docs.reaction.controller;

import com.detoxmate.auth.CurrentUserResolver;

import com.detoxmate.docs.feed.mockdata.ReactionMockData;
import com.detoxmate.reaction.controller.ReactionController;
import com.detoxmate.reaction.dto.request.CreateReactionRequest;
import com.detoxmate.reaction.service.ReactionService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
public class ReactionControllerDocsTest {

    private MockMvc mockMvc;
    private ReactionService reactionService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        reactionService = mock(ReactionService.class);
        UserService userService = mock(UserService.class);

        given(userService.getMe("access-token"))
                .willReturn(new MyProfileResponse(1L, "테스트유저", "https://example.com/profile.png"));

        ReactionController controller = new ReactionController(reactionService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 리액션_추가() throws Exception {

    }

    @Test
    void 리액션_삭제() throws Exception {
        willDoNothing().given(reactionService).delete(eq(1L), eq(9001L), eq(1L));

        mockMvc.perform(delete("/group-challenges/{groupChallengeId}/reactions/{reactionId}", 1L, 9001L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("group-challenges/reaction-delete",
                        preprocessRequest(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")
                        ),
                        pathParameters(
                                parameterWithName("groupChallengeId").description("챌린지 ID"),
                                parameterWithName("reactionId").description("리액션 ID")
                        ),
                        resource(builder()
                                .tag("Reaction")
                                .summary("리액션 제거")
                                .description("본인이 단 리액션을 삭제한다.")
                                .build()
                        )));
    }
}

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
        given(reactionService.create(eq(1L), eq(101L), any(CreateReactionRequest.class), eq(1L)))
                .willReturn(ReactionMockData.createReactionResponse());

        mockMvc.perform(post("/group-challenges/{groupChallengeId}/stamps/{stampId}/reactions", 1L, 101L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reactionCode": "MUSCLE" }
                                """))
                .andExpect(status().isCreated())
                .andDo(document("group-challenges/reaction-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")
                        ),
                        pathParameters(
                                parameterWithName("groupChallengeId").description("챌린지 ID"),
                                parameterWithName("stampId").description("스탬프 ID")
                        ),
                        requestFields(
                                fieldWithPath("reactionCode").type(STRING).description("리액션 코드 (예: MUSCLE)")
                        ),
                        responseFields(
                                fieldWithPath("reactionId").type(NUMBER).description("생성된 리액션 ID"),
                                fieldWithPath("groupChallengeId").type(NUMBER).description("챌린지 ID"),
                                fieldWithPath("stampId").type(NUMBER).description("스탬프 ID"),
                                fieldWithPath("userId").type(NUMBER).description("리액션 단 유저 ID"),
                                fieldWithPath("reactionBody").type(STRING).description("리액션 종류"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각")
                        ),
                        resource(builder()
                                .tag("Reaction")
                                .summary("리액션 추가")
                                .description("스탬프에 리액션을 추가한다. (stamp_id, group_challenge_id, user_id, body) UNIQUE.")
                                .requestSchema(schema("CreateReactionRequest"))
                                .responseSchema(schema("ReactionResponse"))
                                .build()
                        )));
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

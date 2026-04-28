package com.detoxmate.docs.comment.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.comment.controller.CommentController;
import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.service.CommentService;
import com.detoxmate.docs.feed.mockdata.CommentMockData;
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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
public class CommentControllerDocsTest {

    private MockMvc mockMvc;
    private CommentService commentService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        commentService = mock(CommentService.class);
        UserService userService = mock(UserService.class);

        given(userService.getMe("access-token"))
                .willReturn(new MyProfileResponse(1L, "테스트유저", "https://example.com/profile.png"));

        CommentController controller = new CommentController(commentService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 댓글_목록_조회() throws Exception {
        given(commentService.list(eq(1L), eq(101L), any(), eq(20)))
                .willReturn(CommentMockData.createCommentListResponse());

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/stamps/{stampId}/comments", 1L, 101L)
                        .param("cursor", "eyJpZCI6MH0=")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("group-challenges/comments-list-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")
                        ),
                        pathParameters(
                                parameterWithName("groupChallengeId").description("챌린지 ID"),
                                parameterWithName("stampId").description("스탬프 ID")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("페이지 커서 (없으면 처음부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
                        ),
                        responseFields(
                                fieldWithPath("totalCount").type(NUMBER).description("전체 댓글 수"),
                                fieldWithPath("items[].commentId").type(NUMBER).description("댓글 ID"),
                                fieldWithPath("items[].author.userId").type(NUMBER).description("작성자 유저 ID"),
                                fieldWithPath("items[].author.displayName").type(STRING).description("작성자 닉네임"),
                                fieldWithPath("items[].author.profileImageUrl").type(STRING).description("작성자 프로필 URL"),
                                fieldWithPath("items[].body").type(STRING).description("댓글 본문"),
                                fieldWithPath("items[].relatedComment[].commentId").type(NUMBER).description("답글 ID"),
                                fieldWithPath("items[].relatedComment[].author.userId").type(NUMBER).description("답글 작성자 ID"),
                                fieldWithPath("items[].relatedComment[].author.displayName").type(STRING).description("답글 작성자 닉네임"),
                                fieldWithPath("items[].relatedComment[].author.profileImageUrl").type(STRING).description("답글 작성자 프로필 URL"),
                                fieldWithPath("items[].relatedComment[].body").type(STRING).description("답글 본문"),
                                fieldWithPath("items[].relatedComment[].createdAt").type(STRING).description("답글 생성 시각"),
                                fieldWithPath("items[].createdAt").type(STRING).description("댓글 생성 시각"),
                                fieldWithPath("items[].replyCount").type(NUMBER).description("답글 수"),
                                fieldWithPath("nextCursor").type(STRING).optional().description("다음 페이지 커서 (없으면 null)")
                        ),
                        resource(builder()
                                .tag("Comment")
                                .summary("댓글 목록 조회")
                                .description("스탬프의 댓글 목록을 커서 기반으로 조회한다.")
                                .responseSchema(schema("CommentListResponse"))
                                .build()
                        )));
    }

    @Test
    void 댓글_작성() throws Exception {
        given(commentService.create(eq(1L), eq(101L), any(CreateCommentRequest.class), eq(1L)))
                .willReturn(CommentMockData.createCommentResponse());

        mockMvc.perform(post("/group-challenges/{groupChallengeId}/stamps/{stampId}/comments", 1L, 101L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "commentMessage": "와 대박!",
                                    "parentCommentId": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andDo(document("group-challenges/comment-create",
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
                                fieldWithPath("commentMessage").type(STRING).description("댓글 본문"),
                                fieldWithPath("parentCommentId").type(NUMBER).optional()
                                        .description("답글일 경우 부모 댓글 ID (같은 stamp 내여야 함)")
                        ),
                        responseFields(
                                fieldWithPath("commentId").type(NUMBER).description("생성된 댓글 ID"),
                                fieldWithPath("groupChallengeId").type(NUMBER).description("챌린지 ID"),
                                fieldWithPath("stampId").type(NUMBER).description("스탬프 ID"),
                                fieldWithPath("userId").type(NUMBER).description("작성자 유저 ID"),
                                fieldWithPath("parentCommentId").type(NUMBER).optional().description("부모 댓글 ID (없으면 null)"),
                                fieldWithPath("commentMessage").type(STRING).description("댓글 본문"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각")
                        ),
                        resource(builder()
                                .tag("Comment")
                                .summary("댓글/답글 작성")
                                .description("스탬프에 댓글 또는 답글을 작성한다.")
                                .requestSchema(schema("CreateCommentRequest"))
                                .responseSchema(schema("CommentResponse"))
                                .build()
                        )));
    }
}
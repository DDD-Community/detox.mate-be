package com.detoxmate.docs.comment.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.comment.controller.CommentController;
import com.detoxmate.comment.dto.request.CreateCommentRequest;
import com.detoxmate.comment.dto.response.CommentResponse;
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

import java.time.LocalDateTime;

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
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
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
        given(commentService.list(eq(1L), eq("eyJpZCI6MH0="), eq(20)))
                .willReturn(CommentMockData.createCommentListResponse());

        mockMvc.perform(get("/challenge-records/{challengeRecordId}/comments", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .param("cursor", "eyJpZCI6MH0=")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("challenge-records/comments-list-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")
                        ),
                        pathParameters(
                                parameterWithName("challengeRecordId").description("챌린지 기록 ID")
                        ),
                        queryParameters(
                                parameterWithName("cursor").optional().description("다음 페이지 조회용 커서"),
                                parameterWithName("size").optional().description("조회할 댓글 개수. 기본값 20")
                        ),
                        responseFields(
                                fieldWithPath("totalCount").type(NUMBER).description("현재 댓글 상태의 전체 댓글 수"),
                                fieldWithPath("items").description("댓글 목록"),
                                fieldWithPath("items[].commentId").type(NUMBER).description("댓글 ID"),
                                fieldWithPath("items[].author").description("작성자 정보"),
                                fieldWithPath("items[].author.userId").type(NUMBER).description("작성자 유저 ID"),
                                fieldWithPath("items[].author.displayName").type(STRING).description("작성자 표시 이름"),
                                fieldWithPath("items[].author.profileImageUrl").type(STRING).description("저장된 작성자 프로필 이미지 object key를 읽기 URL로 변환한 값"),
                                fieldWithPath("items[].author.isUserWithdrawn").type(BOOLEAN).description("작성자가 회원 탈퇴한 사용자인지 여부"),
                                fieldWithPath("items[].commentBody").type(STRING).description("댓글 내용"),
                                fieldWithPath("items[].createdAt").type(STRING).description("댓글 생성 시각"),
                                fieldWithPath("nextCursor").type(STRING).description("다음 페이지 커서. 다음 페이지가 없으면 null")
                        ),
                        resource(builder()
                                .tag("Comment")
                                .summary("댓글 목록 조회")
                                .description("챌린지 기록의 현재 상태에 맞는 댓글 목록을 조회한다. 인증 전 기록이면 인증 전 댓글, 인증 후 기록이면 인증 후 댓글만 반환한다.")
                                .responseSchema(schema("CommentListResponse"))
                                .build()
                        )));
    }

    @Test
    void 댓글_작성() throws Exception {
        given(commentService.create(eq(1L), any(CreateCommentRequest.class), eq(1L)))
                .willReturn(new CommentResponse(
                        10L,
                        1L,
                        1L,
                        "오늘도 화이팅!",
                        LocalDateTime.parse("2026-05-01T10:30:00")
                ));

        mockMvc.perform(post("/challenge-records/{challengeRecordId}/comments", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "commentBody": "오늘도 화이팅!" }
                                """))
                .andExpect(status().isCreated())
                .andDo(document("challenge-records/comment-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")
                        ),
                        pathParameters(
                                parameterWithName("challengeRecordId").description("챌린지 기록 ID")
                        ),
                        requestFields(
                                fieldWithPath("commentBody").type(STRING).description("댓글 내용")
                        ),
                        responseFields(
                                fieldWithPath("commentId").type(NUMBER).description("생성된 댓글 ID"),
                                fieldWithPath("challengeRecordId").type(NUMBER).description("챌린지 기록 ID"),
                                fieldWithPath("userId").type(NUMBER).description("댓글 작성자 유저 ID"),
                                fieldWithPath("commentBody").type(STRING).description("댓글 내용"),
                                fieldWithPath("createdAt").type(STRING).description("댓글 생성 시각")
                        ),
                        resource(builder()
                                .tag("Comment")
                                .summary("댓글 작성")
                                .description("챌린지 기록에 댓글을 작성한다. 챌린지 기록 상태에 따라 인증 전 댓글 또는 인증 후 댓글로 저장된다.")
                                .requestSchema(schema("CreateCommentRequest"))
                                .responseSchema(schema("CommentResponse"))
                                .build()
                        )));
    }
}

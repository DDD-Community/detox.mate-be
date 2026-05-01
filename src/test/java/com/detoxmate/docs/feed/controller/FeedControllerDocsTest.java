package com.detoxmate.docs.feed.controller;


import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.feed.controller.FeedController;
import com.detoxmate.docs.feed.mockdata.FeedDetailMockData;
import com.detoxmate.docs.feed.mockdata.HomeFeedMockData;
import com.detoxmate.feed.service.FeedDetailService;
import com.detoxmate.feed.service.FeedService;
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
import static com.epages.restdocs.apispec.Schema.schema;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
public class FeedControllerDocsTest {

    private MockMvc mockMvc;
    private FeedService feedService;
    private FeedDetailService feedDetailService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        feedService = mock(FeedService.class);
        feedDetailService = mock(FeedDetailService.class);
        UserService userService = mock(UserService.class);

        given(userService.getMe("access-token"))
                .willReturn(new MyProfileResponse(1L, "테스트유저", "https://example.com/profile.png"));

        FeedController controller = new FeedController(feedService, feedDetailService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 홈_피드_조회() throws Exception {
        given(feedService.getHomeFeed(eq(1L), eq(1L)))
                .willReturn(HomeFeedMockData.createHomeFeedResponse());

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/home", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("group-challenges/home-feed-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")
                        ),
                        pathParameters(
                                parameterWithName("groupChallengeId").description("챌린지 ID")
                        ),
                        responseFields(
                                fieldWithPath("challenge.groupChallengeId").type(NUMBER).description("챌린지 ID"),
                                fieldWithPath("challenge.groupChallengeName").type(STRING).description("챌린지 이름"),
                                fieldWithPath("challenge.startAt").type(STRING).description("시작 시각 (ISO-8601)"),
                                fieldWithPath("challenge.streakCount").type(NUMBER).description("연속 인증 일수"),
                                fieldWithPath("members[].userId").type(NUMBER).description("유저 ID"),
                                fieldWithPath("members[].groupMemberId").type(NUMBER).description("그룹 멤버 ID"),
                                fieldWithPath("members[].displayName").type(STRING).description("닉네임"),
                                fieldWithPath("members[].profileImageUrl").type(STRING).description("프로필 이미지 URL"),
                                fieldWithPath("members[].challengeStatus").type(STRING)
                                        .description("인증 상태 (NOT_YET | VERIFIED | FAILED)"),
                                fieldWithPath("members[].activityImageUrl").type(STRING).optional()
                                        .description("활동 사진 URL (미인증 시 null)"),
                                fieldWithPath("members[].oneLineReview").type(STRING).optional()
                                        .description("한줄평 (미인증 시 null)"),
                                fieldWithPath("members[].totalUsedMinutes").type(NUMBER).optional()
                                        .description("누적 사용 시간(분)"),
                                fieldWithPath("members[].goalMinutes").type(STRING).description("목표 시간 (예: 8H 30M)"),
                                fieldWithPath("members[].activityRecordId").type(NUMBER).optional()
                                        .description("활동 인증 기록 ID (미인증 시 null)"),
                                fieldWithPath("members[].reactionCount").type(NUMBER).description("리액션 수"),
                                fieldWithPath("members[].commentCount").type(NUMBER).description("댓글 수"),
                                fieldWithPath("members[].pokeCount").type(NUMBER).description("받은 콕 수"),
                                fieldWithPath("members[].isPoked").type(BOOLEAN).description("내가 콕 찔렀는지 여부")
                        ),
                        resource(builder()
                                .tag("Feed")
                                .summary("홈 피드 조회")
                                .description("진행 중인 챌린지의 홈 피드(챌린지 요약 + 멤버별 카드)를 반환한다.")
                                .responseSchema(schema("HomeFeedResponse"))
                                .build()
                        )));
    }

    @Test
    void 피드_상세_조회() throws Exception {
        given(feedDetailService.getFeedDetail(eq(1L), eq(101L), eq(1L)))
                .willReturn(FeedDetailMockData.createFeedDetailResponse());

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/stamps/{stampId}", 1L, 101L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("group-challenges/feed-detail-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")
                        ),
                        pathParameters(
                                parameterWithName("groupChallengeId").description("챌린지 ID"),
                                parameterWithName("stampId").description("스탬프 ID")
                        ),
                        responseFields(
                                fieldWithPath("stampId").type(NUMBER).description("스탬프 ID"),
                                fieldWithPath("groupChallengeId").type(NUMBER).description("챌린지 ID"),
                                fieldWithPath("author.userId").type(NUMBER).description("작성자 유저 ID"),
                                fieldWithPath("author.displayName").type(STRING).description("작성자 닉네임"),
                                fieldWithPath("author.profileImageUrl").type(STRING).description("작성자 프로필 URL"),
                                fieldWithPath("createdAt").type(STRING).description("생성 시각"),
                                fieldWithPath("activityImageUrl").type(STRING).description("활동 사진 URL"),
                                fieldWithPath("oneLineReview").type(STRING).description("한줄평"),
                                fieldWithPath("goalStatus").type(STRING).description("목표 달성 상태 (SUCCESS | FAIL)"),
                                fieldWithPath("snapshotGoalMinutes").type(NUMBER).description("당시 목표 시간(분)"),
                                fieldWithPath("details[].usageGoalTypeCode").type(STRING).description("사용 목표 타입 코드"),
                                fieldWithPath("details[].usedMinutes").type(NUMBER).description("실제 사용 시간(분)"),
                                fieldWithPath("reactions.totalCount").type(NUMBER).description("총 리액션 수"),
                                fieldWithPath("reactions.summary[].reactionBody").type(STRING).description("리액션 종류"),
                                fieldWithPath("reactions.summary[].userId").type(NUMBER).description("리액션 단 유저 ID"),
                                fieldWithPath("reactions.summary[].username").type(STRING).description("유저명"),
                                fieldWithPath("reactions.summary[].profileImageUrl").type(STRING).description("프로필 URL"),
                                fieldWithPath("commentCount").type(NUMBER).description("댓글 수")
                        ),
                        resource(builder()
                                .tag("Feed")
                                .summary("피드 상세 조회")
                                .description("스탬프(피드) 단건의 상세 정보를 반환한다.")
                                .responseSchema(schema("FeedDetailResponse"))
                                .build()
                        )));
    }
}

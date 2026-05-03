package com.detoxmate.docs.feed.controller;


import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.docs.feed.mockdata.FeedDetailMockData;
import com.detoxmate.docs.feed.mockdata.HomeFeedMockData;
import com.detoxmate.feed.controller.FeedController;
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
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;
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
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.JsonFieldType.VARIES;
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

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = homeFeedPathParameters();
        FieldDescriptor[] responseFieldDescriptors = homeFeedResponseFields();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/home", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("group-challenges/home-feed-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(builder()
                                .tag("Feed")
                                .summary("홈 피드 조회")
                                .description("그룹 챌린지의 오늘 홈 피드를 조회한다. 오늘 챌린지 기록이 없으면 빈 챌린지 기록을 생성한 뒤 멤버 카드를 반환한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("HomeFeedResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 피드_상세_조회() throws Exception {
        given(feedDetailService.getFeedDetail(eq(1000L), eq(1L)))
                .willReturn(FeedDetailMockData.createFeedDetailResponse());

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = feedDetailPathParameters();
        FieldDescriptor[] responseFieldDescriptors = feedDetailResponseFields();

        mockMvc.perform(get("/challenge-records/{challengeRecordId}", 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("challenge-records/feed-detail-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(builder()
                                .tag("Feed")
                                .summary("피드 상세 조회")
                                .description("챌린지 기록 상세를 조회한다. 인증 전 기록은 인증 정보와 리액션이 null 또는 빈 값이고, 인증 후 기록은 인증 정보와 리액션 요약을 포함한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("FeedDetailResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private ParameterDescriptor[] homeFeedPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("groupChallengeId").description("그룹 챌린지 ID")
        };
    }

    private ParameterDescriptor[] feedDetailPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("challengeRecordId").description("챌린지 기록 ID")
        };
    }

    private FieldDescriptor[] homeFeedResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("challenge.groupChallengeId").type(NUMBER).description("그룹 챌린지 ID"),
                fieldWithPath("challenge.groupChallengeName").type(STRING).description("그룹 챌린지 이름"),
                fieldWithPath("challenge.startAt").type(STRING).description("챌린지 시작 시각"),
                fieldWithPath("challenge.streakCount").type(NUMBER).description("연속 인증 일수"),
                fieldWithPath("members[].userId").type(NUMBER).description("유저 ID"),
                fieldWithPath("members[].groupMemberId").type(NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("members[].displayName").type(STRING).description("닉네임"),
                fieldWithPath("members[].profileImageUrl").type(STRING).description("프로필 이미지 URL"),
                fieldWithPath("members[].challengeRecordId").type(NUMBER).description("오늘 챌린지 기록 ID"),
                fieldWithPath("members[].challengeStatus").type(STRING)
                        .description("챌린지 기록 상태 (BEFORE_RECORD | AFTER_RECORD_SUCCESS | AFTER_RECORD_FAIL)"),
                fieldWithPath("members[].activityImageUrl").type(VARIES).optional()
                        .description("활동 사진 object key 또는 URL. 인증 전이면 null"),
                fieldWithPath("members[].oneLineReview").type(VARIES).optional()
                        .description("한줄평. 인증 전이면 null"),
                fieldWithPath("members[].totalUsedMinutes").type(VARIES).optional()
                        .description("총 사용 시간(분). 인증 전이면 null"),
                fieldWithPath("members[].goalMinutes").type(VARIES).optional()
                        .description("목표 시간 표시값. 없으면 null"),
                fieldWithPath("members[].activityRecordId").type(VARIES).optional()
                        .description("활동 인증 기록 ID. 인증 전이면 null"),
                fieldWithPath("members[].verifiedAt").type(VARIES).optional()
                        .description("인증 생성 시각. 인증 전이면 null"),
                fieldWithPath("members[].reactionCount").type(NUMBER).description("리액션 수"),
                fieldWithPath("members[].commentCount").type(NUMBER).description("댓글 수"),
                fieldWithPath("members[].pokeCount").type(NUMBER).description("받은 콕 수"),
                fieldWithPath("members[].isPoked").type(BOOLEAN).description("내가 콕 찔렀는지 여부")
        };
    }

    private FieldDescriptor[] feedDetailResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("challengeRecordId").type(NUMBER).description("챌린지 기록 ID"),
                fieldWithPath("groupChallengeId").type(NUMBER).description("그룹 챌린지 ID"),
                fieldWithPath("activityRecordId").type(NUMBER).description("활동 인증 기록 ID. 인증 전이면 null"),
                fieldWithPath("challengeStatus").type(STRING)
                        .description("챌린지 기록 상태 (BEFORE_RECORD | AFTER_RECORD_SUCCESS | AFTER_RECORD_FAIL)"),
                fieldWithPath("recordDate").type(STRING).description("기록 날짜"),
                fieldWithPath("author.userId").type(NUMBER).description("작성자 유저 ID"),
                fieldWithPath("author.displayName").type(STRING).description("작성자 닉네임"),
                fieldWithPath("author.profileImageUrl").type(STRING).description("작성자 프로필 이미지 URL"),
                fieldWithPath("activityCreatedAt").type(STRING).description("인증 생성 시각. 인증 전이면 null"),
                fieldWithPath("activityImageUrl").type(STRING).description("활동 사진 object key 또는 URL. 인증 전이면 null"),
                fieldWithPath("oneLineReview").type(STRING).description("한줄평. 인증 전이면 null"),
                fieldWithPath("goalStatus").type(STRING).description("목표 달성 결과 (SUCCESS | FAIL). 인증 전이면 null"),
                fieldWithPath("snapshotGoalMinutes").type(NUMBER).description("인증 당시 목표 시간 합계(분). 인증 전이면 null"),
                fieldWithPath("details").type(ARRAY).description("인증 상세 사용량 목록. 인증 전이면 빈 배열"),
                fieldWithPath("details[].usageGoalTypeCode").type(STRING).description("사용 목표 타입 코드"),
                fieldWithPath("details[].usedMinutes").type(NUMBER).description("사용 시간(분)"),
                fieldWithPath("reactions.totalCount").type(NUMBER).description("리액션 총 개수"),
                fieldWithPath("reactions.summary").type(ARRAY).description("리액션 최신순 요약 목록"),
                fieldWithPath("reactions.summary[].reactionBody").type(STRING).description("리액션 종류"),
                fieldWithPath("reactions.summary[].userId").type(NUMBER).description("리액션 작성자 유저 ID"),
                fieldWithPath("reactions.summary[].displayName").type(STRING).description("리액션 작성자 닉네임"),
                fieldWithPath("reactions.summary[].profileImageUrl").type(STRING).description("리액션 작성자 프로필 이미지 URL"),
                fieldWithPath("commentCount").type(NUMBER).description("현재 상세 상태에 해당하는 댓글 수"),
                fieldWithPath("pokeCount").type(NUMBER).description("받은 콕 수"),
                fieldWithPath("pokeable").type(BOOLEAN).description("현재 사용자가 콕 찌를 수 있는지 여부"),
                fieldWithPath("poked").type(BOOLEAN).description("현재 사용자가 이미 콕 찔렀는지 여부"),
                fieldWithPath("pokedUsers").type(ARRAY).description("콕 찌른 유저 목록. 인증 후 상세이면 빈 배열")
        };
    }
}

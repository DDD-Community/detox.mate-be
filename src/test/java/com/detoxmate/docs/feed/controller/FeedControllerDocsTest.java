package com.detoxmate.docs.feed.controller;


import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.docs.feed.mockdata.HomeFeedMockData;
import com.detoxmate.feed.controller.FeedController;
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
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.JsonFieldType.VARIES;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
public class FeedControllerDocsTest {

    private MockMvc mockMvc;
    private FeedService feedService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        feedService = mock(FeedService.class);
        UserService userService = mock(UserService.class);

        given(userService.getMe("access-token"))
                .willReturn(new MyProfileResponse(1L, "테스트유저", "https://example.com/profile.png"));

        FeedController controller = new FeedController(feedService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void 그룹_챌린지_개요_조회() throws Exception {
        given(feedService.getGroupChallengeOverview(eq(1L), eq(1L)))
                .willReturn(HomeFeedMockData.createGroupChallengeOverviewResponse());

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = homeFeedPathParameters();
        FieldDescriptor[] responseFieldDescriptors = groupChallengeOverviewResponseFields();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/overview", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("group-challenges/overview-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(builder()
                                .tag("Feed")
                                .summary("그룹 챌린지 홈 개요 조회")
                                .description("홈 화면의 피드 목록 외 챌린지/모임 개요 정보를 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("GroupChallengeOverviewResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
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
                                .description("[Deprecated] 기존 호환용 API다. 신규 클라이언트는 GET /group-challenges/{groupChallengeId}/overview와 피드 목록 API를 분리해서 사용한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("HomeFeedResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 홈_챌린지_기록_피드_조회() throws Exception {
        given(feedService.getTodayChallengeRecordFeed(eq(1L), eq(1L)))
                .willReturn(HomeFeedMockData.createGroupChallengeRecordFeedResponse());

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = homeFeedPathParameters();
        FieldDescriptor[] responseFieldDescriptors = groupChallengeRecordFeedResponseFields();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/challenge-records/today", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("group-challenges/challenge-records-today-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(builder()
                                .tag("Feed")
                                .summary("홈 챌린지 기록 피드 조회")
                                .description("홈 화면에서 오늘 피드를 조회한다. 누락된 오늘 챌린지 기록을 생성하고 활성 멤버만 반환한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("GroupChallengeRecordFeedResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 히스토리_챌린지_기록_피드_조회() throws Exception {
        given(feedService.getHistoryChallengeRecordFeed(
                eq(1L),
                eq(java.time.LocalDate.of(2026, 5, 2)),
                eq(1L)
        ))
                .willReturn(HomeFeedMockData.createGroupChallengeRecordFeedResponse());

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = homeFeedPathParameters();
        ParameterDescriptor[] queryParameterDescriptors = historyFeedQueryParameters();
        FieldDescriptor[] responseFieldDescriptors = groupChallengeRecordFeedResponseFields();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/challenge-records", 1L)
                        .queryParam("date", "2026-05-02")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("group-challenges/challenge-records-history-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        queryParameters(queryParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(builder()
                                .tag("Feed")
                                .summary("히스토리 챌린지 기록 피드 조회")
                                .description("캘린더 히스토리 화면에서 과거 날짜의 피드를 조회한다. 오늘 이전 날짜만 허용하고 기록을 생성하지 않는다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .queryParameters(queryParameterDescriptors)
                                .responseSchema(schema("GroupChallengeRecordFeedResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 피드_상세_조회() throws Exception {
        given(feedService.getGroupChallengeRecordDetail(eq(1L), eq(1000L), eq(1L)))
                .willReturn(HomeFeedMockData.createGroupChallengeRecordFeedDetailResponse());

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = feedDetailPathParameters();
        FieldDescriptor[] responseFieldDescriptors = groupChallengeRecordFeedDetailResponseFields();

        mockMvc.perform(get("/group-challenges/{groupChallengeId}/challenge-records/{challengeRecordId}", 1L, 1000L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("group-challenges/challenge-record-detail-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(builder()
                                .tag("Feed")
                                .summary("피드 상세 조회")
                                .description("챌린지 기록 피드 상세를 리스트 카드와 같은 인터페이스로 조회한다. 상세에서는 콕/리액션 상세 필드를 함께 제공한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("GroupChallengeRecordFeedDetailResponse"))
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
                parameterWithName("groupChallengeId").description("그룹 챌린지 ID"),
                parameterWithName("challengeRecordId").description("챌린지 기록 ID")
        };
    }

    private ParameterDescriptor[] historyFeedQueryParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("date").description("조회 날짜(yyyy-MM-dd, KST 기준). 오늘 이전 날짜만 허용")
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
                fieldWithPath("members[].profileImageUrl").type(STRING).description("저장된 프로필 이미지 object key를 읽기 URL로 변환한 값"),
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

    private FieldDescriptor[] groupChallengeOverviewResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("groupChallengeId").type(NUMBER).description("그룹 챌린지 ID"),
                fieldWithPath("groupId").type(NUMBER).description("그룹 ID"),
                fieldWithPath("groupName").type(STRING).description("그룹 이름"),
                fieldWithPath("challengeNo").type(NUMBER).description("그룹 내 챌린지 회차"),
                fieldWithPath("status").type(STRING).description("그룹 챌린지 상태"),
                fieldWithPath("startAt").type(STRING).optional().description("챌린지 시작 시각"),
                fieldWithPath("endAt").type(STRING).optional().description("챌린지 종료 시각"),
                fieldWithPath("streakCount").type(NUMBER).description("연속 인증 일수")
        };
    }

    private FieldDescriptor[] groupChallengeRecordFeedResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("groupId").type(NUMBER).description("그룹 ID"),
                fieldWithPath("date").type(STRING).description("조회 날짜"),
                fieldWithPath("dailySummary").type(OBJECT).description("선택 날짜의 그룹 인증 집계"),
                fieldWithPath("dailySummary.date").type(STRING).description("KST 기준 날짜"),
                fieldWithPath("dailySummary.dayStatus").type(STRING)
                        .description("날짜 상태 (NOT_STARTED | CONFIRMED | IN_PROGRESS | FUTURE)"),
                fieldWithPath("dailySummary.result").type(STRING)
                        .description("일별 그룹 인증 결과 (ALL | HALF | RESET). 확정 전이면 null").optional(),
                fieldWithPath("dailySummary.activeMemberCount").type(NUMBER).description("해당 날짜의 활동중 멤버 수"),
                fieldWithPath("dailySummary.certifiedMemberCount").type(NUMBER).description("해당 날짜에 인증한 활동중 멤버 수"),
                fieldWithPath("dailySummary.requiredCount").type(NUMBER).description("그룹 인증 성공에 필요한 인증자 수"),
                fieldWithPath("members").type(ARRAY).description("선택 날짜의 멤버별 챌린지 기록 피드"),
                fieldWithPath("members[].groupMemberId").type(NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("members[].groupChallengeParticipantId").type(NUMBER).description("그룹 챌린지 참가자 ID"),
                fieldWithPath("members[].userId").type(NUMBER).description("사용자 ID"),
                fieldWithPath("members[].displayName").type(STRING).description("사용자 표시 이름"),
                fieldWithPath("members[].profileImageUrl").type(STRING).description("프로필 이미지 URL").optional(),
                fieldWithPath("members[].isMe").type(BOOLEAN).description("로그인 사용자인지 여부"),
                fieldWithPath("members[].memberStatus").type(STRING).description("그룹 멤버 상태"),
                fieldWithPath("members[].participantStatus").type(STRING).description("챌린지 참가 상태"),
                fieldWithPath("members[].dailyStatus").type(STRING)
                        .description("멤버 일별 상태 (GOAL_ACHIEVED | GOAL_FAILED | NOT_CERTIFIED | NOT_ACTIVE)"),
                fieldWithPath("members[].includedInGroupResult").type(BOOLEAN)
                        .description("해당 날짜 그룹 인증 계산 대상인지 여부"),
                fieldWithPath("members[].goals").type(ARRAY).description("선택 날짜에 유효한 목표 목록"),
                fieldWithPath("members[].goals[].userUsageGoalTimeId").type(NUMBER)
                        .description("user_usage_goal_times ID").optional(),
                fieldWithPath("members[].goals[].usageGoalType").type(STRING).description("목표 타입").optional(),
                fieldWithPath("members[].goals[].goalMinutes").type(NUMBER).description("목표 시간(분)").optional(),
                fieldWithPath("members[].goals[].effectiveDate").type(STRING).description("목표 적용 시작일").optional(),
                fieldWithPath("members[].challengeRecordId").type(NUMBER)
                        .description("challenge-records 댓글/리액션/콕/상세 API 호출에 사용하는 챌린지 기록 ID")
                        .optional(),
                fieldWithPath("members[].activityRecord").type(VARIES)
                        .description("선택 날짜의 활동 인증 내용. 인증하지 않았으면 null").optional(),
                fieldWithPath("members[].activityRecord.submittedAt").type(STRING).description("인증 제출 일시").optional(),
                fieldWithPath("members[].activityRecord.activityImageUrl").type(STRING).description("활동 이미지 URL").optional(),
                fieldWithPath("members[].activityRecord.reflectionText").type(STRING).description("회고 텍스트").optional(),
                fieldWithPath("members[].activityRecord.allAchieved").type(BOOLEAN).description("모든 목표 달성 여부").optional(),
                fieldWithPath("members[].activityRecord.details").type(ARRAY).description("목표 타입별 사용 시간과 달성 여부").optional(),
                fieldWithPath("members[].activityRecord.details[].usageGoalType").type(STRING).description("목표 타입").optional(),
                fieldWithPath("members[].activityRecord.details[].usedMinutes").type(NUMBER)
                        .description("인증 시 제출된 사용 시간(분)").optional(),
                fieldWithPath("members[].activityRecord.details[].goalMinutes").type(NUMBER)
                        .description("인증 날짜에 유효했던 목표 시간(분)").optional(),
                fieldWithPath("members[].activityRecord.details[].isAchieved").type(BOOLEAN)
                        .description("해당 목표 타입의 달성 여부").optional(),
                fieldWithPath("members[].reactionCount").type(NUMBER).description("리액션 수"),
                fieldWithPath("members[].commentCount").type(NUMBER).description("댓글 수"),
                fieldWithPath("members[].pokeCount").type(NUMBER).description("받은 콕 수"),
                fieldWithPath("members[].isPoked").type(BOOLEAN).description("현재 사용자가 콕 찔렀는지 여부"),
                fieldWithPath("members[].pokeable").type(BOOLEAN)
                        .description("상세 조회에서만 제공하는 현재 사용자의 콕 가능 여부").optional(),
                fieldWithPath("members[].pokedUsers").type(ARRAY)
                        .description("상세 조회에서만 제공하는 콕 찌른 사용자 목록").optional(),
                fieldWithPath("members[].pokedUsers[].userId").type(NUMBER)
                        .description("콕 찌른 사용자 ID").optional(),
                fieldWithPath("members[].pokedUsers[].displayName").type(STRING)
                        .description("콕 찌른 사용자 표시 이름").optional(),
                fieldWithPath("members[].pokedUsers[].profileImageUrl").type(STRING)
                        .description("콕 찌른 사용자 프로필 이미지 URL").optional(),
                fieldWithPath("members[].reactions").type(OBJECT)
                        .description("상세 조회에서만 제공하는 리액션 요약").optional(),
                fieldWithPath("members[].reactions.totalCount").type(NUMBER).description("리액션 목록 수").optional(),
                fieldWithPath("members[].reactions.summary").type(ARRAY).description("리액션 목록").optional(),
                fieldWithPath("members[].reactions.summary[].reactionBody").type(STRING).description("리액션 타입").optional(),
                fieldWithPath("members[].reactions.summary[].userId").type(NUMBER).description("리액션 작성자 ID").optional(),
                fieldWithPath("members[].reactions.summary[].displayName").type(STRING)
                        .description("리액션 작성자 표시 이름").optional(),
                fieldWithPath("members[].reactions.summary[].profileImageUrl").type(STRING)
                        .description("리액션 작성자 프로필 이미지 URL").optional()
        };
    }

    private FieldDescriptor[] groupChallengeRecordFeedDetailResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("groupMemberId").type(NUMBER).description("그룹 멤버 ID"),
                fieldWithPath("groupChallengeParticipantId").type(NUMBER).description("그룹 챌린지 참가자 ID"),
                fieldWithPath("userId").type(NUMBER).description("사용자 ID"),
                fieldWithPath("displayName").type(STRING).description("사용자 표시 이름"),
                fieldWithPath("profileImageUrl").type(STRING).description("프로필 이미지 URL").optional(),
                fieldWithPath("isMe").type(BOOLEAN).description("로그인 사용자인지 여부"),
                fieldWithPath("memberStatus").type(STRING).description("그룹 멤버 상태"),
                fieldWithPath("participantStatus").type(STRING).description("챌린지 참가 상태"),
                fieldWithPath("dailyStatus").type(STRING)
                        .description("멤버 일별 상태 (GOAL_ACHIEVED | GOAL_FAILED | NOT_CERTIFIED | NOT_ACTIVE)"),
                fieldWithPath("includedInGroupResult").type(BOOLEAN)
                        .description("해당 날짜 그룹 인증 계산 대상인지 여부"),
                fieldWithPath("goals").type(ARRAY).description("선택 날짜에 유효한 목표 목록"),
                fieldWithPath("goals[].userUsageGoalTimeId").type(NUMBER).description("user_usage_goal_times ID").optional(),
                fieldWithPath("goals[].usageGoalType").type(STRING).description("목표 타입").optional(),
                fieldWithPath("goals[].goalMinutes").type(NUMBER).description("목표 시간(분)").optional(),
                fieldWithPath("goals[].effectiveDate").type(STRING).description("목표 적용 시작일").optional(),
                fieldWithPath("challengeRecordId").type(NUMBER)
                        .description("challenge-records 댓글/리액션/콕 API 호출에 사용하는 챌린지 기록 ID"),
                fieldWithPath("activityRecord").type(VARIES)
                        .description("선택 날짜의 활동 인증 내용. 인증하지 않았으면 null").optional(),
                fieldWithPath("activityRecord.submittedAt").type(STRING).description("인증 제출 일시").optional(),
                fieldWithPath("activityRecord.activityImageUrl").type(STRING).description("활동 이미지 URL").optional(),
                fieldWithPath("activityRecord.reflectionText").type(STRING).description("회고 텍스트").optional(),
                fieldWithPath("activityRecord.allAchieved").type(BOOLEAN).description("모든 목표 달성 여부").optional(),
                fieldWithPath("activityRecord.details").type(ARRAY).description("목표 타입별 사용 시간과 달성 여부").optional(),
                fieldWithPath("activityRecord.details[].usageGoalType").type(STRING).description("목표 타입").optional(),
                fieldWithPath("activityRecord.details[].usedMinutes").type(NUMBER)
                        .description("인증 시 제출된 사용 시간(분)").optional(),
                fieldWithPath("activityRecord.details[].goalMinutes").type(NUMBER)
                        .description("인증 날짜에 유효했던 목표 시간(분)").optional(),
                fieldWithPath("activityRecord.details[].isAchieved").type(BOOLEAN)
                        .description("해당 목표 타입의 달성 여부").optional(),
                fieldWithPath("reactionCount").type(NUMBER).description("리액션 수"),
                fieldWithPath("commentCount").type(NUMBER).description("댓글 수"),
                fieldWithPath("pokeCount").type(NUMBER).description("받은 콕 수"),
                fieldWithPath("isPoked").type(BOOLEAN).description("현재 사용자가 콕 찔렀는지 여부"),
                fieldWithPath("pokeable").type(BOOLEAN).description("현재 사용자의 콕 가능 여부").optional(),
                fieldWithPath("pokedUsers").type(ARRAY).description("콕 찌른 사용자 목록").optional(),
                fieldWithPath("pokedUsers[].userId").type(NUMBER).description("콕 찌른 사용자 ID").optional(),
                fieldWithPath("pokedUsers[].displayName").type(STRING).description("콕 찌른 사용자 표시 이름").optional(),
                fieldWithPath("pokedUsers[].profileImageUrl").type(STRING).description("콕 찌른 사용자 프로필 이미지 URL").optional(),
                fieldWithPath("reactions").type(OBJECT).description("리액션 요약").optional(),
                fieldWithPath("reactions.totalCount").type(NUMBER).description("리액션 목록 수").optional(),
                fieldWithPath("reactions.summary").type(ARRAY).description("리액션 목록").optional(),
                fieldWithPath("reactions.summary[].reactionBody").type(STRING).description("리액션 타입").optional(),
                fieldWithPath("reactions.summary[].userId").type(NUMBER).description("리액션 작성자 ID").optional(),
                fieldWithPath("reactions.summary[].displayName").type(STRING)
                        .description("리액션 작성자 표시 이름").optional(),
                fieldWithPath("reactions.summary[].profileImageUrl").type(STRING)
                        .description("리액션 작성자 프로필 이미지 URL").optional()
        };
    }

}

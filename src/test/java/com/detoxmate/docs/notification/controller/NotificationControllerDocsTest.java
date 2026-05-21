package com.detoxmate.docs.notification.controller;

import com.detoxmate.auth.CurrentUserResolver;
import com.detoxmate.notification.controller.FcmTokenController;
import com.detoxmate.notification.controller.NotificationHistoryController;
import com.detoxmate.notification.dto.NotificationHistoryGroupResponse;
import com.detoxmate.notification.dto.NotificationHistoryItemResponse;
import com.detoxmate.notification.dto.NotificationHistoryListResponse;
import com.detoxmate.notification.dto.NotificationNavigationResponse;
import com.detoxmate.notification.service.FcmTokenService;
import com.detoxmate.notification.service.NotificationHistoryService;
import com.detoxmate.notification.service.NotificationNavigationService;
import com.detoxmate.user.controller.UserController;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
class NotificationControllerDocsTest {

    private MockMvc mockMvc;
    private FcmTokenService fcmTokenService;
    private NotificationHistoryService notificationHistoryService;
    private NotificationNavigationService notificationNavigationService;
    private UserService userService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        fcmTokenService = mock(FcmTokenService.class);
        notificationHistoryService = mock(NotificationHistoryService.class);
        notificationNavigationService = mock(NotificationNavigationService.class);
        userService = mock(UserService.class);

        given(userService.getMe("access-token"))
                .willReturn(new MyProfileResponse(1L, "테스트유저", "https://example.com/profile.png", true));

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new FcmTokenController(fcmTokenService),
                        new NotificationHistoryController(notificationHistoryService, notificationNavigationService),
                        new UserController(userService)
                )
                .setCustomArgumentResolvers(new CurrentUserResolver(userService))
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void FCM_토큰을_등록한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = registerTokenRequestFields();

        mockMvc.perform(post("/notifications/tokens")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
                                  "platform": "IOS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andDo(result -> verify(fcmTokenService).register(
                        eq(1L),
                        eq("ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]"),
                        eq(com.detoxmate.notification.domain.DevicePlatform.IOS)
                ))
                .andDo(document("notifications/tokens-register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        resource(builder()
                                .tag("Notification")
                                .summary("FCM 토큰 등록")
                                .description("사용자가 알림을 허용했을 때 클라이언트의 Expo Push Token과 플랫폼을 등록한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("RegisterFcmTokenRequest"))
                                .requestFields(requestFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void FCM_토큰을_삭제한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = removeTokenRequestFields();

        mockMvc.perform(delete("/notifications/tokens")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andDo(result -> verify(fcmTokenService).remove(
                        eq(1L),
                        eq("ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
                ))
                .andDo(document("notifications/tokens-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        resource(builder()
                                .tag("Notification")
                                .summary("FCM 토큰 삭제")
                                .description("사용자가 앱 내 알림을 끄거나 토큰이 더 이상 필요하지 않을 때 등록된 Expo Push Token을 삭제한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("RemoveFcmTokenRequest"))
                                .requestFields(requestFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 알림_수신_설정을_변경한다() throws Exception {
        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] requestFieldDescriptors = updatePushNotificationSettingRequestFields();

        mockMvc.perform(patch("/users/me/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pushNotificationEnabled": false
                                }
                                """))
                .andExpect(status().isNoContent())
                .andDo(result -> verify(userService).updatePushNotificationSetting(eq(1L), eq(false)))
                .andDo(document("notifications/settings-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        requestFields(requestFieldDescriptors),
                        resource(builder()
                                .tag("Notification")
                                .summary("알림 수신 설정 변경")
                                .description("로그인 사용자의 푸시 알림 수신 여부를 변경한다. 수신 거부 상태여도 알림 히스토리는 저장된다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .requestSchema(schema("UpdatePushNotificationSettingRequest"))
                                .requestFields(requestFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 알림_목록을_조회한다() throws Exception {
        given(notificationHistoryService.getMyNotifications(eq(1L)))
                .willReturn(new NotificationHistoryListResponse(
                        3,
                        List.of(
                                new NotificationHistoryGroupResponse(
                                        "오늘",
                                        List.of(
                                                new NotificationHistoryItemResponse(
                                                        100L,
                                                        "인증 알림",
                                                        "슬빈님이 인증을 업로드했습니다. 반응을 남겨보세요!",
                                                        false,
                                                        "FEED",
                                                        10L,
                                                        "NONE",
                                                        null,
                                                        LocalDateTime.of(2026, 5, 17, 14, 30)
                                                ),
                                                new NotificationHistoryItemResponse(
                                                        101L,
                                                        "댓글 알림",
                                                        "의진님이 댓글을 남겼습니다: \"좋아요\"",
                                                        true,
                                                        "FEED_DETAIL",
                                                        200L,
                                                        "COMMENT",
                                                        300L,
                                                        LocalDateTime.of(2026, 5, 17, 13, 0)
                                                ),
                                                new NotificationHistoryItemResponse(
                                                        102L,
                                                        "스트릭 알림",
                                                        "1명이 더 인증하지 않으면 우리 그룹 스트릭이 깨져요!",
                                                        false,
                                                        "FEED",
                                                        10L,
                                                        "NONE",
                                                        null,
                                                        LocalDateTime.of(2026, 5, 17, 23, 30)
                                                )
                                        )
                                )
                        )
                ));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        FieldDescriptor[] responseFieldDescriptors = notificationHistoryListResponseFields();

        mockMvc.perform(get("/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("notifications/list-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(builder()
                                .tag("Notification")
                                .summary("알림 목록 조회")
                                .description("로그인 사용자의 활성 알림 히스토리를 날짜 라벨별로 그룹핑해서 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .responseSchema(schema("NotificationHistoryListResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    @Test
    void 알림_클릭_이동_정보를_조회한다() throws Exception {
        given(notificationNavigationService.resolve(eq(1L), eq(100L)))
                .willReturn(NotificationNavigationResponse.navigable("FEED_DETAIL", 200L));

        HeaderDescriptor[] requestHeaderDescriptors = authorizationHeaderDescriptors();
        ParameterDescriptor[] pathParameterDescriptors = notificationNavigationPathParameters();
        FieldDescriptor[] responseFieldDescriptors = notificationNavigationResponseFields();

        mockMvc.perform(get("/notifications/{notificationHistoryId}/navigation", 100L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andDo(document("notifications/navigation-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(requestHeaderDescriptors),
                        pathParameters(pathParameterDescriptors),
                        responseFields(responseFieldDescriptors),
                        resource(builder()
                                .tag("Notification")
                                .summary("알림 이동 정보 조회")
                                .description("알림 목록에서 특정 알림을 클릭했을 때 현재 상태 기준으로 이동 가능한 화면과 target 정보를 조회한다.")
                                .requestHeaders(requestHeaderDescriptors)
                                .pathParameters(pathParameterDescriptors)
                                .responseSchema(schema("NotificationNavigationResponse"))
                                .responseFields(responseFieldDescriptors)
                                .build()
                        )));
    }

    private HeaderDescriptor[] authorizationHeaderDescriptors() {
        return new HeaderDescriptor[] {
                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {accessToken} 형식의 서비스 access token")
        };
    }

    private FieldDescriptor[] registerTokenRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("token").type(STRING).description("Expo Push Token"),
                fieldWithPath("platform").type(STRING).description("디바이스 플랫폼 (IOS | ANDROID)")
        };
    }

    private FieldDescriptor[] removeTokenRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("token").type(STRING).description("삭제할 Expo Push Token")
        };
    }

    private FieldDescriptor[] updatePushNotificationSettingRequestFields() {
        return new FieldDescriptor[] {
                fieldWithPath("pushNotificationEnabled").type(BOOLEAN).description("푸시 알림 수신 여부")
        };
    }

    private FieldDescriptor[] notificationHistoryListResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("unreadCount").type(NUMBER).description("읽지 않은 활성 알림 개수"),
                fieldWithPath("groups").type(ARRAY).description("날짜 라벨별 알림 그룹"),
                fieldWithPath("groups[].label").type(STRING).description("날짜 그룹 라벨 (오늘 | 어제 | N일 전 | MM월 DD일 | YY년 MM월 DD일)"),
                fieldWithPath("groups[].notifications").type(ARRAY).description("해당 날짜 그룹의 알림 목록"),
                fieldWithPath("groups[].notifications[].id").type(NUMBER).description("알림 히스토리 ID"),
                fieldWithPath("groups[].notifications[].title").type(STRING).description("알림 제목"),
                fieldWithPath("groups[].notifications[].message").type(STRING).description("치환이 완료된 알림 본문"),
                fieldWithPath("groups[].notifications[].read").type(BOOLEAN).description("읽음 여부"),
                fieldWithPath("groups[].notifications[].targetType").type(STRING).description("알림 이동 대상 타입 (NONE | GROUP | FEED | FEED_DETAIL | GROUP_CHALLENGE)"),
                fieldWithPath("groups[].notifications[].targetId").type(VARIES).optional().description("알림 이동 대상 ID. FEED이면 groupChallengeId, FEED_DETAIL이면 challengeRecordId, GROUP이면 groupId, NONE이면 null"),
                fieldWithPath("groups[].notifications[].sourceType").type(STRING).description("알림 발생 원인 타입 (NONE | COMMENT | REACTION | POKE | CHALLENGE_RECORD)"),
                fieldWithPath("groups[].notifications[].sourceId").type(VARIES).optional().description("알림 발생 원인 ID. sourceType이 NONE이면 null"),
                fieldWithPath("groups[].notifications[].createdAt").type(STRING).description("알림 생성 시각")
        };
    }

    private ParameterDescriptor[] notificationNavigationPathParameters() {
        return new ParameterDescriptor[] {
                parameterWithName("notificationHistoryId").description("알림 히스토리 ID")
        };
    }

    private FieldDescriptor[] notificationNavigationResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("navigable").type(BOOLEAN).description("현재 상태 기준 직접 이동 가능 여부"),
                fieldWithPath("targetType").type(VARIES).optional().description("이동 가능할 때 대상 타입"),
                fieldWithPath("targetId").type(VARIES).optional().description("이동 가능할 때 대상 ID"),
                fieldWithPath("fallbackTargetType").type(VARIES).optional().description("직접 이동이 불가능할 때 대체 이동 대상 타입"),
                fieldWithPath("fallbackTargetId").type(VARIES).optional().description("직접 이동이 불가능할 때 대체 이동 대상 ID"),
                fieldWithPath("reason").type(VARIES).optional().description("직접 이동이 불가능한 사유 코드")
        };
    }
}

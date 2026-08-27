package com.detoxmate.notification.controller;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.notification.domain.DevicePlatform;
import com.detoxmate.notification.domain.FcmToken;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import com.detoxmate.notification.util.FcmSender;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class AppUnlockNotificationControllerTest {

    private static final String APP_UNLOCK_REQUEST_URL = "/notifications/app-unlock-requests";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    UserRepository userRepository;

    @Autowired
    FcmTokenRepository fcmTokenRepository;

    @Autowired
    NotificationHistoryRepository notificationHistoryRepository;

    @MockitoBean
    FcmSender fcmSender;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.createNew("잠금사용자"));
        userId = user.getId();
        fcmTokenRepository.save(FcmToken.create(userId, "app-unlock-ios-token", DevicePlatform.IOS));
    }

    @Test
    @DisplayName("앱 잠금 해제 요청은 이동 정보가 담긴 푸시만 전송하고 알림 이력은 저장하지 않는다")
    void requestAppUnlock_sendsPushOnlyWithNavigationPayload() throws Exception {
        mockMvc.perform(post(APP_UNLOCK_REQUEST_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isNoContent());

        verify(fcmSender).send(
                eq("app-unlock-ios-token"),
                eq("앱 잠금 해제"),
                eq("앱을 사용하려면, 이 알림을 클릭해주세요!"),
                eq(Map.of(
                        "type", "APP_UNLOCK_REQUESTED",
                        "targetType", "APP_UNLOCK_DURATION_SETTING"
                ))
        );
        assertThat(notificationHistoryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("인증하지 않은 앱 잠금 해제 요청은 거부하고 푸시를 전송하지 않는다")
    void requestAppUnlock_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(APP_UNLOCK_REQUEST_URL))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(fcmSender);
    }

    private String bearer(Long targetUserId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(targetUserId);
    }
}

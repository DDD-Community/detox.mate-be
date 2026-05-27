package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.Notification;
import com.detoxmate.notification.domain.NotificationHistory;
import com.detoxmate.notification.domain.NotificationPayload;
import com.detoxmate.notification.domain.NotificationType;
import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.dto.NotificationHistoryItemResponse;
import com.detoxmate.notification.dto.NotificationHistoryListResponse;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import com.detoxmate.user.dto.UserProfileSummary;
import com.detoxmate.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private NotificationHistoryRepository historyRepository;

    @Mock
    private UserService userService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"), KST);

    private NotificationHistoryService notificationHistoryService;

    @BeforeEach
    void setUp() {
        notificationHistoryService = new NotificationHistoryService(historyRepository, userService, clock);
    }

    @Test
    @DisplayName("알림 목록 조회 시 발신자 ID와 프로필 이미지 URL을 함께 반환한다")
    void getMyNotifications_includesSenderProfileImageUrl() {
        // given
        Long recipientUserId = 1L;
        Long senderUserId = 2L;
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        Notification notification = Notification.create(
                NotificationType.create(NotificationTypeCode.COMMENT_CREATED),
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다."
        );
        NotificationHistory history = NotificationHistory.fromResolvedMessage(
                notification,
                recipientUserId,
                senderUserId,
                "슬빈님이 댓글을 남겼습니다.",
                NotificationPayload.commentFeedDetail(10L, 20L),
                null
        );

        when(historyRepository.findActiveByUserId(recipientUserId, now)).thenReturn(List.of(history));
        when(historyRepository.countUnreadActiveByUserId(recipientUserId, now)).thenReturn(1L);
        when(userService.getProfileSummariesByIds(Set.of(senderUserId)))
                .thenReturn(Map.of(
                        senderUserId,
                        new UserProfileSummary(senderUserId, "슬빈", "https://cdn.detoxmate.co.kr/profile/2.png", false)
                ));

        // when
        NotificationHistoryListResponse response = notificationHistoryService.getMyNotifications(recipientUserId);

        // then
        NotificationHistoryItemResponse item = response.groups().getFirst().notifications().getFirst();
        assertThat(item.senderUserId()).isEqualTo(senderUserId);
        assertThat(item.senderProfileImageUrl()).isEqualTo("https://cdn.detoxmate.co.kr/profile/2.png");
    }
}

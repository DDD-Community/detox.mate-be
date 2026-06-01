package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.NotificationHistory;
import com.detoxmate.notification.dto.NotificationHistoryItemResponse;
import com.detoxmate.notification.dto.NotificationHistoryListResponse;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NotificationHistoryRepository historyRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public NotificationHistoryListResponse getMyNotifications(Long userId){
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));

        List<NotificationHistory> histories = historyRepository.findActiveByUserId(userId, now);
        long unreadCount = historyRepository.countUnreadActiveByUserId(userId, now);

        List<NotificationHistoryItemResponse> notifications = histories.stream()
                .map(this::toItemResponse)
                .toList();

        return new NotificationHistoryListResponse(unreadCount, notifications);
    }

    private NotificationHistoryItemResponse toItemResponse(NotificationHistory history) {
        return new NotificationHistoryItemResponse(
                history.getId(),
                history.getTitle(),
                history.getMessage(),
                history.isRead(),
                history.getTargetType().name(),
                history.getTargetId(),
                history.getSourceType().name(),
                history.getSourceId(),
                history.getCreatedAt().atZone(KST).toOffsetDateTime()
        );
    }
}

package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.NotificationHistory;
import com.detoxmate.notification.dto.NotificationHistoryItemResponse;
import com.detoxmate.notification.dto.NotificationHistoryListResponse;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import com.detoxmate.user.dto.UserProfileSummary;
import com.detoxmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NotificationHistoryRepository historyRepository;
    private final UserService userService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public NotificationHistoryListResponse getMyNotifications(Long userId){
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));

        List<NotificationHistory> histories = historyRepository.findActiveByUserId(userId, now);
        long unreadCount = historyRepository.countUnreadActiveByUserId(userId, now);
        Map<Long, UserProfileSummary> senderProfiles = findSenderProfiles(histories);

        List<NotificationHistoryItemResponse> notifications = histories.stream()
                .map(history -> toItemResponse(history, senderProfiles))
                .toList();

        return new NotificationHistoryListResponse(unreadCount, notifications);
    }

    private Map<Long, UserProfileSummary> findSenderProfiles(List<NotificationHistory> histories) {
        Set<Long> senderUserIds = histories.stream()
                .map(NotificationHistory::getSenderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return userService.getProfileSummariesByIds(senderUserIds);
    }

    private NotificationHistoryItemResponse toItemResponse(
            NotificationHistory history,
            Map<Long, UserProfileSummary> senderProfiles
    ) {
        UserProfileSummary senderProfile = senderProfiles.get(history.getSenderUserId());

        return new NotificationHistoryItemResponse(
                history.getId(),
                history.getTitle(),
                history.getMessage(),
                history.getSenderUserId(),
                senderProfile == null ? null : senderProfile.profileImageUrl(),
                history.isRead(),
                history.getTargetType().name(),
                history.getTargetId(),
                history.getSourceType().name(),
                history.getSourceId(),
                history.getCreatedAt()
                        .atZone(KST)
                        .toOffsetDateTime()
        );
    }

}

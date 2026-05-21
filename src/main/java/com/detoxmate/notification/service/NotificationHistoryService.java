package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.NotificationHistory;
import com.detoxmate.notification.dto.NotificationHistoryGroupResponse;
import com.detoxmate.notification.dto.NotificationHistoryItemResponse;
import com.detoxmate.notification.dto.NotificationHistoryListResponse;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, List<NotificationHistoryItemResponse>> grouped = histories.stream()
                .collect(
                        java.util.stream.Collectors.groupingBy(
                                history -> groupLabel(history.getCreatedAt(), now.toLocalDate()),
                                LinkedHashMap::new,
                                java.util.stream.Collectors.mapping(this::toItemResponse, java.util.stream.Collectors.toList())
                        )
                );

        List<NotificationHistoryGroupResponse> groups = grouped.entrySet().stream()
                .map(entry -> new NotificationHistoryGroupResponse(entry.getKey(), entry.getValue()))
                .toList();

        return new NotificationHistoryListResponse(unreadCount, groups);
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
                history.getCreatedAt()
        );
    }

    private String groupLabel(LocalDateTime createdAt, LocalDate today) {
        LocalDate createdDate = createdAt.toLocalDate();
        long days = java.time.temporal.ChronoUnit.DAYS.between(createdDate, today);

        if (days == 0) {
            return "오늘";
        }

        if (days == 1) {
            return "어제";
        }

        if (days <= 7) {
            return days + "일 전";
        }

        if (createdDate.getYear() == today.getYear()) {
            return String.format("%02d월 %02d일", createdDate.getMonthValue(), createdDate.getDayOfMonth());
        }

        return String.format(
                "%02d년 %02d월 %02d일",
                createdDate.getYear() % 100,
                createdDate.getMonthValue(),
                createdDate.getDayOfMonth()
        );
    }

}

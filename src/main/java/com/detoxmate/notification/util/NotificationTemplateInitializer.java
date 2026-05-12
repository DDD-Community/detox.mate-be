package com.detoxmate.notification.util;

import com.detoxmate.notification.domain.Notification;
import com.detoxmate.notification.domain.NotificationType;
import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.repository.NotificationRepository;
import com.detoxmate.notification.repository.NotificationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class NotificationTemplateInitializer implements ApplicationRunner {

    private final NotificationTypeRepository typeRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args){
        seed(NotificationTypeCode.GROUP_JOINED,
                "그룹 알림",
                "{nickname}님이 {groupName}에 합류했습니다. 확인해보세요!");

        seed(NotificationTypeCode.CERTIFICATION_CREATED,
                "인증 알림",
                "{nickname}님이 인증을 완료했습니다. 반응을 남겨보세요!");

        seed(NotificationTypeCode.POKE_RECEIVED,
                "콕 찌르기 알림",
                "{nickname}님이 {me}님을 콕 찔렀어요. 인증하러 가볼까요?");

        seed(NotificationTypeCode.REACTION_CREATED,
                "반응 알림",
                "{nickname}님이 반응을 남겼습니다. {me}님도 반응을 보내볼까요?");

        seed(NotificationTypeCode.COMMENT_CREATED,
                "댓글 알림",
                "{nickname}님이 \"{commentBody}\"이라고 반응했습니다.");

        seed(NotificationTypeCode.STREAK_WARNING,
                "스트릭 알림",
                "{remainingCount}명이 더 인증하지 않으면 우리 그룹 스트릭이 깨져요!");

        seed(NotificationTypeCode.DAILY_CERTIFICATION_REMINDER,
                "인증 마감 알림",
                "오늘 아직 인증을 안했어요. 인증하러 가볼까요?");

        seed(NotificationTypeCode.WEEKLY_GOAL_SUMMARY,
                "주간 목표 알림",
                "이번주는 {achievementCount}번 목표 달성을 했네요! 다음주도 파이팅!");
    }

    private void seed(NotificationTypeCode typeCode, String title, String messageTemplate){
        NotificationType type = typeRepository.findByTypeCode(typeCode)
                .orElseGet(() -> typeRepository.save(NotificationType.create(typeCode)));

        if (notificationRepository.findByTypeCode(typeCode).isEmpty()) {
            notificationRepository.save(Notification.create(type, title, messageTemplate));
        }
    }
}

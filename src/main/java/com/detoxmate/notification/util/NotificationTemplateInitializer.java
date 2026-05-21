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
        seed(
                NotificationTypeCode.GROUP_JOINED,
                "그룹 알림",
                "{nickname}님이 {groupName}에 합류했습니다. 확인해보세요!"
        );

        seed(
                NotificationTypeCode.CERTIFICATION_START_TOMORROW,
                "인증 시작 알림",
                "내일부터 {groupName} 인증이 시작됩니다. 오늘부터 디톡스 시작!"
        );

        seed(
                NotificationTypeCode.GOAL_SETTING_REMINDER,
                "목표 설정 알림",
                "{nickname}님의 목표 설정을 멤버들이 기다리고 있어요. 목표 설정하러 가볼까요?"
        );

        seed(
                NotificationTypeCode.POKE_RECEIVED,
                "콕 찌르기 알림",
                "{nickname}님이 {me}님을 콕 찔렀어요. 인증하러 가볼까요?"
        );

        seed(
                NotificationTypeCode.POKE_GOAL_SETTING_REMINDER,
                "목표 설정 재촉 알림",
                "{nickname}님이 {me}님을 콕 찔렀어요. 목표 설정을 해볼까요?"
        );

        seed(
                NotificationTypeCode.CERTIFICATION_CREATED,
                "인증 업로드 알림",
                "{nickname}님이 인증을 업로드했습니다. 반응을 남겨보세요!"
        );

        seed(
                NotificationTypeCode.REACTION_CREATED,
                "반응 알림",
                "{nickname}님이 반응을 남겼습니다. {me}님도 반응을 보내볼까요?"
        );

        seed(
                NotificationTypeCode.COMMENT_CREATED,
                "댓글 알림",
                "{nickname}님이 댓글을 남겼습니다: \"{commentBody}\""
        );

        seed(
                NotificationTypeCode.DAILY_CERTIFICATION_REMINDER,
                "인증 마감 알림",
                "아직 오늘의 인증을 안 했어요. 인증하러 가볼까요?"
        );

        seed(
                NotificationTypeCode.STREAK_WARNING,
                "스트릭 알림",
                "{remainingCount}명이 더 인증하지 않으면 우리 그룹 스트릭이 깨져요!"
        );

        seed(
                NotificationTypeCode.WEEKLY_GOAL_SUMMARY,
                "주간 목표 알림",
                "이번 주는 {achievementCount}번 목표 달성을 했네요! 다음 주도 파이팅!"
        );
    }

    private void seed(NotificationTypeCode typeCode, String title, String messageTemplate) {
        NotificationType type = typeRepository.findByTypeCode(typeCode)
                .orElseGet(() -> typeRepository.save(NotificationType.create(typeCode)));

        notificationRepository.findByTypeCode(typeCode)
                .ifPresentOrElse(
                        notification -> notification.updateTemplate(title, messageTemplate),
                        () -> notificationRepository.save(Notification.create(type, title, messageTemplate))
                );
    }
}

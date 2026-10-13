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
                "{nickname}님이 {groupName}에 합류했어요. 확인해보세요!"
        );

        seed(
                NotificationTypeCode.CERTIFICATION_START_TOMORROW,
                "내일부터 {groupName} 인증이 시작돼요. 오늘부터 디톡스를 시작해요!"
        );

        seed(
                NotificationTypeCode.GOAL_SETTING_REMINDER,
                "{nickname}님의 목표 설정을 멤버들이 기다리고 있어요. 목표 설정하러 가볼까요?"
        );

        seed(
                NotificationTypeCode.POKE_RECEIVED,
                "{nickname}님이 {me}님을 콕 찔렀어요. 인증하러 가볼까요?"
        );

        seed(
                NotificationTypeCode.POKE_GOAL_SETTING_REMINDER,
                "{nickname}님이 {me}님을 콕 찔렀어요. 목표 설정을 해볼까요?"
        );

        seed(
                NotificationTypeCode.CERTIFICATION_CREATED,
                "{nickname}님이 인증을 업로드했어요. 반응을 남겨보세요!"
        );

        seed(
                NotificationTypeCode.REACTION_CREATED,
                "{nickname}님이 반응을 남겼어요. {me}님도 반응을 보내볼까요?"
        );

        seed(
                NotificationTypeCode.COMMENT_CREATED,
                "{nickname}님이 댓글을 남겼어요: \"{commentBody}\""
        );

        seed(
                NotificationTypeCode.DAILY_CERTIFICATION_REMINDER,
                "아직 오늘의 인증을 안 했어요. 인증하러 가볼까요?"
        );

        seed(
                NotificationTypeCode.STREAK_WARNING,
                "{remainingCount}명이 더 인증하지 않으면 우리 그룹 스트릭이 깨져요!"
        );

        seed(
                NotificationTypeCode.WEEKLY_GOAL_SUMMARY,
                "이번 주는 {achievementCount}번 목표 달성을 했네요! 다음 주도 파이팅!"
        );

        seed(
                NotificationTypeCode.APP_UNLOCK_REQUESTED,
                "앱을 사용하려면, 이 알림을 클릭해주세요!"
        );

        seed(NotificationTypeCode.FRIEND_UNLOCKED,
                "{friendName}님이 잠근 앱을 등록 해제했어요.");
        seed(NotificationTypeCode.FRIEND_REQUEST_RECEIVED,
                "{friendName}님이 친구 요청을 보냈어요.");
        seed(NotificationTypeCode.FRIEND_REQUEST_ACCEPTED,
                "{friendName}님과 친구가 되었어요! 함께 스크린타임을 줄여봐요.");
        seed(NotificationTypeCode.FRIEND_APP_UNLOCKED,
                "{friendName}님이 잠긴 앱을 {minutes}분 동안 일시 해제했어요.");
        seed(NotificationTypeCode.FRIEND_TIME_LIMIT_CHANGED,
                "{friendName}님이 목표 제한 시간을 {duration}으로 변경했어요.");
        seed(NotificationTypeCode.APP_RELOCK_REMINDER,
                "앱 사용 시간이 얼마 남지 않았어요!");
    }

    private void seed(NotificationTypeCode typeCode, String messageTemplate) {
        NotificationType type = typeRepository.findByTypeCode(typeCode)
                .orElseGet(() -> typeRepository.save(NotificationType.create(typeCode)));

        notificationRepository.findByTypeCode(typeCode)
                .ifPresentOrElse(
                        notification -> notification.updateTemplate("Detoxmate", messageTemplate),
                        () -> notificationRepository.save(Notification.create(type, "Detoxmate", messageTemplate))
                );
    }
}

package com.detoxmate.notification.service;

import com.detoxmate.notification.domain.NotificationContext;
import com.detoxmate.notification.domain.NotificationPayload;
import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.dto.AppLockRemovalRecipientsResponse;
import com.detoxmate.notification.event.FriendUnlockEvent;
import com.detoxmate.notification.util.FriendUnlockRecipientSelector;
import com.detoxmate.notification.util.NotificationRecipientReader;
import com.detoxmate.notification.util.NotificationUserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppLockNotificationService {

    private final NotificationService notificationService;
    private final NotificationRecipientReader recipientReader;
    private final NotificationUserReader userReader;
    private final FriendUnlockRecipientSelector recipientSelector;
    private final ApplicationEventPublisher eventPublisher;

    public void reportUnlock(Long userId, int unlockMinutes, boolean limitExceeded) {
        if (!limitExceeded) {
            return;
        }
        List<Long> recipients = recipientSelector.select(recipientReader.findFriendWhenUnlock(userId));
        if (recipients.isEmpty()) {
            return;
        }
        String name = userReader.findDisplayName(userId);
        for (Long recipient : recipients) {
            notificationService.send(NotificationCommand.pushOnly(
                    recipient, userId, NotificationTypeCode.FRIEND_APP_UNLOCKED,
                    NotificationContext.of("friendName", name, "minutes", String.valueOf(unlockMinutes)),
                    NotificationPayload.none()
            ));
        }
    }

    @Transactional(readOnly = true)
    public AppLockRemovalRecipientsResponse previewRemovalRecipients(Long userId) {
        List<Long> recipients = recipientSelector.select(recipientReader.findFriendWhenUnlock(userId));
        Map<Long, String> names = userReader.findDisplayNames(new HashSet<>(recipients));
        return new AppLockRemovalRecipientsResponse(recipients.stream()
                .map(id -> new AppLockRemovalRecipientsResponse.Recipient(id, names.get(id)))
                .toList());
    }

    @Transactional(readOnly = true)
    public void confirmRemoval(Long userId, List<Long> recipientUserIds) {
        if (recipientUserIds.size() != new HashSet<>(recipientUserIds).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수신자는 중복될 수 없습니다.");
        }
        List<Long> friends = recipientReader.findFriendWhenUnlock(userId);
        if (!friends.containsAll(recipientUserIds)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "현재 활성 친구에게만 알림을 보낼 수 있습니다.");
        }
        eventPublisher.publishEvent(new FriendUnlockEvent(userId, recipientUserIds));
    }

    public void remindRelock(Long userId) {
        notificationService.send(NotificationCommand.pushOnly(
                userId, NotificationTypeCode.APP_RELOCK_REMINDER,
                NotificationContext.empty(), NotificationPayload.none()
        ));
    }
}

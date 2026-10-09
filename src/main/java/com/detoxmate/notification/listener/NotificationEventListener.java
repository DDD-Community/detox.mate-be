package com.detoxmate.notification.listener;

import com.detoxmate.notification.domain.NotificationContext;
import com.detoxmate.notification.domain.NotificationPayload;
import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.dto.ChallengeRecordNotificationRow;
import com.detoxmate.notification.event.*;
import com.detoxmate.notification.service.NotificationCommand;
import com.detoxmate.notification.util.*;
import com.detoxmate.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class NotificationEventListener {

    private static final int COMMENT_PREVIEW_MAX_LENGTH = 60;

    private final NotificationService notificationService;
    private final NotificationRecipientReader recipientReader;
    private final NotificationUserReader userReader;
    private final NotificationCommentReader commentReader;
    private final NotificationGroupReader groupReader;
    private final FriendUnlockRecipientSelector friendUnlockRecipientSelector;


    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(GroupJoinedEvent event) {
        String joinedUserName = userReader.findDisplayName(event.joinedUserId());
        String groupName = groupReader.findGroupName(event.groupId());

        List<Long> recipientUserIds = recipientReader.findActiveGroupMemberUserIds(event.groupId())
                .stream()
                .filter(recipientUserId -> !recipientUserId.equals(event.joinedUserId()))
                .toList();

        for (Long recipientUserId : recipientUserIds) {
            notificationService.send(NotificationCommand.history(
                    recipientUserId,
                    event.joinedUserId(),
                    NotificationTypeCode.GROUP_JOINED,
                    NotificationContext.of(
                            "nickname", joinedUserName,
                            "groupName", groupName
                    ),
                    NotificationPayload.feed(event.groupChallengeId())
            ));
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CertificationCreatedEvent event) {
        ChallengeRecordNotificationRow info = recipientReader.findChallengeRecordInfo(event.challengeRecordId());

        String actorName = userReader.findDisplayName(event.actorUserId());

        List<Long> recipientUserIds = recipientReader.findGroupChallengeParticipantUserIds(info.groupChallengeId())
                .stream()
                .filter(recipientUserId -> !recipientUserId.equals(event.actorUserId()))
                .toList();

        for (Long recipientUserId : recipientUserIds) {
            notificationService.send(NotificationCommand.history(
                    recipientUserId,
                    event.actorUserId(),
                    NotificationTypeCode.CERTIFICATION_CREATED,
                    NotificationContext.of("nickname", actorName),
                    NotificationPayload.feedDetail(event.challengeRecordId())
            ));
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PokeCreatedEvent event) {
        ChallengeRecordNotificationRow info = recipientReader.findChallengeRecordInfo(event.challengeRecordId());

        Map<Long, String> names = userReader.findDisplayNames(
                Set.of(event.senderUserId(), event.receiverUserId())
        );

        String senderName = names.get(event.senderUserId());
        String receiverName = names.get(event.receiverUserId());

        notificationService.send(NotificationCommand.history(
                event.receiverUserId(),
                event.senderUserId(),
                NotificationTypeCode.POKE_RECEIVED,
                NotificationContext.of(
                        "nickname", senderName,
                        "me", receiverName
                ),
                NotificationPayload.feed(info.groupChallengeId())
        ));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ReactionCreatedEvent event) {
        ChallengeRecordNotificationRow info = recipientReader.findChallengeRecordInfo(event.challengeRecordId());

        if (info.authorUserId().equals(event.reactorUserId())) {
            return;
        }

        String reactorName = userReader.findDisplayName(event.reactorUserId());

        notificationService.send(NotificationCommand.history(
                info.authorUserId(),
                event.reactorUserId(),
                NotificationTypeCode.REACTION_CREATED,
                NotificationContext.of(
                        "nickname", reactorName,
                        "me", info.authorNickname()
                ),
                NotificationPayload.feedDetail(event.challengeRecordId())
        ));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CommentCreatedEvent event) {
        ChallengeRecordNotificationRow info = recipientReader.findChallengeRecordInfo(event.challengeRecordId());

        if (info.authorUserId().equals(event.commenterUserId())) {
            return;
        }

        String commenterName = userReader.findDisplayName(event.commenterUserId());
        String commentBody = commentReader.findCommentBody(event.commentId());

        notificationService.send(NotificationCommand.history(
                info.authorUserId(),
                event.commenterUserId(),
                NotificationTypeCode.COMMENT_CREATED,
                NotificationContext.of(
                        "nickname", commenterName,
                        "commentBody", truncateComment(commentBody)
                ),
                NotificationPayload.commentFeedDetail(event.challengeRecordId(), event.commentId())
        ));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CertificationStartTomorrowEvent event) {
        String groupName = groupReader.findGroupName(event.groupId());

        List<Long> recipientUserIds = recipientReader.findActiveGroupMemberUserIds(event.groupId());
        for (Long recipientUserId : recipientUserIds) {
            notificationService.send(NotificationCommand.history(
                    recipientUserId,
                    NotificationTypeCode.CERTIFICATION_START_TOMORROW,
                    NotificationContext.of("groupName", groupName),
                    NotificationPayload.feed(event.groupChallengeId())
            ));
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(GoalSettingReminderEvent event) {
        String targetUserName = userReader.findDisplayName(event.targetUserId());

        notificationService.send(NotificationCommand.history(
                event.targetUserId(),
                NotificationTypeCode.GOAL_SETTING_REMINDER,
                NotificationContext.of("nickname",targetUserName),
                NotificationPayload.feed(event.groupChallengeId())
        ));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PokeGoalSettingReminderEvent event) {
        ChallengeRecordNotificationRow info = recipientReader.findChallengeRecordInfo(event.challengeRecordId());

        Map<Long, String> names = userReader.findDisplayNames(
                Set.of(event.senderUserId(), event.receiverUserId())
        );

        String senderName = names.get(event.senderUserId());
        String receiverName = names.get(event.receiverUserId());

        notificationService.send(NotificationCommand.history(
                event.receiverUserId(),
                event.senderUserId(),
                NotificationTypeCode.POKE_GOAL_SETTING_REMINDER,
                NotificationContext.of(
                        "nickname",senderName,
                        "me",receiverName
                ),
                NotificationPayload.feed(info.groupChallengeId())
        ));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(FriendUnlockEvent event) {
        Long unlockUserId = event.unlockUserId();
        List<Long> recipientUserIds = event.recipientUserIds();
        if(recipientUserIds.isEmpty()){
            return;
        }
        String unlockUserName = userReader.findDisplayName(unlockUserId);

        for(Long recipientUserId : recipientUserIds){
            notificationService.send(NotificationCommand.pushOnly(
                    recipientUserId,
                    event.unlockUserId(),
                    NotificationTypeCode.FRIEND_UNLOCKED,
                    NotificationContext.of("friendName", unlockUserName),
                    NotificationPayload.none()
            ));
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(FriendRequestSentEvent event) {
        notificationService.send(NotificationCommand.history(
                event.receiverUserId(), event.senderUserId(), NotificationTypeCode.FRIEND_REQUEST_RECEIVED,
                NotificationContext.of("friendName", userReader.findDisplayName(event.senderUserId())),
                NotificationPayload.friendRequests()
        ));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(FriendRequestAcceptedEvent event) {
        notificationService.send(NotificationCommand.history(
                event.requesterUserId(), event.acceptingUserId(), NotificationTypeCode.FRIEND_REQUEST_ACCEPTED,
                NotificationContext.of("friendName", userReader.findDisplayName(event.acceptingUserId())),
                NotificationPayload.friends()
        ));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TimeLimitChangedEvent event) {
        List<Long> recipients = friendUnlockRecipientSelector.select(recipientReader.findFriendWhenUnlock(event.userId()));
        if (recipients.isEmpty()) {
            return;
        }
        String name = userReader.findDisplayName(event.userId());
        String duration = formatMinutes(event.totalLockMinutes());
        for (Long recipient : recipients) {
            notificationService.send(NotificationCommand.pushOnly(
                    recipient, event.userId(), NotificationTypeCode.FRIEND_TIME_LIMIT_CHANGED,
                    NotificationContext.of("friendName", name, "duration", duration), NotificationPayload.none()
            ));
        }
    }

    private String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (minutes == 0) {
            return hours + "시간";
        }
        return hours == 0 ? minutes + "분" : hours + "시간 " + minutes + "분";
    }

    private String truncateComment(String commentBody) {
        if (commentBody == null) {
            return "";
        }

        if (commentBody.length() <= COMMENT_PREVIEW_MAX_LENGTH) {
            return commentBody;
        }

        return commentBody.substring(0, COMMENT_PREVIEW_MAX_LENGTH) + "...";
    }

}

package com.detoxmate.notification.service;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.user.domain.User;
import org.springframework.web.util.UriComponentsBuilder;
import com.detoxmate.comment.domain.Comment;
import com.detoxmate.comment.domain.CommentStatus;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.notification.NotificationErrorCode;
import com.detoxmate.notification.domain.NotificationHistory;
import com.detoxmate.notification.domain.NotificationSourceType;
import com.detoxmate.notification.domain.NotificationTargetType;
import com.detoxmate.notification.dto.NotificationNavigationResponse;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import com.detoxmate.notification.util.NotificationNavigationReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationNavigationService {

    private final NotificationHistoryRepository historyRepository;
    private final NotificationNavigationReader navigationReader;

    @Transactional(readOnly = true)
    public NotificationNavigationResponse resolve(Long userId, Long notificationHistoryId) {
        NotificationHistory history = historyRepository.findByIdAndUserId(notificationHistoryId, userId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_HISTORY_NOT_FOUND));

        if (NotificationTypeCode.FRIEND_HISTORY_TYPES.contains(history.getNotification().getType().getTypeCode())) {
            return resolveFriend(userId, history);
        }

        if (history.getTargetType() == NotificationTargetType.NONE) {
            return NotificationNavigationResponse.noNavigatable("NO_TARGET");
        }

        if (history.getTargetType() == NotificationTargetType.MY_PAGE
                || history.getTargetType() == NotificationTargetType.FRIEND_REQUESTS
                || history.getTargetType() == NotificationTargetType.FRIENDS
                || history.getTargetType() == NotificationTargetType.APP_UNLOCK_TIMER
                || history.getTargetType() == NotificationTargetType.APP_UNLOCK_DURATION_SETTING) {
            return NotificationNavigationResponse.navigable(history.getTargetType().name(), null);
        }

        if (history.getTargetId() == null) {
            return NotificationNavigationResponse.noNavigatable("NO_TARGET");
        }

        if (history.getTargetType() == NotificationTargetType.FEED) {
            return resolveFeed(userId, history.getTargetId());
        }

        if (history.getTargetType() == NotificationTargetType.FEED_DETAIL) {
            return resolveFeedDetail(userId, history);
        }

        return NotificationNavigationResponse.navigable(
                history.getTargetType().name(),
                history.getTargetId()
        );
    }

    private NotificationNavigationResponse resolveFriend(Long userId, NotificationHistory history) {
        if (history.getSenderUserId() == null) {
            return NotificationNavigationResponse.noNavigatable("TARGET_NOT_FOUND");
        }
        User sender = navigationReader.findActiveUser(history.getSenderUserId()).orElse(null);
        if (sender == null) {
            return NotificationNavigationResponse.noNavigatable("TARGET_NOT_FOUND");
        }

        Friend friendship = navigationReader.findFriendship(userId, sender.getId()).orElse(null);
        UriComponentsBuilder url = UriComponentsBuilder.fromPath("/friend-page")
                .queryParam("userId", sender.getId())
                .queryParam("displayName", "{displayName}");
        if (friendship == null) {
            url.queryParam("relationshipStatus", FriendRelationshipStatus.NONE);
        } else if (friendship.isAccepted()) {
            url.queryParam("relationshipStatus", FriendRelationshipStatus.FRIEND)
                    .queryParam("friendshipId", friendship.getId());
        } else {
            url.queryParam("relationshipStatus", friendship.isFrom(userId)
                            ? FriendRelationshipStatus.PENDING_SENT : FriendRelationshipStatus.PENDING_RECEIVED)
                    .queryParam("requestId", friendship.getId());
        }
        return NotificationNavigationResponse.friendPage(
                history.getTargetType().name(), history.getTargetId(),
                url.encode().buildAndExpand(sender.getPublicDisplayName()).toUriString()
        );
    }

    private NotificationNavigationResponse resolveFeed(Long userId, Long groupChallengeId) {
        if (!navigationReader.canAccessFeed(userId, groupChallengeId)) {
            return NotificationNavigationResponse.noNavigatable("NO_ACCESS");
        }

        return NotificationNavigationResponse.navigable(
                NotificationTargetType.FEED.name(),
                groupChallengeId
        );
    }

    private NotificationNavigationResponse resolveFeedDetail(Long userId, NotificationHistory history) {
        ChallengeRecord challengeRecord = navigationReader.findChallengeRecord(history.getTargetId())
                .orElse(null);

        if (challengeRecord == null) {
            return NotificationNavigationResponse.noNavigatable("TARGET_NOT_FOUND");
        }

        if (!navigationReader.canAccessFeed(userId, challengeRecord.getGroupChallengeId())) {
            return NotificationNavigationResponse.noNavigatable("NO_ACCESS");
        }

        if (history.getSourceType() == NotificationSourceType.COMMENT) {
            return resolveCommentNavigation(history, challengeRecord);
        }

        return NotificationNavigationResponse.navigable(
                NotificationTargetType.FEED_DETAIL.name(),
                challengeRecord.getId()
        );
    }

    private NotificationNavigationResponse resolveCommentNavigation(
            NotificationHistory history,
            ChallengeRecord challengeRecord
    ) {
        Comment comment = navigationReader.findComment(history.getSourceId())
                .orElse(null);

        if (comment == null) {
            return NotificationNavigationResponse.fallback(
                    NotificationTargetType.FEED.name(),
                    challengeRecord.getGroupChallengeId(),
                    "COMMENT_NOT_FOUND"
            );
        }

        if (comment.getCommentStatus() == CommentStatus.BEFORE_RECORD && challengeRecord.isCertified()) {
            return NotificationNavigationResponse.fallback(
                    NotificationTargetType.FEED.name(),
                    challengeRecord.getGroupChallengeId(),
                    "BEFORE_COMMENT_CLOSED_AFTER_CERTIFICATION"
            );
        }

        return NotificationNavigationResponse.navigable(
                NotificationTargetType.FEED_DETAIL.name(),
                challengeRecord.getId()
        );
    }
}

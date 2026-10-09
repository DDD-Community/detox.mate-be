package com.detoxmate.notification.event;

public record FriendRequestAcceptedEvent(Long acceptingUserId, Long requesterUserId) {
}

package com.detoxmate.notification.event;

public record FriendRequestSentEvent(Long senderUserId, Long receiverUserId) {
}

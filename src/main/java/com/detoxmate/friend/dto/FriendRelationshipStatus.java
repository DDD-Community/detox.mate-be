package com.detoxmate.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum FriendRelationshipStatus {
    NONE,
    SELF,
    PENDING_SENT,
    PENDING_RECEIVED,
    FRIEND
}

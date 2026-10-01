package com.detoxmate.friend.mapper;

import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.friend.dto.FriendRequestResponse;
import com.detoxmate.friend.dto.FriendResponse;
import com.detoxmate.friend.dto.FriendUserResponse;
import com.detoxmate.friend.dto.FriendListUserResponse;
import com.detoxmate.friend.dto.FriendReceivedRequestResponse;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendResponseMapper {

    private final ImageReadUrlBuilder imageReadUrlBuilder;

    public FriendUserResponse toUserResponse(
            User user,
            FriendRelationshipStatus relationshipStatus
    ) {
        return toUserResponse(user, relationshipStatus, null);
    }

    public FriendUserResponse toUserResponse(
            User user,
            FriendRelationshipStatus relationshipStatus,
            Long requestId
    ) {
        return new FriendUserResponse(
                user.getId(),
                user.getPublicDisplayName(),
                imageReadUrlBuilder.build(user.getPublicProfileImageObjectKey()),
                relationshipStatus,
                requestId
        );
    }

    public FriendRequestResponse toRequestResponse(
            Friend request,
            User otherUser,
            FriendRelationshipStatus relationshipStatus
    ) {
        return new FriendRequestResponse(
                request.getId(),
                toUserResponse(otherUser, relationshipStatus, request.getId()),
                request.getCreatedAt()
        );
    }

    public FriendResponse toFriendResponse(Friend friendship, User friendUser) {
        return new FriendResponse(
                friendship.getId(),
                toListUserResponse(friendUser, FriendRelationshipStatus.FRIEND, null),
                friendship.getAcceptedAt()
        );
    }

    public FriendReceivedRequestResponse toReceivedRequestResponse(Friend request, User sender) {
        return new FriendReceivedRequestResponse(
                request.getId(),
                toListUserResponse(sender, FriendRelationshipStatus.PENDING_RECEIVED, request.getId()),
                request.getCreatedAt()
        );
    }

    private FriendListUserResponse toListUserResponse(User user, FriendRelationshipStatus status, Long requestId) {
        FriendUserResponse base = toUserResponse(user, status, requestId);
        return new FriendListUserResponse(
                base.userId(), base.displayName(), base.profileImageUrl(), base.relationshipStatus(), base.requestId(),
                user.isActive() ? user.getEmail() : null
        );
    }
}

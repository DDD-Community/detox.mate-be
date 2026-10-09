package com.detoxmate.friend.service;

import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.dto.FriendProfileResponse;
import com.detoxmate.friend.dto.FriendProfileResponse.FriendPreview;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.friend.repository.FriendRepository;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendProfileService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final ImageReadUrlBuilder imageReadUrlBuilder;

    @Transactional(readOnly = true)
    public FriendProfileResponse getProfile(Long viewerUserId, Long friendUserId) {
        User friendUser = userRepository.findById(friendUserId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (viewerUserId.equals(friendUserId)
                || friendRepository.findByUserPair(viewerUserId, friendUserId).filter(Friend::isAccepted).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수락된 친구의 프로필만 조회할 수 있습니다.");
        }

        return new FriendProfileResponse(
                friendUser.getId(),
                friendUser.getPublicDisplayName(),
                imageReadUrlBuilder.build(friendUser.getPublicProfileImageObjectKey()),
                friendUser.getUserCode(),
                FriendRelationshipStatus.FRIEND,
                getFriendPreviews(friendUserId)
        );
    }

    private List<FriendPreview> getFriendPreviews(Long friendUserId) {
        List<Friend> friendships = friendRepository.findAcceptedFriendshipsByUserId(friendUserId);
        Set<Long> friendIds = friendships.stream()
                .map(friendship -> friendship.otherUserId(friendUserId))
                .collect(Collectors.toSet());
        Map<Long, User> activeUsersById = userRepository.findAllById(friendIds).stream()
                .filter(User::isActive)
                .collect(Collectors.toMap(User::getId, user -> user));

        return friendships.stream()
                .map(friendship -> activeUsersById.get(friendship.otherUserId(friendUserId)))
                .filter(Objects::nonNull)
                .map(user -> new FriendPreview(
                        user.getPublicDisplayName(),
                        imageReadUrlBuilder.build(user.getPublicProfileImageObjectKey())
                ))
                .toList();
    }
}

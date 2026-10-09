package com.detoxmate.friend.service;

import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.domain.FriendInvite;
import com.detoxmate.friend.dto.FriendInviteResponse;
import com.detoxmate.friend.dto.FriendRequestResponse;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.friend.dto.FriendResponse;
import com.detoxmate.friend.dto.FriendUserResponse;
import com.detoxmate.friend.dto.FriendInviteeResponse;
import com.detoxmate.friend.dto.FriendReceivedRequestResponse;
import com.detoxmate.friend.dto.FriendSearchResponse;
import com.detoxmate.friend.repository.MutualFriendSummary;
import com.detoxmate.friend.mapper.FriendResponseMapper;
import com.detoxmate.friend.repository.FriendInviteRepository;
import com.detoxmate.friend.repository.FriendRepository;
import com.detoxmate.notification.event.FriendRequestAcceptedEvent;
import com.detoxmate.notification.event.FriendRequestSentEvent;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.domain.UserCode;
import com.detoxmate.user.domain.UserStatus;
import com.detoxmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private static final int MAX_INVITE_CODE_GENERATION_ATTEMPTS = 10;

    private final FriendInviteRepository friendInviteRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final FriendInviteCodeGenerator friendInviteCodeGenerator;
    private final FriendResponseMapper friendResponseMapper;
    private final FriendInviteStatisticsService friendInviteStatisticsService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FriendInviteResponse getMyInvite(Long userId) {
        User currentUser = userRepository.findByIdForUpdate(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "탈퇴한 사용자입니다."));

        Optional<FriendInvite> existingInvite = friendInviteRepository.findByUserId(userId);
        if (existingInvite.isPresent()) {
            return new FriendInviteResponse(existingInvite.get().getCode(), currentUser.getEmail());
        }

        for (int attempt = 0; attempt < MAX_INVITE_CODE_GENERATION_ATTEMPTS; attempt++) {
            try {
                FriendInvite invite = friendInviteRepository.saveAndFlush(
                        FriendInvite.create(userId, friendInviteCodeGenerator.generate())
                );
                return new FriendInviteResponse(invite.getCode(), currentUser.getEmail());
            } catch (DataIntegrityViolationException exception) {
                FriendInvite inviteCreatedByConcurrentRequest = friendInviteRepository.findByUserId(userId)
                        .orElse(null);
                if (inviteCreatedByConcurrentRequest != null) {
                    return new FriendInviteResponse(inviteCreatedByConcurrentRequest.getCode(), currentUser.getEmail());
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사용 가능한 친구 초대코드를 생성할 수 없습니다.");
    }

    @Transactional(readOnly = true)
    public FriendInviteeResponse getInvitee(String code, Long currentUserId) {
        FriendInvite invite = friendInviteRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대코드를 찾을 수 없습니다."));
        User invitee = getActiveUser(invite.getUserId());

        return friendInviteStatisticsService.enrich(
                invitee, friendResponseMapper.toUserResponse(invitee, relationshipBetween(currentUserId, invitee.getId()))
        );
    }

    @Transactional(readOnly = true)
    public FriendSearchResponse searchByUserCode(String userCode, Long currentUserId) {
        String normalizedUserCode = UserCode.parse(userCode).value();

        User target = userRepository.findActiveByUserCode(normalizedUserCode, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 코드에 해당하는 사용자를 찾을 수 없습니다."));

        FriendUserResponse base = friendResponseMapper.toUserResponse(target, relationshipBetween(currentUserId, target.getId()));
        if (base.relationshipStatus() == FriendRelationshipStatus.SELF) {
            return toSearchResponse(base, 0, null);
        }
        MutualFriendSummary summary = friendRepository.summarizeMutualFriends(currentUserId, target.getId());
        String previewName = summary.getPreviewUserId() == null ? null
                : userRepository.findById(summary.getPreviewUserId())
                        .filter(User::isActive).map(User::getPublicDisplayName).orElse(null);
        return toSearchResponse(base, summary.getMutualFriendCount(), previewName);
    }

    @Transactional
    public FriendRequestResponse sendRequest(Long fromUserId, Long targetUserId) {
        if (fromUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인에게 친구 요청을 보낼 수 없습니다.");
        }

        Map<Long, User> lockedUsers = lockActiveUsers(fromUserId, targetUserId);
        User target = lockedUsers.get(targetUserId);

        Optional<Friend> existingRelation = friendRepository.findByUserPair(fromUserId, targetUserId);
        if (existingRelation.isPresent()) {
            if (existingRelation.get().isAccepted()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 친구인 사용자입니다.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 대기 중인 친구 요청이 있습니다.");
        }

        Friend request = Friend.request(fromUserId, targetUserId);
        try {
            request = friendRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 대기 중인 친구 요청이 있습니다.", exception);
        }

        eventPublisher.publishEvent(new FriendRequestSentEvent(fromUserId, targetUserId));
        return friendResponseMapper.toRequestResponse(request, target, FriendRelationshipStatus.PENDING_SENT);
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getSentRequests(Long userId) {
        return toRequestResponses(
                friendRepository.findSentPendingRequests(userId),
                userId,
                FriendRelationshipStatus.PENDING_SENT
        );
    }

    @Transactional(readOnly = true)
    public List<FriendReceivedRequestResponse> getReceivedRequests(Long userId) {
        List<Friend> requests = friendRepository.findReceivedPendingRequests(userId);
        Set<Long> senderIds = requests.stream().map(Friend::getFromUserId).collect(Collectors.toSet());
        Map<Long, User> senders = userRepository.findAllById(senderIds).stream()
                .filter(User::isActive).collect(Collectors.toMap(User::getId, user -> user));
        return requests.stream()
                .filter(request -> senders.containsKey(request.getFromUserId()))
                .map(request -> friendResponseMapper.toReceivedRequestResponse(request, senders.get(request.getFromUserId())))
                .toList();
    }

    @Transactional
    public FriendResponse acceptRequest(Long requestId, Long userId) {
        Friend request = getFriend(requestId);
        validatePending(request);
        if (!request.isTo(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "받은 친구 요청만 수락할 수 있습니다.");
        }

        int updated = friendRepository.acceptPendingRequest(
                requestId,
                userId,
                LocalDateTime.now(clock)
        );
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "친구 요청 상태가 변경되었습니다.");
        }

        Friend acceptedFriend = getFriend(requestId);
        User friendUser = getUser(acceptedFriend.otherUserId(userId));
        eventPublisher.publishEvent(new FriendRequestAcceptedEvent(userId, acceptedFriend.getFromUserId()));
        return friendResponseMapper.toFriendResponse(acceptedFriend, friendUser);
    }

    @Transactional
    public void deletePendingRequest(Long requestId, Long userId) {
        Friend request = getFriend(requestId);
        validatePending(request);
        if (!request.isTo(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "받은 친구 요청만 거절할 수 있습니다.");
        }

        int deleted = friendRepository.deletePendingRequest(requestId, userId);
        if (deleted != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "친구 요청 상태가 변경되었습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(Long userId) {
        List<Friend> friendships = friendRepository.findAcceptedFriendshipsByUserId(userId);
        Set<Long> friendUserIds = friendships.stream()
                .map(friend -> friend.otherUserId(userId))
                .collect(Collectors.toSet());
        Map<Long, User> usersById = userRepository.findAllById(friendUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return friendships.stream()
                .map(friend -> {
                    User friendUser = usersById.get(friend.otherUserId(userId));
                    if (friendUser == null) {
                        return null;
                    }
                    return friendResponseMapper.toFriendResponse(friend, friendUser);
                })
                .filter(response -> response != null)
                .toList();
    }

    @Transactional
    public void unfriend(Long friendshipId, Long userId) {
        Friend friendship = getFriend(friendshipId);
        if (!friendship.isAccepted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 친구 관계가 아닙니다.");
        }
        if (!friendship.involves(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "친구 관계를 해제할 권한이 없습니다.");
        }

        int deleted = friendRepository.deleteAcceptedFriend(friendshipId, userId);
        if (deleted != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "친구 관계 상태가 변경되었습니다.");
        }
    }

    private List<FriendRequestResponse> toRequestResponses(
            List<Friend> requests,
            Long userId,
            FriendRelationshipStatus relationshipStatus
    ) {
        Set<Long> userIds = requests.stream()
                .map(request -> request.otherUserId(userId))
                .collect(Collectors.toSet());
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .filter(User::isActive)
                .collect(Collectors.toMap(User::getId, user -> user));

        return requests.stream()
                .map(request -> {
                    User otherUser = usersById.get(request.otherUserId(userId));
                    if (otherUser == null) {
                        return null;
                    }
                    return friendResponseMapper.toRequestResponse(request, otherUser, relationshipStatus);
                })
                .filter(response -> response != null)
                .toList();
    }

    private FriendSearchResponse toSearchResponse(FriendUserResponse base, long mutualFriendCount, String previewName) {
        return new FriendSearchResponse(
                base.userId(), base.displayName(), base.profileImageUrl(), base.relationshipStatus(), base.requestId(),
                mutualFriendCount, previewName
        );
    }

    private FriendRelationshipStatus relationshipBetween(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return FriendRelationshipStatus.SELF;
        }

        return friendRepository.findByUserPair(currentUserId, targetUserId)
                .map(friend -> {
                    if (friend.isAccepted()) {
                        return FriendRelationshipStatus.FRIEND;
                    }
                    return friend.isFrom(currentUserId)
                            ? FriendRelationshipStatus.PENDING_SENT
                            : FriendRelationshipStatus.PENDING_RECEIVED;
                })
                .orElse(FriendRelationshipStatus.NONE);
    }

    private User getActiveUser(Long userId) {
        User user = getUser(userId);
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    private Map<Long, User> lockActiveUsers(Long firstUserId, Long secondUserId) {
        Long firstLockId = Math.min(firstUserId, secondUserId);
        Long secondLockId = Math.max(firstUserId, secondUserId);
        User firstUser = getActiveUserForUpdate(firstLockId);
        User secondUser = getActiveUserForUpdate(secondLockId);
        return Map.of(firstUser.getId(), firstUser, secondUser.getId(), secondUser);
    }

    private User getActiveUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Friend getFriend(Long friendId) {
        return friendRepository.findById(friendId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "친구 관계를 찾을 수 없습니다."));
    }

    private void validatePending(Friend request) {
        if (!request.isPending()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "대기 중인 친구 요청이 아닙니다.");
        }
    }
}

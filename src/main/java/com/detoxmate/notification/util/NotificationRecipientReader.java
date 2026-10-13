package com.detoxmate.notification.util;

import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.feed.FeedErrorCode;
import com.detoxmate.friend.repository.FriendRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.detoxmate.group.dto.GroupChallengeParticipantRow;
import com.detoxmate.group.dto.GroupMemberUserQueryResult;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.notification.dto.ChallengeRecordNotificationRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationRecipientReader {

    private final ChallengeRecordRepository challengeRecordRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupChallengeParticipantRepository participantRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    public ChallengeRecordNotificationRow findChallengeRecordInfo(Long challengeRecordId) {
        return challengeRecordRepository.findChallengeRecordNotificationRow(challengeRecordId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
    }

    public List<Long> findActiveGroupMemberUserIds(Long groupId) {
        return groupMemberRepository.findMemberUserQueryResultsByGroupId(groupId).stream()
                .map(GroupMemberUserQueryResult::userId)
                .toList();
    }

    public List<Long> findGroupChallengeParticipantUserIds(Long groupChallengeId) {
        return participantRepository.findParticipantRowsByGroupChallengeId(groupChallengeId).stream()
                .map(GroupChallengeParticipantRow::userId)
                .toList();
    }

    public List<Long> findFriendWhenUnlock(Long userId){
        // 수락된 친구 관계에서 상대방의 사용자 ID를 추출
        List<Long> friendUserIds = friendRepository
                .findAcceptedFriendshipsByUserId(userId)
                .stream()
                .map(friend -> friend.otherUserId(userId))
                .filter(friendUserid -> !friendUserid.equals(userId))
                .distinct()
                .toList();

        if(friendUserIds.isEmpty()){
            return List.of();
        }

        return userRepository.findAllById(friendUserIds)
                .stream()
                .filter(User::isActive)
                .map(User::getId)
                .toList();

    }
}

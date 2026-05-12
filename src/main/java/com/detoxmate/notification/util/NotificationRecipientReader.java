package com.detoxmate.notification.util;

import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.feed.FeedErrorCode;
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
}

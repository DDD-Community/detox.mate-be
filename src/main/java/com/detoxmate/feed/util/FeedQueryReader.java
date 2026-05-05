package com.detoxmate.feed.util;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.feed.FeedErrorCode;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.dto.GroupChallengeParticipantResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedQueryReader {

    private final GroupChallengeRepository groupChallengeRepository;
    private final GroupRepository groupRepository;
    private final GroupChallengeParticipantRepository participantRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ChallengeRecordStatusCountRepository statusCountRepository;

    public GroupChallengeFeedSource getHomeFeedSource(Long groupChallengeId) {
        GroupChallenge challenge = groupChallengeRepository.findById(groupChallengeId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_GROUP_CHALLENGE_NOT_FOUND));

        Group group = groupRepository.findById(challenge.getGroupId())
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_GROUP_NOT_FOUND));

        List<GroupChallengeParticipantResponse> participants =
                participantRepository.findFeedParticipantResponsesByGroupChallengeId(groupChallengeId);

        return new GroupChallengeFeedSource(challenge, group, participants);
    }

    public GroupChallengeParticipantResponse getParticipantForFeedDetail(Long participantId) {
        return participantRepository.findParticipantResponseForFeedDetail(participantId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_PARTICIPANT_NOT_FOUND));
    }

    public ActivityRecord findActivityRecord(Long activityRecordId) {
        if (activityRecordId == null) {
            return null;
        }

        return activityRecordRepository.findById(activityRecordId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_ACTIVITY_RECORD_NOT_FOUND));
    }

    public ChallengeRecordStatusCount findStatusCount(Long challengeRecordId) {
        return statusCountRepository.findByChallengeRecordId(challengeRecordId)
                .orElse(null);
    }
}

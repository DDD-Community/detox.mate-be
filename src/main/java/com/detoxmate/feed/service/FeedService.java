package com.detoxmate.feed.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecordchallengestatus.domain.ActivityRecordChallengeStatus;
import com.detoxmate.activityrecordchallengestatus.repository.ActivityRecordChallengeStatusRepository;
import com.detoxmate.feed.dto.response.HomeFeedChallengeInfo;
import com.detoxmate.feed.dto.response.HomeFeedMemberCard;
import com.detoxmate.feed.dto.response.HomeFeedResponse;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.dto.GroupChallengeParticipantResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.poke.repository.PokeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private static final String VERIFIED = "VERIFIED";
    private static final String NOT_YET = "NOT_YET";

    private final GroupChallengeRepository groupChallengeRepository;
    private final GroupRepository groupRepository;
    private final GroupChallengeParticipantRepository participantRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityRecordChallengeStatusRepository statusRepository;
    private final PokeRepository pokeRepository;

    @Transactional(readOnly = true)
    public HomeFeedResponse getHomeFeed(Long groupChallengeId, Long currentUserId) {
        GroupChallenge challenge = groupChallengeRepository.findById(groupChallengeId).orElseThrow();

        Group group = groupRepository.findById(challenge.getGroupId())
                .orElseThrow();

        List<GroupChallengeParticipantResponse> participants =
                participantRepository.findParticipantResponsesByGroupChallengeId(groupChallengeId);

        List<HomeFeedMemberCard> members = participants.stream()
                .map(participant -> toMemberCard(groupChallengeId, currentUserId, participant))
                .toList();

        return new HomeFeedResponse(
                toChallengeInfo(challenge, group),
                members
        );
    }

    private HomeFeedChallengeInfo toChallengeInfo(GroupChallenge challenge, Group group) {
        return new HomeFeedChallengeInfo(
                challenge.getId(),
                group.getName(),
                challenge.getStartAt() == null ? null : challenge.getStartAt().toInstant(ZoneOffset.UTC),
                0
        );
    }

    private HomeFeedMemberCard toMemberCard(
            Long groupChallengeId,
            Long currentUserId,
            GroupChallengeParticipantResponse participant
    ) {
        ActivityRecord activityRecord = activityRecordRepository
                .findTopByGroupChallengeParticipantIdOrderByCreatedAtDesc(participant.groupChallengeParticipantId())
                .orElse(null);

        if (activityRecord == null) {
            return notYetCard(participant);
        }

        ActivityRecordChallengeStatus status = statusRepository
                .findByChallengeRecord(groupChallengeId, activityRecord.getId())
                .orElse(null);

        boolean isPoked = pokeRepository.existsPoke(
                groupChallengeId,
                activityRecord.getId(),
                currentUserId,
                participant.userId(),
                LocalDate.now()
        );

        return verifiedCard(participant, activityRecord, status, isPoked);
    }

    private HomeFeedMemberCard notYetCard(GroupChallengeParticipantResponse participant) {
        return new HomeFeedMemberCard(
                participant.userId(),
                participant.groupMemberId(),
                participant.displayName(),
                participant.profileImageUrl(),
                NOT_YET,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                false
        );
    }

    private HomeFeedMemberCard verifiedCard(
            GroupChallengeParticipantResponse participant,
            ActivityRecord activityRecord,
            ActivityRecordChallengeStatus status,
            boolean isPoked
    ) {
        return new HomeFeedMemberCard(
                participant.userId(),
                participant.groupMemberId(),
                participant.displayName(),
                participant.profileImageUrl(),
                VERIFIED,
                activityRecord.getActivityImageUrl(),
                activityRecord.getReflectionText(),
                null,
                null,
                activityRecord.getId(),
                reactionCount(status),
                commentCount(status),
                pokeCount(status),
                isPoked
        );
    }

    private int commentCount(ActivityRecordChallengeStatus status) {
        return status == null ? 0 : status.getCommentCount();
    }

    private int reactionCount(ActivityRecordChallengeStatus status) {
        return status == null ? 0 : status.getReactionCount();
    }

    private int pokeCount(ActivityRecordChallengeStatus status) {
        return status == null ? 0 : status.getPokeCount();
    }
}

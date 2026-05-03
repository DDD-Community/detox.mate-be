package com.detoxmate.feed.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.feed.dto.response.HomeFeedChallengeInfo;
import com.detoxmate.feed.dto.response.HomeFeedMemberCard;
import com.detoxmate.feed.dto.response.HomeFeedResponse;
import com.detoxmate.feed.util.FeedQueryReader;
import com.detoxmate.feed.util.GroupChallengeFeedSource;
import com.detoxmate.group.dto.GroupChallengeParticipantResponse;
import com.detoxmate.poke.service.PokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedQueryReader feedQueryReader;
    private final ChallengeRecordService challengeRecordService;
    private final PokeService pokeService;

    @Transactional
    public HomeFeedResponse getHomeFeed(Long groupChallengeId, Long currentUserId) {
        GroupChallengeFeedSource source = feedQueryReader.getHomeFeedSource(groupChallengeId);

        LocalDate today = LocalDate.now();

        List<HomeFeedMemberCard> members = source.participants().stream()
                .map(participant -> {
                    ChallengeRecord challengeRecord = challengeRecordService.create(
                            groupChallengeId,
                            participant.id(),
                            today
                    );

                    return toMemberCard(participant, challengeRecord, currentUserId);
                })
                .sorted(this::compareMemberCard)
                .toList();

        return new HomeFeedResponse(
                toChallengeInfo(source),
                members
        );
    }

    private HomeFeedChallengeInfo toChallengeInfo(GroupChallengeFeedSource source) {
        return new HomeFeedChallengeInfo(
                source.challenge().getId(),
                source.group().getName(),
                source.challenge().getStartAt(),
                0
        );
    }

    private HomeFeedMemberCard toMemberCard(GroupChallengeParticipantResponse participant,
                                            ChallengeRecord challengeRecord,
                                            Long currentUserId) {

        ActivityRecord activityRecord = feedQueryReader.findActivityRecord(
                challengeRecord.getActivityRecordId()
        );

        ChallengeRecordStatusCount statusCount = feedQueryReader.findStatusCount(
                challengeRecord.getId()
        );

        boolean poked = challengeRecord.isBeforeRecord()
                && pokeService.existsPoke(
                challengeRecord.getId(),
                currentUserId,
                participant.userId()
        );

        return new HomeFeedMemberCard(
                participant.userId(),
                participant.groupMemberId(),
                participant.displayName(),
                participant.profileImageUrl(),
                challengeRecord.getId(),
                challengeRecord.getStatus().name(),
                activityRecord == null ? null : activityRecord.getActivityImageObjectKey(),
                activityRecord == null ? null : activityRecord.getReflectionText(),
                activityRecord == null ? null : totalUsedMinutes(activityRecord),
                null,
                challengeRecord.getActivityRecordId(),
                activityRecord == null ? null : activityRecord.getCreatedAt(),
                reactionCount(statusCount),
                commentCount(challengeRecord, statusCount),
                pokeCount(statusCount),
                poked
        );
    }

    private int compareMemberCard(HomeFeedMemberCard first, HomeFeedMemberCard second) {
        boolean firstVerified = first.activityRecordId() != null;
        boolean secondVerified = second.activityRecordId() != null;

        if (firstVerified != secondVerified) {
            return firstVerified ? -1 : 1;
        }

        if (firstVerified) {
            return second.verifiedAt().compareTo(first.verifiedAt());
        }

        return first.displayName().compareTo(second.displayName());
    }

    private int totalUsedMinutes(ActivityRecord activityRecord) {
        return activityRecord.getDetails().stream()
                .mapToInt(detail -> detail.getUseMinutes() == null ? 0 : detail.getUseMinutes())
                .sum();
    }

    private int commentCount(ChallengeRecord challengeRecord, ChallengeRecordStatusCount statusCount) {
        if (statusCount == null) {
            return 0;
        }

        if (challengeRecord.isBeforeRecord()) {
            return statusCount.getBeforeCommentCount();
        }

        return statusCount.getAfterCommentCount();
    }

    private int reactionCount(ChallengeRecordStatusCount statusCount) {
        return statusCount == null ? 0 : statusCount.getReactionCount();
    }

    private int pokeCount(ChallengeRecordStatusCount statusCount) {
        return statusCount == null ? 0 : statusCount.getPokeCount();
    }

}

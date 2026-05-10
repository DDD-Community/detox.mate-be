package com.detoxmate.feed.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.feed.FeedErrorCode;
import com.detoxmate.feed.dto.response.*;
import com.detoxmate.feed.util.FeedQueryReader;
import com.detoxmate.group.dto.GroupChallengeParticipantResponse;
import com.detoxmate.group.service.GroupChallengeParticipantService;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.service.PokeService;
import com.detoxmate.reaction.domain.Reaction;
import com.detoxmate.reaction.service.ReactionService;
import com.detoxmate.user.dto.MyProfileResponse;
import com.detoxmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedDetailService {

    private final ChallengeRecordService challengeRecordService;
    private final FeedQueryReader feedQueryReader;
    private final ReactionService reactionService;
    private final PokeService pokeService;
    private final UserService userService;

    private final GroupChallengeParticipantService participantService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FeedDetailResponse getFeedDetail(Long challengeRecordId, Long currentUserId) {
        ChallengeRecord challengeRecord = challengeRecordService.get(challengeRecordId);

        validateParticipant(challengeRecord.getGroupChallengeId(), currentUserId);

        GroupChallengeParticipantResponse participant = feedQueryReader.getParticipantForFeedDetail(
                challengeRecord.getGroupChallengeParticipantId()
        );

        ChallengeRecordStatusCount statusCount = feedQueryReader.findStatusCount(challengeRecordId);

        if (challengeRecord.isBeforeRecord()) {
            return beforeRecordDetail(
                    challengeRecord,
                    participant,
                    statusCount,
                    currentUserId
            );
        }

        ActivityRecord activityRecord = feedQueryReader.findActivityRecord(challengeRecord.getActivityRecordId());

        return afterRecordDetail(
                challengeRecord,
                participant,
                activityRecord,
                statusCount
        );
    }

    private FeedDetailResponse beforeRecordDetail(ChallengeRecord challengeRecord,
                                                  GroupChallengeParticipantResponse participant,
                                                  ChallengeRecordStatusCount statusCount,
                                                  Long currentUserId) {

        List<Poke> pokes = pokeService.getPokesForChallengeRecord(challengeRecord.getId());

        boolean poked = pokes.stream().anyMatch(poke -> Objects.equals(poke.getSenderUserId(), currentUserId)
                        && Objects.equals(poke.getReceiverUserId(), participant.userId()));

        return new FeedDetailResponse(
                challengeRecord.getId(),
                challengeRecord.getGroupChallengeId(),
                null,
                challengeRecord.getStatus().name(),
                challengeRecord.getRecordDate(),
                toAuthor(participant),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                new FeedDetailReactionSummary(0, List.of()),
                beforeCommentCount(statusCount),
                pokeCount(statusCount),
                challengeRecord.isToday(LocalDate.now(clock)),
                poked,
                toPokedUsers(pokes)
        );
    }

    private FeedDetailResponse afterRecordDetail(ChallengeRecord challengeRecord,
                                                 GroupChallengeParticipantResponse participant,
                                                 ActivityRecord activityRecord,
                                                 ChallengeRecordStatusCount statusCount) {
        if (activityRecord == null) {
            throw new CustomException(FeedErrorCode.FEED_ACTIVITY_RECORD_NOT_FOUND);
        }

        List<Reaction> reactions = reactionService.getReactionsForChallengeRecord(challengeRecord.getId());

        return new FeedDetailResponse(
                challengeRecord.getId(),
                challengeRecord.getGroupChallengeId(),
                activityRecord.getId(),
                challengeRecord.getStatus().name(),
                challengeRecord.getRecordDate(),
                toAuthor(participant),
                activityRecord.getCreatedAt(),
                activityRecord.getActivityImageObjectKey(),
                activityRecord.getReflectionText(),
                goalStatus(challengeRecord),
                snapshotGoalMinutes(activityRecord),
                toUsageDetails(activityRecord),
                toReactionSummary(reactions),
                afterCommentCount(statusCount),
                pokeCount(statusCount),
                false,
                false,
                List.of()
        );
    }

    private FeedGoalStatus goalStatus(ChallengeRecord challengeRecord) {
        return switch (challengeRecord.getCertificationResult()) {
            case SUCCESS -> FeedGoalStatus.SUCCESS;
            case FAIL -> FeedGoalStatus.FAIL;
            case null -> null;
        };
    }

    private FeedDetailAuthorInfo toAuthor(GroupChallengeParticipantResponse participant) {
        return new FeedDetailAuthorInfo(
                participant.userId(),
                participant.displayName(),
                participant.profileImageUrl()
        );
    }

    private List<FeedDetailUsageDetail> toUsageDetails(ActivityRecord activityRecord) {
        return activityRecord.getDetails().stream()
                .map(this::toUsageDetail)
                .toList();
    }

    private FeedDetailUsageDetail toUsageDetail(ActivityRecordDetail detail) {
        return new FeedDetailUsageDetail(
                detail.getUsageGoalType().getCode().name(),
                detail.getUseMinutes()
        );
    }

    private Integer snapshotGoalMinutes(ActivityRecord activityRecord) {
        if (activityRecord.getDetails().isEmpty()) {
            return null;
        }

        return activityRecord.getDetails().stream()
                .map(ActivityRecordDetail::getUserUsageGoalTime)
                .mapToInt(goalTime -> goalTime.getGoalMinutes() == null ? 0 : goalTime.getGoalMinutes())
                .sum();
    }

    private FeedDetailReactionSummary toReactionSummary(List<Reaction> reactions) {
        Map<Long, MyProfileResponse> profiles = userService.getProfilesByIds(
                reactions.stream()
                        .map(Reaction::getUserId)
                        .collect(Collectors.toSet())
        );

        List<FeedDetailReactionItem> items = reactions.stream()
                .map(reaction -> toReactionItem(reaction, profiles.get(reaction.getUserId())))
                .toList();

        return new FeedDetailReactionSummary(items.size(), items);
    }

    private FeedDetailReactionItem toReactionItem(Reaction reaction, MyProfileResponse profile) {
        return new FeedDetailReactionItem(
                reaction.getBody().name(),
                reaction.getUserId(),
                profile == null ? null : profile.displayName(),
                profile == null ? null : profile.profileImageUrl()
        );
    }

    private List<FeedDetailPokedUser> toPokedUsers(List<Poke> pokes) {
        Map<Long, MyProfileResponse> profiles = userService.getProfilesByIds(
                pokes.stream()
                        .map(Poke::getSenderUserId)
                        .collect(Collectors.toSet())
        );

        return pokes.stream()
                .map(poke -> toPokedUser(poke, profiles.get(poke.getSenderUserId())))
                .toList();
    }

    private FeedDetailPokedUser toPokedUser(Poke poke, MyProfileResponse profile) {
        return new FeedDetailPokedUser(
                poke.getSenderUserId(),
                profile == null ? null : profile.displayName(),
                profile == null ? null : profile.profileImageUrl()
        );
    }

    private int beforeCommentCount(ChallengeRecordStatusCount statusCount) {
        return statusCount == null ? 0 : statusCount.getBeforeCommentCount();
    }

    private int afterCommentCount(ChallengeRecordStatusCount statusCount) {
        return statusCount == null ? 0 : statusCount.getAfterCommentCount();
    }

    private int pokeCount(ChallengeRecordStatusCount statusCount) {
        return statusCount == null ? 0 : statusCount.getPokeCount();
    }

    private void validateParticipant(Long groupChallengeId, Long currentUserId) {
        if (!participantService.checkGroupChallengeParticipant(groupChallengeId, currentUserId)) {
            throw new CustomException(FeedErrorCode.FEED_ACCESS_DENIED);
        }
    }
}

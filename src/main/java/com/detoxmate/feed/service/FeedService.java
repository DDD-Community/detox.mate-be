package com.detoxmate.feed.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.common.MemberActivityOrder;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.feed.FeedErrorCode;
import com.detoxmate.feed.dto.response.GroupChallengeOverviewResponse;
import com.detoxmate.feed.dto.response.HomeFeedChallengeInfo;
import com.detoxmate.feed.dto.response.HomeFeedMemberCard;
import com.detoxmate.feed.dto.response.HomeFeedResponse;
import com.detoxmate.feed.util.FeedQueryReader;
import com.detoxmate.feed.util.GroupChallengeFeedSource;
import com.detoxmate.group.dto.GroupChallengeParticipantResponse;
import com.detoxmate.group.service.GroupChallengeParticipantService;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.service.PokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final FeedQueryReader feedQueryReader;
    private final ChallengeRecordService challengeRecordService;
    private final GroupChallengeParticipantService participantService;
    private final PokeService pokeService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public GroupChallengeOverviewResponse getGroupChallengeOverview(Long groupChallengeId, Long currentUserId) {
        validateParticipant(groupChallengeId, currentUserId);

        GroupChallengeFeedSource source = feedQueryReader.getHomeFeedSource(groupChallengeId);

        return new GroupChallengeOverviewResponse(
                source.challenge().getId(),
                source.group().getId(),
                source.group().getName(),
                source.challenge().getChallengeNo(),
                source.challenge().getStatus().name(),
                source.challenge().getStartAt(),
                source.challenge().getEndAt(),
                0
        );
    }

    @Transactional
    public HomeFeedResponse getHomeFeed(Long groupChallengeId, Long currentUserId) {
        validateParticipant(groupChallengeId, currentUserId);

        GroupChallengeFeedSource source = feedQueryReader.getHomeFeedSource(groupChallengeId);

        LocalDate today = LocalDate.now(clock.withZone(KST));

        List<FeedMemberSource> memberSources = source.participants().stream()
                .map(participant -> {
                    ChallengeRecord challengeRecord = challengeRecordService.create(
                            groupChallengeId,
                            participant.id(),
                            today
                    );

                    return new FeedMemberSource(participant, challengeRecord);
                })
                .toList();
        Set<PokeKey> pokedRecords = pokedRecords(memberSources, currentUserId);

        List<HomeFeedMemberCard> members = memberSources.stream()
                .map(memberSource -> toMemberCard(
                        memberSource.participant(),
                        memberSource.challengeRecord(),
                        pokedRecords
                ))
                .sorted(MemberActivityOrder.latestCertifiedThenDisplayName(
                        HomeFeedMemberCard::verifiedAt,
                        HomeFeedMemberCard::displayName
                ))
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
                                            Set<PokeKey> pokedRecords) {

        ActivityRecord activityRecord = feedQueryReader.findActivityRecord(
                challengeRecord.getActivityRecordId()
        );

        ChallengeRecordStatusCount statusCount = feedQueryReader.findStatusCount(
                challengeRecord.getId()
        );

        boolean poked = challengeRecord.isBeforeRecord()
                && pokedRecords.contains(new PokeKey(challengeRecord.getId(), participant.userId()));

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

    private Set<PokeKey> pokedRecords(List<FeedMemberSource> memberSources, Long currentUserId) {
        List<Long> beforeRecordIds = memberSources.stream()
                .map(FeedMemberSource::challengeRecord)
                .filter(ChallengeRecord::isBeforeRecord)
                .map(ChallengeRecord::getId)
                .toList();

        return pokeService.getPokesByChallengeRecordsAndSender(beforeRecordIds, currentUserId).stream()
                .map(PokeKey::from)
                .collect(Collectors.toSet());
    }

    private void validateParticipant(Long groupChallengeId, Long currentUserId) {
        if (!participantService.checkGroupChallengeParticipant(groupChallengeId, currentUserId)) {
            throw new CustomException(FeedErrorCode.FEED_ACCESS_DENIED);
        }
    }

    private record FeedMemberSource(
            GroupChallengeParticipantResponse participant,
            ChallengeRecord challengeRecord
    ) {
    }

    private record PokeKey(Long challengeRecordId, Long receiverUserId) {

        private static PokeKey from(Poke poke) {
            return new PokeKey(poke.getChallengeRecordId(), poke.getReceiverUserId());
        }
    }
}

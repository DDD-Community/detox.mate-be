package com.detoxmate.feed.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.domain.ActivityRecordDetail;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecord.service.ChallengeRecordService;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.common.MemberActivityOrder;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.feed.FeedErrorCode;
import com.detoxmate.feed.dto.response.GroupChallengeRecordFeedResponse;
import com.detoxmate.group.activity.CalendarDayStatus;
import com.detoxmate.group.activity.GroupActivityParticipant;
import com.detoxmate.group.activity.GroupActivityVerificationPolicy;
import com.detoxmate.group.activity.GroupDailyVerification;
import com.detoxmate.group.activity.MemberDailyGoal;
import com.detoxmate.group.activity.MemberDailyStatus;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.dto.ActivityRecordDetailHistoryResponse;
import com.detoxmate.group.dto.GroupActivityParticipantRow;
import com.detoxmate.group.dto.GroupDailyVerificationSummaryResponse;
import com.detoxmate.group.dto.MemberDailyGoalResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.group.service.GroupChallengeParticipantService;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.service.PokeService;
import com.detoxmate.reaction.domain.Reaction;
import com.detoxmate.reaction.service.ReactionService;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.user.dto.UserProfileSummary;
import com.detoxmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupChallengeRecordFeedService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String ACTIVE_MEMBER_STATUS = "ACTIVE";
    private static final String JOINED_PARTICIPANT_STATUS = "JOINED";

    private final GroupRepository groupRepository;
    private final GroupChallengeRepository groupChallengeRepository;
    private final GroupChallengeParticipantRepository participantRepository;
    private final GroupChallengeParticipantService participantService;
    private final UserUsageGoalTimeRepository userUsageGoalTimeRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final ChallengeRecordService challengeRecordService;
    private final ActivityRecordRepository activityRecordRepository;
    private final ChallengeRecordStatusCountRepository statusCountRepository;
    private final PokeService pokeService;
    private final ReactionService reactionService;
    private final UserService userService;
    private final ImageReadUrlBuilder imageReadUrlBuilder;
    private final GroupActivityVerificationPolicy verificationPolicy;
    private final Clock clock;

    @Transactional
    public GroupChallengeRecordFeedResponse getTodayFeed(Long groupChallengeId, Long currentUserId) {
        GroupActivityContext activity = loadActivityContext(groupChallengeId, currentUserId);
        LocalDate date = today();

        ensureTodayChallengeRecordsForActiveParticipants(activity, date);

        GroupActivityDayContext activityDay = loadActivityDayContext(activity, date, currentUserId, true);

        return new GroupChallengeRecordFeedResponse(
                activity.challenge().getGroupId(),
                date,
                toDailySummary(activityDay),
                toTodayMemberResponses(activityDay)
        );
    }

    public GroupChallengeRecordFeedResponse getHistoryFeed(
            Long groupChallengeId,
            LocalDate date,
            Long currentUserId
    ) {
        if (!date.isBefore(today())) {
            throw new CustomException(FeedErrorCode.FEED_HISTORY_DATE_MUST_BE_PAST);
        }

        GroupActivityContext activity = loadActivityContext(groupChallengeId, currentUserId);
        GroupActivityDayContext activityDay = loadActivityDayContext(activity, date, currentUserId, false);

        return new GroupChallengeRecordFeedResponse(
                activity.challenge().getGroupId(),
                date,
                toDailySummary(activityDay),
                toHistoryMemberResponses(activityDay)
        );
    }

    public GroupChallengeRecordFeedResponse.MemberResponse getDetail(
            Long groupChallengeId,
            Long challengeRecordId,
            Long currentUserId
    ) {
        GroupActivityContext activity = loadActivityContext(groupChallengeId, currentUserId);
        ChallengeRecord challengeRecord = challengeRecordService.get(challengeRecordId);
        if (!Objects.equals(challengeRecord.getGroupChallengeId(), groupChallengeId)) {
            throw new CustomException(FeedErrorCode.FEED_ACCESS_DENIED);
        }

        GroupActivityDayContext activityDay = loadActivityDayContext(
                activity,
                challengeRecord.getRecordDate(),
                currentUserId,
                challengeRecord.isToday(today())
        );

        return activity.participantRows().stream()
                .filter(row -> Objects.equals(
                        row.groupChallengeParticipantId(),
                        challengeRecord.getGroupChallengeParticipantId()
                ))
                .map(row -> toMemberResponse(row, activityDay, true))
                .findFirst()
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_PARTICIPANT_NOT_FOUND));
    }

    private GroupActivityContext loadActivityContext(Long groupChallengeId, Long currentUserId) {
        validateParticipant(groupChallengeId, currentUserId);

        GroupChallenge challenge = groupChallengeRepository.findById(groupChallengeId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_GROUP_CHALLENGE_NOT_FOUND));
        groupRepository.findById(challenge.getGroupId())
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_GROUP_NOT_FOUND));

        List<GroupActivityParticipantRow> participantRows =
                participantRepository.findActivityParticipantRowsByGroupChallengeId(groupChallengeId);
        List<GroupActivityParticipant> participants = participantRows.stream()
                .map(this::toParticipant)
                .toList();
        List<MemberDailyGoal> goals = memberDailyGoals(participantRows);

        return new GroupActivityContext(challenge, participantRows, participants, goals);
    }

    private GroupActivityDayContext loadActivityDayContext(
            GroupActivityContext activity,
            LocalDate date,
            Long currentUserId,
            boolean todayFeed
    ) {
        LocalDate today = today();
        LocalDate firstVerificationDate = firstVerificationDate(activity, today);
        CalendarDayStatus dayStatus = dayStatus(date, today, firstVerificationDate);
        List<ChallengeRecord> challengeRecords = challengeRecordRepository.findAllByGroupChallengeDate(
                activity.challenge().getId(),
                date
        );
        Set<Long> certifiedParticipantIds = certifiedParticipantIds(challengeRecords);

        return new GroupActivityDayContext(
                activity,
                date,
                currentUserId,
                dayStatus,
                certifiedParticipantIds,
                challengeRecordByParticipantId(challengeRecords),
                activityRecordById(challengeRecords),
                statusCountByRecordId(challengeRecords),
                pokedRecords(challengeRecords, currentUserId),
                participantById(activity.participants()),
                todayFeed
        );
    }

    private void ensureTodayChallengeRecordsForActiveParticipants(GroupActivityContext activity, LocalDate date) {
        activity.participants().stream()
                .filter(this::isActiveParticipant)
                .forEach(participant -> challengeRecordService.create(
                        activity.challenge().getId(),
                        participant.participantId(),
                        date
                ));
    }

    private void validateParticipant(Long groupChallengeId, Long currentUserId) {
        if (!participantService.checkGroupChallengeParticipant(groupChallengeId, currentUserId)) {
            throw new CustomException(FeedErrorCode.FEED_ACCESS_DENIED);
        }
    }

    private LocalDate firstVerificationDate(GroupActivityContext activity, LocalDate today) {
        return verificationPolicy.firstVerificationDate(
                activity.participants(),
                activity.goals(),
                challengeStartDate(activity.challenge()),
                today
        );
    }

    private LocalDate challengeStartDate(GroupChallenge challenge) {
        if (challenge.getStartAt() == null) {
            return null;
        }

        return challenge.getStartAt().toLocalDate();
    }

    private List<MemberDailyGoal> memberDailyGoals(List<GroupActivityParticipantRow> participantRows) {
        Set<Long> userIds = participantRows.stream()
                .map(GroupActivityParticipantRow::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return List.of();
        }

        return userUsageGoalTimeRepository.findAllByUser_IdIn(userIds).stream()
                .map(this::toMemberDailyGoal)
                .toList();
    }

    private MemberDailyGoal toMemberDailyGoal(UserUsageGoalTime goalTime) {
        LocalDate effectiveDate = goalTime.getCreatedAt().toLocalDate().plusDays(1);

        return new MemberDailyGoal(
                goalTime.getId(),
                goalTime.getUser().getId(),
                goalTime.getUsageGoalType().getCode(),
                goalTime.getGoalMinutes(),
                effectiveDate,
                goalTime.getCreatedAt()
        );
    }

    private GroupActivityParticipant toParticipant(GroupActivityParticipantRow row) {
        return new GroupActivityParticipant(
                row.groupChallengeParticipantId(),
                row.groupMemberId(),
                row.userId(),
                row.memberStatus(),
                row.memberJoinedAt(),
                row.memberLeftAt(),
                row.participantStatus(),
                row.participantJoinedAt(),
                row.participantWithdrawnAt()
        );
    }

    private Set<Long> certifiedParticipantIds(Collection<ChallengeRecord> challengeRecords) {
        return challengeRecords.stream()
                .filter(ChallengeRecord::isCertified)
                .map(ChallengeRecord::getGroupChallengeParticipantId)
                .collect(Collectors.toSet());
    }

    private GroupDailyVerificationSummaryResponse toDailySummary(GroupActivityDayContext activityDay) {
        if (activityDay.dayStatus() == CalendarDayStatus.NOT_STARTED
                || activityDay.dayStatus() == CalendarDayStatus.FUTURE) {
            return new GroupDailyVerificationSummaryResponse(
                    activityDay.date(),
                    activityDay.dayStatus().name(),
                    null,
                    0,
                    0,
                    0
            );
        }

        GroupDailyVerification verification = verificationPolicy.verifyConfirmedDate(
                activityDay.date(),
                activityDay.activity().participants(),
                activityDay.activity().goals(),
                activityDay.certifiedParticipantIds()
        );

        return new GroupDailyVerificationSummaryResponse(
                activityDay.date(),
                activityDay.dayStatus().name(),
                activityDay.dayStatus() == CalendarDayStatus.CONFIRMED ? verification.result().name() : null,
                verification.activeMemberCount(),
                verification.certifiedMemberCount(),
                verification.requiredCount()
        );
    }

    private List<GroupChallengeRecordFeedResponse.MemberResponse> toTodayMemberResponses(
            GroupActivityDayContext activityDay
    ) {
        return activityDay.activity().participantRows().stream()
                .filter(row -> isActiveParticipant(activityDay.participantById().get(row.groupChallengeParticipantId())))
                .map(row -> toMemberResponse(row, activityDay, false))
                .sorted(MemberActivityOrder.latestCertifiedThenDisplayName(
                        this::activitySubmittedAt,
                        GroupChallengeRecordFeedResponse.MemberResponse::displayName
                ))
                .toList();
    }

    private List<GroupChallengeRecordFeedResponse.MemberResponse> toHistoryMemberResponses(
            GroupActivityDayContext activityDay
    ) {
        return activityDay.activity().participantRows().stream()
                .map(row -> toMemberResponse(row, activityDay, false))
                .sorted(MemberActivityOrder.latestCertifiedThenDisplayName(
                        this::activitySubmittedAt,
                        GroupChallengeRecordFeedResponse.MemberResponse::displayName
                ))
                .toList();
    }

    private GroupChallengeRecordFeedResponse.MemberResponse toMemberResponse(
            GroupActivityParticipantRow row,
            GroupActivityDayContext activityDay,
            boolean includeReactions
    ) {
        GroupActivityParticipant participant = activityDay.participantById().get(row.groupChallengeParticipantId());
        ChallengeRecord challengeRecord = activityDay.challengeRecordByParticipantId()
                .get(row.groupChallengeParticipantId());
        boolean includedInGroupResult = includedInGroupResult(activityDay, participant);
        GroupChallengeRecordFeedResponse.ActivityRecordResponse activityRecord = toActivityRecordResponse(
                challengeRecord,
                activityDay.activityRecordById()
        );
        MemberDailyStatus dailyStatus = dailyStatus(includedInGroupResult, challengeRecord);
        ChallengeRecordStatusCount statusCount = challengeRecord == null
                ? null
                : activityDay.statusCountByRecordId().get(challengeRecord.getId());

        return new GroupChallengeRecordFeedResponse.MemberResponse(
                row.groupMemberId(),
                row.groupChallengeParticipantId(),
                row.userId(),
                row.displayName(),
                imageReadUrlBuilder.build(row.profileImageObjectKey()),
                row.userWithdrawn(),
                Objects.equals(row.userId(), activityDay.currentUserId()),
                row.memberStatus(),
                row.participantStatus(),
                dailyStatus.name(),
                includedInGroupResult,
                toGoalResponses(verificationPolicy.effectiveGoals(
                        row.userId(),
                        activityDay.activity().goals(),
                        activityDay.date()
                )),
                challengeRecord == null ? null : challengeRecord.getId(),
                activityRecord,
                reactionCount(statusCount),
                commentCount(challengeRecord, statusCount),
                pokeCount(statusCount),
                isPoked(challengeRecord, row.userId(), activityDay),
                includeReactions ? pokeable(challengeRecord) : null,
                includeReactions ? toPokedUsers(challengeRecord) : null,
                includeReactions ? toReactionSummary(challengeRecord) : null
        );
    }

    private Boolean pokeable(ChallengeRecord challengeRecord) {
        return challengeRecord != null
                && challengeRecord.isBeforeRecord()
                && challengeRecord.isToday(today());
    }

    private List<GroupChallengeRecordFeedResponse.PokedUserResponse> toPokedUsers(ChallengeRecord challengeRecord) {
        if (challengeRecord == null || !challengeRecord.isBeforeRecord()) {
            return List.of();
        }

        List<Poke> pokes = pokeService.getPokesForChallengeRecord(challengeRecord.getId());
        Map<Long, UserProfileSummary> profiles = userService.getProfileSummariesByIds(
                pokes.stream()
                        .map(Poke::getSenderUserId)
                        .collect(Collectors.toSet())
        );

        return pokes.stream()
                .map(poke -> toPokedUserResponse(poke, profiles.get(poke.getSenderUserId())))
                .toList();
    }

    private GroupChallengeRecordFeedResponse.PokedUserResponse toPokedUserResponse(
            Poke poke,
            UserProfileSummary profile
    ) {
        return new GroupChallengeRecordFeedResponse.PokedUserResponse(
                poke.getSenderUserId(),
                profile == null ? null : profile.displayName(),
                profile == null ? null : profile.profileImageUrl(),
                profile != null && profile.userWithdrawn()
        );
    }

    private GroupChallengeRecordFeedResponse.ReactionSummaryResponse toReactionSummary(ChallengeRecord challengeRecord) {
        if (challengeRecord == null || !challengeRecord.isCertified()) {
            return new GroupChallengeRecordFeedResponse.ReactionSummaryResponse(0, List.of());
        }

        List<Reaction> reactions = reactionService.getReactionsForChallengeRecord(challengeRecord.getId());
        Map<Long, UserProfileSummary> profiles = userService.getProfileSummariesByIds(
                reactions.stream()
                        .map(Reaction::getUserId)
                        .collect(Collectors.toSet())
        );
        List<GroupChallengeRecordFeedResponse.ReactionResponse> summary = reactions.stream()
                .map(reaction -> toReactionResponse(reaction, profiles.get(reaction.getUserId())))
                .toList();

        return new GroupChallengeRecordFeedResponse.ReactionSummaryResponse(summary.size(), summary);
    }

    private GroupChallengeRecordFeedResponse.ReactionResponse toReactionResponse(
            Reaction reaction,
            UserProfileSummary profile
    ) {
        return new GroupChallengeRecordFeedResponse.ReactionResponse(
                reaction.getBody().name(),
                reaction.getUserId(),
                profile == null ? null : profile.displayName(),
                profile == null ? null : profile.profileImageUrl(),
                profile != null && profile.userWithdrawn()
        );
    }

    private boolean includedInGroupResult(
            GroupActivityDayContext activityDay,
            GroupActivityParticipant participant
    ) {
        return (activityDay.dayStatus() == CalendarDayStatus.CONFIRMED
                || activityDay.dayStatus() == CalendarDayStatus.IN_PROGRESS)
                && participant != null
                && verificationPolicy.includedInGroupResult(
                        participant,
                        activityDay.activity().goals(),
                        activityDay.date()
                );
    }

    private Map<Long, ActivityRecord> activityRecordById(List<ChallengeRecord> challengeRecords) {
        List<Long> activityRecordIds = challengeRecords.stream()
                .filter(ChallengeRecord::isCertified)
                .map(ChallengeRecord::getActivityRecordId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (activityRecordIds.isEmpty()) {
            return Map.of();
        }

        return activityRecordRepository.findAllById(activityRecordIds).stream()
                .collect(Collectors.toMap(ActivityRecord::getId, Function.identity()));
    }

    private Map<Long, ChallengeRecord> challengeRecordByParticipantId(List<ChallengeRecord> challengeRecords) {
        return challengeRecords.stream()
                .collect(Collectors.toMap(
                        ChallengeRecord::getGroupChallengeParticipantId,
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    private Map<Long, GroupActivityParticipant> participantById(List<GroupActivityParticipant> participants) {
        return participants.stream()
                .collect(Collectors.toMap(GroupActivityParticipant::participantId, Function.identity()));
    }

    private Map<Long, ChallengeRecordStatusCount> statusCountByRecordId(List<ChallengeRecord> challengeRecords) {
        List<Long> challengeRecordIds = challengeRecords.stream()
                .map(ChallengeRecord::getId)
                .filter(Objects::nonNull)
                .toList();

        if (challengeRecordIds.isEmpty()) {
            return Map.of();
        }

        return statusCountRepository.findAllByChallengeRecordIdIn(challengeRecordIds).stream()
                .collect(Collectors.toMap(ChallengeRecordStatusCount::getChallengeRecordId, Function.identity()));
    }

    private Set<PokeKey> pokedRecords(List<ChallengeRecord> challengeRecords, Long currentUserId) {
        List<Long> beforeRecordIds = challengeRecords.stream()
                .filter(ChallengeRecord::isBeforeRecord)
                .map(ChallengeRecord::getId)
                .toList();

        return pokeService.getPokesByChallengeRecordsAndSender(beforeRecordIds, currentUserId).stream()
                .map(PokeKey::from)
                .collect(Collectors.toSet());
    }

    private boolean isPoked(
            ChallengeRecord challengeRecord,
            Long receiverUserId,
            GroupActivityDayContext activityDay
    ) {
        return challengeRecord != null
                && challengeRecord.isBeforeRecord()
                && receiverUserId != null
                && activityDay.pokedRecords().contains(new PokeKey(challengeRecord.getId(), receiverUserId));
    }

    private GroupChallengeRecordFeedResponse.ActivityRecordResponse toActivityRecordResponse(
            ChallengeRecord challengeRecord,
            Map<Long, ActivityRecord> activityRecordById
    ) {
        if (challengeRecord == null || !challengeRecord.isCertified()) {
            return null;
        }

        ActivityRecord activityRecord = activityRecordById.get(challengeRecord.getActivityRecordId());
        if (activityRecord == null) {
            throw new CustomException(FeedErrorCode.FEED_ACTIVITY_RECORD_NOT_FOUND);
        }

        List<ActivityRecordDetailHistoryResponse> details = activityRecord.getDetails().stream()
                .map(this::toActivityRecordDetail)
                .toList();

        return new GroupChallengeRecordFeedResponse.ActivityRecordResponse(
                activityRecord.getCreatedAt(),
                imageReadUrlBuilder.build(activityRecord.getActivityImageObjectKey()),
                activityRecord.getReflectionText(),
                details.stream().allMatch(ActivityRecordDetailHistoryResponse::isAchieved),
                details
        );
    }

    private ActivityRecordDetailHistoryResponse toActivityRecordDetail(ActivityRecordDetail detail) {
        return new ActivityRecordDetailHistoryResponse(
                detail.getUsageGoalType().getCode(),
                detail.getUseMinutes(),
                detail.getUserUsageGoalTime().getGoalMinutes(),
                detail.isAchieved()
        );
    }

    private List<MemberDailyGoalResponse> toGoalResponses(List<MemberDailyGoal> goals) {
        return goals.stream()
                .map(goal -> new MemberDailyGoalResponse(
                        goal.id(),
                        goal.usageGoalType(),
                        goal.goalMinutes(),
                        goal.effectiveDate()
                ))
                .toList();
    }

    private MemberDailyStatus dailyStatus(boolean includedInGroupResult, ChallengeRecord challengeRecord) {
        if (!includedInGroupResult) {
            return MemberDailyStatus.NOT_ACTIVE;
        }
        if (challengeRecord == null || !challengeRecord.isCertified()) {
            return MemberDailyStatus.NOT_CERTIFIED;
        }
        if (challengeRecord.isCertificationSucceeded()) {
            return MemberDailyStatus.GOAL_ACHIEVED;
        }
        return MemberDailyStatus.GOAL_FAILED;
    }

    private boolean isActiveParticipant(GroupActivityParticipant participant) {
        return participant != null
                && ACTIVE_MEMBER_STATUS.equals(participant.memberStatus())
                && JOINED_PARTICIPANT_STATUS.equals(participant.participantStatus());
    }

    private LocalDateTime activitySubmittedAt(GroupChallengeRecordFeedResponse.MemberResponse member) {
        if (member.activityRecord() == null) {
            return null;
        }

        return member.activityRecord().submittedAt();
    }

    private CalendarDayStatus dayStatus(LocalDate date, LocalDate today, LocalDate firstVerificationDate) {
        if (date.isAfter(today)) {
            return CalendarDayStatus.FUTURE;
        }
        if (firstVerificationDate == null || date.isBefore(firstVerificationDate)) {
            return CalendarDayStatus.NOT_STARTED;
        }
        if (date.isEqual(today)) {
            return CalendarDayStatus.IN_PROGRESS;
        }
        return CalendarDayStatus.CONFIRMED;
    }

    private int reactionCount(ChallengeRecordStatusCount statusCount) {
        return statusCount == null ? 0 : statusCount.getReactionCount();
    }

    private int commentCount(ChallengeRecord challengeRecord, ChallengeRecordStatusCount statusCount) {
        if (statusCount == null) {
            return 0;
        }

        if (challengeRecord != null && challengeRecord.isBeforeRecord()) {
            return statusCount.getBeforeCommentCount();
        }

        return statusCount.getAfterCommentCount();
    }

    private int pokeCount(ChallengeRecordStatusCount statusCount) {
        return statusCount == null ? 0 : statusCount.getPokeCount();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(KST));
    }

    private record GroupActivityContext(
            GroupChallenge challenge,
            List<GroupActivityParticipantRow> participantRows,
            List<GroupActivityParticipant> participants,
            List<MemberDailyGoal> goals
    ) {
    }

    private record GroupActivityDayContext(
            GroupActivityContext activity,
            LocalDate date,
            Long currentUserId,
            CalendarDayStatus dayStatus,
            Set<Long> certifiedParticipantIds,
            Map<Long, ChallengeRecord> challengeRecordByParticipantId,
            Map<Long, ActivityRecord> activityRecordById,
            Map<Long, ChallengeRecordStatusCount> statusCountByRecordId,
            Set<PokeKey> pokedRecords,
            Map<Long, GroupActivityParticipant> participantById,
            boolean todayFeed
    ) {
    }

    private record PokeKey(Long challengeRecordId, Long receiverUserId) {

        private static PokeKey from(Poke poke) {
            return new PokeKey(poke.getChallengeRecordId(), poke.getReceiverUserId());
        }
    }
}

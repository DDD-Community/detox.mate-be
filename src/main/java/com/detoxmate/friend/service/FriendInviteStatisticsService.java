package com.detoxmate.friend.service;

import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.friend.dto.FriendInviteeResponse;
import com.detoxmate.friend.dto.FriendUserResponse;
import com.detoxmate.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class FriendInviteStatisticsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ChallengeRecordRepository challengeRecordRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FriendInviteeResponse enrich(User invitee, FriendUserResponse base) {
        LocalDate today = LocalDate.now(clock.withZone(KST));
        long daysSinceStart = ChronoUnit.DAYS.between(invitee.getCreatedAt().toLocalDate(), today) + 1;
        long targetSuccessCount = challengeRecordRepository.countSuccessfulRecordsByUserId(invitee.getId());

        return new FriendInviteeResponse(
                base.userId(),
                base.displayName(),
                base.profileImageUrl(),
                base.relationshipStatus(),
                base.requestId(),
                daysSinceStart,
                targetSuccessCount
        );
    }
}

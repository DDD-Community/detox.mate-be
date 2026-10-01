package com.detoxmate.friend.service;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.friend.dto.FriendInviteeResponse;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.friend.dto.FriendUserResponse;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FriendInviteStatisticsServiceTest {

    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 9, 30);

    @Autowired
    private ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupChallengeParticipantRepository participantRepository;

    @Test
    @DisplayName("가입 당일은 1일차이며 성공 기록이 없으면 0회다")
    void enrich_returnsDayOneAndZeroSuccessesOnSignupDate() {
        User user = userRepository.saveAndFlush(User.createNew("초대한 친구"));
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 10, 1, 0, 0));
        FriendUserResponse base = new FriendUserResponse(
                user.getId(), "초대한 친구", "https://example.com/profile.png",
                FriendRelationshipStatus.PENDING_RECEIVED, 17L
        );

        FriendInviteeResponse response = serviceAt("2026-10-01T03:00:00Z").enrich(user, base);

        assertThat(response).isEqualTo(new FriendInviteeResponse(
                user.getId(), base.displayName(), base.profileImageUrl(), base.relationshipStatus(), base.requestId(),
                1L, 0L
        ));
    }

    @Test
    @DisplayName("UTC 시계여도 한국 자정을 기준으로 가입일을 포함한 일차가 증가한다")
    void enrich_countsCalendarDaysAtKoreanMidnight() {
        User user = userRepository.saveAndFlush(User.createNew("초대한 친구"));
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 9, 30, 23, 59));
        FriendUserResponse base = baseFor(user);

        assertThat(serviceAt("2026-09-30T14:59:59Z").enrich(user, base).daysSinceStart()).isEqualTo(1);
        assertThat(serviceAt("2026-09-30T15:00:00Z").enrich(user, base).daysSinceStart()).isEqualTo(2);
    }

    @Test
    @DisplayName("모든 그룹의 성공 기록을 세며 같은 날의 별도 기록과 탈퇴 전 기록도 포함한다")
    void enrich_countsAllSuccessfulRecordsAcrossHistoricalParticipations() {
        User user = userRepository.saveAndFlush(User.createNew("초대한 친구"));
        User other = userRepository.saveAndFlush(User.createNew("다른 사용자"));
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 9, 28, 12, 0));
        GroupMember currentMembership = groupMemberRepository.save(GroupMember.createMember(user.getId(), 10L));
        GroupMember formerMembership = groupMemberRepository.save(GroupMember.createMember(user.getId(), 20L));
        GroupMember otherMembership = groupMemberRepository.save(GroupMember.createMember(other.getId(), 10L));
        GroupChallengeParticipant current = participantRepository.save(GroupChallengeParticipant.join(currentMembership.getId(), 100L));
        GroupChallengeParticipant former = participantRepository.save(GroupChallengeParticipant.join(formerMembership.getId(), 200L));
        GroupChallengeParticipant unrelated = participantRepository.save(GroupChallengeParticipant.join(otherMembership.getId(), 100L));

        saveCertified(current, RECORD_DATE, 1L, ChallengeRecordCertificationResult.SUCCESS);
        saveCertified(former, RECORD_DATE, 2L, ChallengeRecordCertificationResult.SUCCESS);
        saveCertified(former, RECORD_DATE.minusDays(1), 3L, ChallengeRecordCertificationResult.SUCCESS);
        saveCertified(current, RECORD_DATE.minusDays(1), 4L, ChallengeRecordCertificationResult.FAIL);
        challengeRecordRepository.save(ChallengeRecord.create(current.getGroupChallengeId(), current.getId(), RECORD_DATE.minusDays(2)));
        saveCertified(unrelated, RECORD_DATE, 5L, ChallengeRecordCertificationResult.SUCCESS);
        former.withdraw();
        formerMembership.leave();

        FriendInviteeResponse response = serviceAt("2026-10-01T03:00:00Z").enrich(user, baseFor(user));

        assertThat(response.daysSinceStart()).isEqualTo(4);
        assertThat(response.targetSuccessCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("성공 기록이 실패로 정정되면 성공 횟수에서도 제외된다")
    void enrich_usesCurrentCertificationResults() {
        User user = userRepository.saveAndFlush(User.createNew("초대한 친구"));
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 9, 30, 12, 0));
        GroupMember member = groupMemberRepository.save(GroupMember.createMember(user.getId(), 10L));
        GroupChallengeParticipant participant = participantRepository.save(GroupChallengeParticipant.join(member.getId(), 100L));
        ChallengeRecord record = saveCertified(participant, RECORD_DATE, 1L, ChallengeRecordCertificationResult.SUCCESS);
        FriendInviteStatisticsService service = serviceAt("2026-10-01T03:00:00Z");

        assertThat(service.enrich(user, baseFor(user)).targetSuccessCount()).isEqualTo(1);
        record.correctCertificationResult(ChallengeRecordCertificationResult.FAIL);
        assertThat(service.enrich(user, baseFor(user)).targetSuccessCount()).isZero();
    }

    private FriendInviteStatisticsService serviceAt(String instant) {
        return new FriendInviteStatisticsService(
                challengeRecordRepository, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
        );
    }

    private FriendUserResponse baseFor(User user) {
        return new FriendUserResponse(user.getId(), user.getDisplayName(), null, FriendRelationshipStatus.NONE, null);
    }

    private ChallengeRecord saveCertified(GroupChallengeParticipant participant, LocalDate date,
                                          Long activityId, ChallengeRecordCertificationResult result) {
        ChallengeRecord record = ChallengeRecord.create(participant.getGroupChallengeId(), participant.getId(), date);
        record.certify(activityId, participant.getId(), result);
        return challengeRecordRepository.save(record);
    }
}

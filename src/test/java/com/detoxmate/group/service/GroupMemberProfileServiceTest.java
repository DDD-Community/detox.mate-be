package com.detoxmate.group.service;

import com.detoxmate.activityrecord.domain.UsageGoalType;
import com.detoxmate.activityrecord.domain.UserUsageGoalTime;
import com.detoxmate.activityrecord.dto.UsageGoalTypeCode;
import com.detoxmate.activityrecord.repository.UsageGoalTypeRepository;
import com.detoxmate.activityrecord.repository.UserUsageGoalTimeRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupMemberProfileResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupMemberProfileServiceTest {

    @Autowired
    GroupMemberProfileService groupMemberProfileService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    GroupMemberRepository groupMemberRepository;

    @Autowired
    GroupChallengeRepository groupChallengeRepository;

    @Autowired
    GroupChallengeParticipantRepository participantRepository;

    @Autowired
    UsageGoalTypeRepository usageGoalTypeRepository;

    @Autowired
    UserUsageGoalTimeRepository userUsageGoalTimeRepository;

    @Autowired
    ChallengeRecordRepository challengeRecordRepository;

    @Test
    @DisplayName("활성 그룹원은 회원 탈퇴한 과거 그룹 멤버 프로필을 익명화된 상태로 조회한다")
    void getGroupMemberProfile_withWithdrawnPastMember_returnsAnonymizedProfile() {
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준", "profile-images/2/profile.png"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupMember targetGroupMember = saveParticipant(group.getId(), challenge.getId(), targetUser);

        targetGroupMember.leave();
        participantRepository.findByGroupChallengeIdAndGroupMemberId(challenge.getId(), targetGroupMember.getId())
                .orElseThrow()
                .withdraw();
        targetUser.withdraw();

        GroupMemberProfileResponse response = groupMemberProfileService.getGroupMemberProfile(
                group.getId(),
                targetGroupMember.getId(),
                currentUser.getId()
        );

        assertThat(response.id()).isEqualTo(targetGroupMember.getId());
        assertThat(response.userId()).isEqualTo(targetUser.getId());
        assertThat(response.displayName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(response.profileImageUrl()).isNull();
        assertThat(response.status()).isEqualTo("LEFT");
        assertThat(response.userWithdrawn()).isTrue();
    }

    @Test
    @DisplayName("그룹 멤버 프로필은 현재 목표와 목표 달성 통계를 반환한다")
    void getGroupMemberProfile_returnsCurrentGoalsAndStats() {
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));
        UsageGoalType totalUsage = usageGoalTypeRepository.save(UsageGoalType.create(1L, UsageGoalTypeCode.TOTAL_USAGE));

        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupMember targetGroupMember = saveParticipant(group.getId(), challenge.getId(), targetUser);
        GroupChallengeParticipant targetParticipant = participantRepository
                .findByGroupChallengeIdAndGroupMemberId(challenge.getId(), targetGroupMember.getId())
                .orElseThrow();
        UserUsageGoalTime goalTime = userUsageGoalTimeRepository.save(
                UserUsageGoalTime.create(targetUser, totalUsage, 60)
        );
        ChallengeRecord challengeRecord = ChallengeRecord.create(
                challenge.getId(),
                targetParticipant.getId(),
                today()
        );
        challengeRecord.certify(999L, targetParticipant.getId(), ChallengeRecordCertificationResult.SUCCESS);
        challengeRecordRepository.save(challengeRecord);

        GroupMemberProfileResponse response = groupMemberProfileService.getGroupMemberProfile(
                group.getId(),
                targetGroupMember.getId(),
                currentUser.getId()
        );

        assertThat(response.currentGoals())
                .extracting("id", "usageGoalType", "goalMinutes")
                .containsExactly(tuple(goalTime.getId(), UsageGoalTypeCode.TOTAL_USAGE, 60));
        assertThat(response.stats().overall().totalDays()).isEqualTo(1);
        assertThat(response.stats().overall().achievedDays()).isEqualTo(1);
        assertThat(response.stats().overall().achievementRate()).isEqualTo(100);
        assertThat(response.stats().recent7Days().submittedDays()).isEqualTo(1);
        assertThat(response.stats().recent7Days().achievedDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("활성 그룹원이 아니면 과거 그룹 멤버 프로필도 조회할 수 없다")
    void getGroupMemberProfile_withoutActiveRequester_throwsForbidden() {
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        User requester = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준"));
        GroupMember targetGroupMember = groupMemberRepository.save(GroupMember.createMember(targetUser.getId(), group.getId()));

        assertThatThrownBy(() -> groupMemberProfileService.getGroupMemberProfile(
                group.getId(),
                targetGroupMember.getId(),
                requester.getId()
        ))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    private GroupMember saveParticipant(Long groupId, Long challengeId, User user) {
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), groupId));
        participantRepository.save(GroupChallengeParticipant.join(groupMember.getId(), challengeId));
        return groupMember;
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
}

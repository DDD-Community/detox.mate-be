package com.detoxmate.group.repository;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class GroupChallengeParticipantRepositoryTest {

    private static final AtomicInteger INVITE_CODE_SEQUENCE = new AtomicInteger();

    @Autowired
    private GroupChallengeParticipantRepository participantRepository;

    @Autowired
    private GroupChallengeRepository groupChallengeRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("참여자가 활성 그룹멤버와 현재 사용자를 가리키면 true를 반환한다")
    void existsActiveByIdAndUserId_returnsTrueForActiveParticipantOfUser() {
        User user = userRepository.save(User.createNew("지민"));
        Group group = saveGroup();
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), group.getId()));
        GroupChallenge groupChallenge = saveActiveChallenge(group.getId(), 1);
        GroupChallengeParticipant participant = participantRepository.save(
                GroupChallengeParticipant.join(groupMember.getId(), groupChallenge.getId())
        );

        boolean exists = participantRepository.existsActiveByIdAndUserId(participant.getId(), user.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("참여자가 다른 사용자를 가리키면 false를 반환한다")
    void existsActiveByIdAndUserId_returnsFalseForOtherUsersParticipant() {
        User owner = userRepository.save(User.createNew("지민"));
        User other = userRepository.save(User.createNew("민수"));
        Group group = saveGroup();
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(other.getId(), group.getId()));
        GroupChallenge groupChallenge = saveActiveChallenge(group.getId(), 1);
        GroupChallengeParticipant participant = participantRepository.save(
                GroupChallengeParticipant.join(groupMember.getId(), groupChallenge.getId())
        );

        boolean exists = participantRepository.existsActiveByIdAndUserId(participant.getId(), owner.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("참여자나 그룹멤버가 활성 상태가 아니면 false를 반환한다")
    void existsActiveByIdAndUserId_returnsFalseForInactiveParticipantOrMember() {
        User withdrawnUser = userRepository.save(User.createNew("지민"));
        Group group = saveGroup();
        GroupMember activeMember = groupMemberRepository.save(GroupMember.createMember(withdrawnUser.getId(), group.getId()));
        GroupChallenge groupChallenge = saveActiveChallenge(group.getId(), 1);
        GroupChallengeParticipant withdrawnParticipant = GroupChallengeParticipant.join(
                activeMember.getId(),
                groupChallenge.getId()
        );
        withdrawnParticipant.withdraw();
        participantRepository.save(withdrawnParticipant);

        User leftUser = userRepository.save(User.createNew("민수"));
        GroupMember leftMember = GroupMember.createMember(leftUser.getId(), group.getId());
        leftMember.leave();
        GroupMember savedLeftMember = groupMemberRepository.save(leftMember);
        GroupChallengeParticipant activeParticipant = participantRepository.save(
                GroupChallengeParticipant.join(savedLeftMember.getId(), groupChallenge.getId())
        );

        assertThat(participantRepository.existsActiveByIdAndUserId(withdrawnParticipant.getId(), withdrawnUser.getId()))
                .isFalse();
        assertThat(participantRepository.existsActiveByIdAndUserId(activeParticipant.getId(), leftUser.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("최신 챌린지가 ACTIVE가 아니면 인증 가능한 참여자로 보지 않는다")
    void existsActiveByIdAndUserId_returnsFalseWhenChallengeIsNotActive() {
        User user = userRepository.save(User.createNew("지민"));
        Group group = saveGroup();
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), group.getId()));
        GroupChallenge canceledChallenge = saveCanceledChallenge(group.getId(), 1);
        GroupChallengeParticipant participant = participantRepository.save(
                GroupChallengeParticipant.join(groupMember.getId(), canceledChallenge.getId())
        );

        boolean exists = participantRepository.existsActiveByIdAndUserId(participant.getId(), user.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("최신 챌린지가 아니면 인증 가능한 참여자로 보지 않는다")
    void existsActiveByIdAndUserId_returnsFalseWhenChallengeIsNotLatest() {
        User user = userRepository.save(User.createNew("지민"));
        Group group = saveGroup();
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), group.getId()));
        GroupChallenge oldChallenge = saveActiveChallenge(group.getId(), 1);
        saveActiveChallenge(group.getId(), 2);
        GroupChallengeParticipant participant = participantRepository.save(
                GroupChallengeParticipant.join(groupMember.getId(), oldChallenge.getId())
        );

        boolean exists = participantRepository.existsActiveByIdAndUserId(participant.getId(), user.getId());

        assertThat(exists).isFalse();
    }

    private Group saveGroup() {
        String inviteCode = "A%04d".formatted(INVITE_CODE_SEQUENCE.incrementAndGet());
        return groupRepository.save(Group.createNew("테스트그룹", inviteCode));
    }

    private GroupChallenge saveActiveChallenge(Long groupId, int challengeNo) {
        GroupChallenge challenge = GroupChallenge.createFirst(groupId);
        ReflectionTestUtils.setField(challenge, "challengeNo", challengeNo);
        challenge.activate(LocalDateTime.of(2026, 5, 1, 0, 0));
        return groupChallengeRepository.save(challenge);
    }

    private GroupChallenge saveCanceledChallenge(Long groupId, int challengeNo) {
        GroupChallenge challenge = GroupChallenge.createFirst(groupId);
        ReflectionTestUtils.setField(challenge, "challengeNo", challengeNo);
        challenge.cancel();
        return groupChallengeRepository.save(challenge);
    }
}

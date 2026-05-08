package com.detoxmate.group.repository;

import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class GroupChallengeParticipantRepositoryTest {

    private static final Long GROUP_ID = 1L;
    private static final Long GROUP_CHALLENGE_ID = 10L;

    @Autowired
    private GroupChallengeParticipantRepository participantRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("참여자가 활성 그룹멤버와 현재 사용자를 가리키면 true를 반환한다")
    void existsActiveByIdAndUserId_returnsTrueForActiveParticipantOfUser() {
        User user = userRepository.save(User.createNew("지민"));
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), GROUP_ID));
        GroupChallengeParticipant participant = participantRepository.save(
                GroupChallengeParticipant.join(groupMember.getId(), GROUP_CHALLENGE_ID)
        );

        boolean exists = participantRepository.existsActiveByIdAndUserId(participant.getId(), user.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("참여자가 다른 사용자를 가리키면 false를 반환한다")
    void existsActiveByIdAndUserId_returnsFalseForOtherUsersParticipant() {
        User owner = userRepository.save(User.createNew("지민"));
        User other = userRepository.save(User.createNew("민수"));
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(other.getId(), GROUP_ID));
        GroupChallengeParticipant participant = participantRepository.save(
                GroupChallengeParticipant.join(groupMember.getId(), GROUP_CHALLENGE_ID)
        );

        boolean exists = participantRepository.existsActiveByIdAndUserId(participant.getId(), owner.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("참여자나 그룹멤버가 활성 상태가 아니면 false를 반환한다")
    void existsActiveByIdAndUserId_returnsFalseForInactiveParticipantOrMember() {
        User withdrawnUser = userRepository.save(User.createNew("지민"));
        GroupMember activeMember = groupMemberRepository.save(GroupMember.createMember(withdrawnUser.getId(), GROUP_ID));
        GroupChallengeParticipant withdrawnParticipant = GroupChallengeParticipant.join(
                activeMember.getId(),
                GROUP_CHALLENGE_ID
        );
        withdrawnParticipant.withdraw();
        participantRepository.save(withdrawnParticipant);

        User leftUser = userRepository.save(User.createNew("민수"));
        GroupMember leftMember = GroupMember.createMember(leftUser.getId(), GROUP_ID);
        leftMember.leave();
        GroupMember savedLeftMember = groupMemberRepository.save(leftMember);
        GroupChallengeParticipant activeParticipant = participantRepository.save(
                GroupChallengeParticipant.join(savedLeftMember.getId(), GROUP_CHALLENGE_ID)
        );

        assertThat(participantRepository.existsActiveByIdAndUserId(withdrawnParticipant.getId(), withdrawnUser.getId()))
                .isFalse();
        assertThat(participantRepository.existsActiveByIdAndUserId(activeParticipant.getId(), leftUser.getId()))
                .isFalse();
    }
}

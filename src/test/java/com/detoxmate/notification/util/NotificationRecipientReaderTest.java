package com.detoxmate.notification.util;

import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.feed.FeedErrorCode;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.notification.dto.ChallengeRecordNotificationRow;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRecipientReaderTest {

    @Autowired
    private ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupChallengeParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    private NotificationRecipientReader reader;

    @BeforeEach
    void setUp() {
        reader = new NotificationRecipientReader(
                challengeRecordRepository,
                groupMemberRepository,
                participantRepository
        );
    }

    @Test
    @DisplayName("groupId로 활성 그룹 멤버 userId 목록을 조회한다")
    void findActiveGroupMemberUserIds_returnsActiveMembersOnly() {
        // given
        Group group = groupRepository.save(Group.createNew("알림방", "A1001"));
        Group otherGroup = groupRepository.save(Group.createNew("다른방", "A1002"));
        User activeUser = userRepository.save(User.createNew("활성"));
        User leftUser = userRepository.save(User.createNew("탈퇴"));
        User otherGroupUser = userRepository.save(User.createNew("다른그룹"));
        groupMemberRepository.save(GroupMember.createMember(activeUser.getId(), group.getId()));
        GroupMember leftMember = groupMemberRepository.save(GroupMember.createMember(leftUser.getId(), group.getId()));
        leftMember.leave();
        groupMemberRepository.save(GroupMember.createMember(otherGroupUser.getId(), otherGroup.getId()));

        // when
        List<Long> userIds = reader.findActiveGroupMemberUserIds(group.getId());

        // then
        assertThat(userIds).containsExactly(activeUser.getId());
    }

    @Test
    @DisplayName("groupChallengeId로 활성 챌린지 참여자 userId 목록을 조회한다")
    void findGroupChallengeParticipantUserIds_returnsJoinedActiveMembersOnly() {
        // given
        Group group = groupRepository.save(Group.createNew("챌린지방", "A1003"));
        Long groupChallengeId = 100L;
        User joinedUser = userRepository.save(User.createNew("참여자"));
        User withdrawnUser = userRepository.save(User.createNew("철회자"));
        User leftUser = userRepository.save(User.createNew("탈퇴자"));

        GroupMember joinedMember = groupMemberRepository.save(GroupMember.createMember(joinedUser.getId(), group.getId()));
        GroupMember withdrawnMember = groupMemberRepository.save(GroupMember.createMember(withdrawnUser.getId(), group.getId()));
        GroupMember leftMember = groupMemberRepository.save(GroupMember.createMember(leftUser.getId(), group.getId()));
        leftMember.leave();

        participantRepository.save(GroupChallengeParticipant.join(joinedMember.getId(), groupChallengeId));
        GroupChallengeParticipant withdrawnParticipant =
                participantRepository.save(GroupChallengeParticipant.join(withdrawnMember.getId(), groupChallengeId));
        withdrawnParticipant.withdraw();
        participantRepository.save(GroupChallengeParticipant.join(leftMember.getId(), groupChallengeId));

        // when
        List<Long> userIds = reader.findGroupChallengeParticipantUserIds(groupChallengeId);

        // then
        assertThat(userIds).containsExactly(joinedUser.getId());
    }

    @Test
    @DisplayName("challengeRecordId로 알림에 필요한 게시물 작성자 정보를 조회한다")
    void findChallengeRecordInfo_returnsRecordAuthorInfo() {
        // given
        Group group = groupRepository.save(Group.createNew("피드방", "A1004"));
        Long groupChallengeId = 200L;
        User author = userRepository.save(User.createNew("작성자"));
        GroupMember authorMember = groupMemberRepository.save(GroupMember.createMember(author.getId(), group.getId()));
        GroupChallengeParticipant participant =
                participantRepository.save(GroupChallengeParticipant.join(authorMember.getId(), groupChallengeId));
        ChallengeRecord record = challengeRecordRepository.save(
                ChallengeRecord.create(groupChallengeId, participant.getId(), LocalDate.of(2026, 5, 12))
        );

        // when
        ChallengeRecordNotificationRow row = reader.findChallengeRecordInfo(record.getId());

        // then
        assertThat(row.challengeRecordId()).isEqualTo(record.getId());
        assertThat(row.groupChallengeId()).isEqualTo(groupChallengeId);
        assertThat(row.authorUserId()).isEqualTo(author.getId());
        assertThat(row.authorNickname()).isEqualTo("작성자");
    }

    @Test
    @DisplayName("challengeRecordId에 해당하는 게시물이 없으면 FEED_NOT_FOUND 예외를 던진다")
    void findChallengeRecordInfo_throwsWhenRecordDoesNotExist() {
        // when & then
        assertThatThrownBy(() -> reader.findChallengeRecordInfo(999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(FeedErrorCode.FEED_NOT_FOUND);
    }
}

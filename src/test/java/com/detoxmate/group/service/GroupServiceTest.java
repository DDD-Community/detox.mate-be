package com.detoxmate.group.service;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupServiceTest {

    // DB 경계 — mock
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
    private final GroupChallengeRepository groupChallengeRepository = mock(GroupChallengeRepository.class);
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository = mock(GroupChallengeParticipantRepository.class);

    // 내부 그룹 서비스 — 실객체
    private final GroupMemberService groupMemberService = new GroupMemberService(groupMemberRepository);
    private final GroupChallengeService groupChallengeService = new GroupChallengeService(groupChallengeRepository);
    private final GroupChallengeParticipantService groupChallengeParticipantService = new GroupChallengeParticipantService(groupChallengeParticipantRepository);

    private final GroupService groupService = new GroupService(
            groupRepository, groupMemberService, groupChallengeService, groupChallengeParticipantService
    );

    @BeforeEach
    void setUp() {
        when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(i -> i.getArgument(0));
        when(groupChallengeRepository.save(any(GroupChallenge.class))).thenAnswer(i -> i.getArgument(0));
        when(groupChallengeParticipantRepository.save(any(GroupChallengeParticipant.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void 그룹_생성_시_생성자가_OWNER_멤버로_등록된다() {
        GroupResponse response = groupService.createGroup(1L, "테스트그룹");

        assertThat(response.myRole()).isEqualTo("OWNER");
    }

    @Test
    void 그룹_생성_시_그룹_이름이_반환된다() {
        GroupResponse response = groupService.createGroup(1L, "나의그룹");

        assertThat(response.name()).isEqualTo("나의그룹");
    }

    @Test
    @Disabled("GroupService.join is not implemented yet")
    void 모집중인_그룹에_유효한_초대코드로_참여하면_그룹_멤버와_챌린지_참가자가_추가된다() {
    }
}

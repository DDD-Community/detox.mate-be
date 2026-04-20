package com.detoxmate.group.service;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.repository.GroupRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupServiceTest {

    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final GroupMemberService groupMemberService = mock(GroupMemberService.class);
    private final GroupChallengeService groupChallengeService = mock(GroupChallengeService.class);
    private final GroupChallengeParticipantService groupChallengeParticipantService = mock(GroupChallengeParticipantService.class);

    private final GroupService groupService = new GroupService(
            groupRepository, groupMemberService, groupChallengeService, groupChallengeParticipantService
    );

    @Test
    void 유효한_이름으로_그룹을_생성하면_생성된_그룹을_반환한다() {
        String groupName = "테스트1";

        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Group group = groupService.saveGroup(groupName);

        assertThat(group.getName()).isEqualTo(groupName);
    }
}

package com.detoxmate.group.service;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.repository.GroupRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupServiceTest {

    @Test
    void 유효한_이름으로_그룹을_생성하면_생성된_그룹을_반환한다() {
        Long creatorUserId = 1L;
        String groupName = "테스트1";

        GroupRepository groupRepository = mock(GroupRepository.class);
        GroupService groupService = new GroupService(groupRepository);

        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Group group = groupService.saveGroup(groupName);

        assertThat(group.getName()).isEqualTo(groupName);
    }
}

package com.detoxmate.group.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupMemberTest {

    @Test
    void 그룹_생성자는_OWNER_역할로_멤버가_된다() {
        Long userId = 1L;
        Long groupId = 10L;

        GroupMember member = GroupMember.createOwner(userId, groupId);

        assertThat(member.getUserId()).isEqualTo(userId);
        assertThat(member.getGroupId()).isEqualTo(groupId);
        assertThat(member.getRole()).isEqualTo(GroupMemberRole.OWNER.name());
    }

    @Test
    void 초대로_참여한_멤버는_MEMBER_역할을_가진다() {
        Long userId = 2L;
        Long groupId = 10L;

        GroupMember member = GroupMember.createMember(userId, groupId);

        assertThat(member.getUserId()).isEqualTo(userId);
        assertThat(member.getGroupId()).isEqualTo(groupId);
        assertThat(member.getRole()).isEqualTo(GroupMemberRole.MEMBER.name());
    }

    @Test
    void 신규_멤버는_ACTIVE_상태로_생성된다() {
        Long userId = 1L;
        Long groupId = 10L;

        GroupMember owner = GroupMember.createOwner(userId, groupId);
        GroupMember member = GroupMember.createMember(userId, groupId);

        assertThat(owner.getStatus()).isEqualTo(GroupMemberStatus.ACTIVE.name());
        assertThat(member.getStatus()).isEqualTo(GroupMemberStatus.ACTIVE.name());
    }

    @Test
    void 멤버_생성_시_joinedAt이_설정된다() {
        Long userId = 1L;
        Long groupId = 10L;

        GroupMember member = GroupMember.createOwner(userId, groupId);

        assertThat(member.getJoinedAt()).isNotNull();
    }
}

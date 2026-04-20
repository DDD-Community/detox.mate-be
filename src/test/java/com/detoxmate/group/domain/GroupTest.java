package com.detoxmate.group.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GroupTest {

    @Test
    void 그룹이름으로_그룹을_생성할_수_있다() {
        String groupName = "groupName";

        Group group = Group.createNew(groupName);

        assertThat(group.getName()).isEqualTo(groupName);
    }

    @Test
    void 초대코드와_함께_그룹을_생성할_수_있다() {
        String groupName = "groupName";
        String inviteCode = "ABC12";

        Group group = Group.createNew(groupName, inviteCode);

        assertThat(group.getName()).isEqualTo(groupName);
        assertThat(group.getInviteCode()).isEqualTo(inviteCode);
    }
}

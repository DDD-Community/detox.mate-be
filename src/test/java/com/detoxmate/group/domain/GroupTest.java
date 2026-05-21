package com.detoxmate.group.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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

    @Test
    void 그룹_이름이_12자를_초과하면_그룹을_생성할_수_없다() {
        assertThatThrownBy(() -> Group.createNew("1234567890123", "ABC12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("그룹 이름은 12자 이하여야 합니다.");
    }
}

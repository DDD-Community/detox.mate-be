package com.detoxmate.notification.util;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class NotificationGroupReaderTest {

    @Autowired
    private GroupRepository groupRepository;

    private NotificationGroupReader reader;

    @BeforeEach
    void setUp() {
        reader = new NotificationGroupReader(groupRepository);
    }

    @Test
    @DisplayName("groupId로 그룹 이름을 조회한다")
    void findGroupName_returnsGroupName() {
        // given
        Group group = groupRepository.save(Group.createNew("알림방", "G1001"));

        // when
        String groupName = reader.findGroupName(group.getId());

        // then
        assertThat(groupName).isEqualTo("알림방");
    }

    @Test
    @DisplayName("groupId에 해당하는 그룹이 없으면 404 예외를 던진다")
    void findGroupName_throwsWhenGroupDoesNotExist() {
        // when & then
        assertThatThrownBy(() -> reader.findGroupName(999L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}

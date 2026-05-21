package com.detoxmate.notification.util;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.user.UserErrorCode;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class NotificationUserReaderTest {

    @Autowired
    private UserRepository userRepository;

    private NotificationUserReader reader;

    @BeforeEach
    void setUp() {
        reader = new NotificationUserReader(userRepository);
    }

    @Test
    @DisplayName("userId로 사용자 닉네임을 조회한다")
    void findDisplayName_returnsDisplayName() {
        // given
        User user = userRepository.save(User.createNew("슬빈"));

        // when
        String displayName = reader.findDisplayName(user.getId());

        // then
        assertThat(displayName).isEqualTo("슬빈");
    }

    @Test
    @DisplayName("여러 userId로 사용자 닉네임 맵을 조회한다")
    void findDisplayNames_returnsDisplayNameMap() {
        // given
        User user1 = userRepository.save(User.createNew("슬빈"));
        User user2 = userRepository.save(User.createNew("지민"));

        // when
        Map<Long, String> displayNames = reader.findDisplayNames(Set.of(user1.getId(), user2.getId()));

        // then
        assertThat(displayNames)
                .containsEntry(user1.getId(), "슬빈")
                .containsEntry(user2.getId(), "지민");
    }

    @Test
    @DisplayName("userId에 해당하는 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void findDisplayName_throwsWhenUserDoesNotExist() {
        // when & then
        assertThatThrownBy(() -> reader.findDisplayName(999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("여러 userId 중 누락된 사용자가 있으면 USER_NOT_FOUND 예외를 던진다")
    void findDisplayNames_throwsWhenAnyUserDoesNotExist() {
        // given
        User user = userRepository.save(User.createNew("슬빈"));

        // when & then
        assertThatThrownBy(() -> reader.findDisplayNames(Set.of(user.getId(), 999L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}

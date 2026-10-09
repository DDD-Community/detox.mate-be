package com.detoxmate.applock.domain;

import com.detoxmate.support.UserFixtures;
import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TimeLimitTest {

    private final User owner = UserFixtures.createUser("owner");

    @ParameterizedTest
    @ValueSource(ints = {0, 60, 1440})
    @DisplayName("사용자별 잠금 설정은 0분부터 1440분까지 저장할 수 있다")
    void create_acceptsMinutesWithinRange(int minutes) {
        TimeLimit timeLimit = TimeLimit.create(owner, minutes);

        assertThat(timeLimit.getUser()).isSameAs(owner);
        assertThat(timeLimit.getTotalLockMinutes()).isEqualTo(minutes);
    }

    @Test
    @DisplayName("사용자가 없으면 잠금 설정을 생성할 수 없다")
    void create_rejectsMissingOwner() {
        assertThatIllegalArgumentException().isThrownBy(() -> TimeLimit.create(null, 60));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 1441})
    @DisplayName("시간이 없거나 허용 범위 밖이면 잠금 설정을 생성할 수 없다")
    void create_rejectsInvalidMinutes(Integer minutes) {
        assertThatIllegalArgumentException().isThrownBy(() -> TimeLimit.create(owner, minutes));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1440})
    @DisplayName("설정 변경은 같은 사용자의 현재 시간을 교체하며 누적하지 않는다")
    void change_replacesCurrentMinutes(int minutes) {
        // given
        TimeLimit timeLimit = TimeLimit.create(owner, 60);

        // when
        timeLimit.changeTotalLockMinutes(minutes);
        timeLimit.changeTotalLockMinutes(minutes);

        // then
        assertThat(timeLimit.getTotalLockMinutes()).isEqualTo(minutes);
        assertThat(timeLimit.getUser()).isSameAs(owner);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 1441})
    @DisplayName("유효하지 않은 시간으로 변경하면 기존 시간과 사용자를 보존한다")
    void change_preservesCurrentStateWhenMinutesAreInvalid(Integer minutes) {
        // given
        TimeLimit timeLimit = TimeLimit.create(owner, 60);

        // when & then
        assertThatIllegalArgumentException().isThrownBy(() -> timeLimit.changeTotalLockMinutes(minutes));
        assertThat(timeLimit.getTotalLockMinutes()).isEqualTo(60);
        assertThat(timeLimit.getUser()).isSameAs(owner);
    }
}

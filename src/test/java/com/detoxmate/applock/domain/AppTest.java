package com.detoxmate.applock.domain;

import com.detoxmate.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AppTest {

    private static final String APP_DISPLAY_NAME = "Instagram";
    private static final int DAILY_LIMIT_MINUTES = 60;

    @Test
    @DisplayName("앱 잠금을 생성하면 사용자, 표시 이름, 소유된 제한 시간을 가진다")
    void create_returnsValidAggregate() {
        // given
        User user = User.createNew("tester");

        // when
        App app = App.create(user, APP_DISPLAY_NAME, DAILY_LIMIT_MINUTES);

        // then
        assertThat(app.getUser()).isSameAs(user);
        assertThat(app.getAppDisplayName()).isEqualTo(APP_DISPLAY_NAME);
        assertThat(app.getAppTimeLimit()).isNotNull();
        assertThat(app.getAppTimeLimit().getApp()).isSameAs(app);
        assertThat(app.getAppTimeLimit().getDailyLimitMinutes()).isEqualTo(DAILY_LIMIT_MINUTES);
    }

    @Test
    @DisplayName("사용자가 null이면 앱 잠금을 생성할 수 없다")
    void create_rejectsNullUser() {
        assertThatThrownBy(() -> App.create(null, APP_DISPLAY_NAME, DAILY_LIMIT_MINUTES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("앱 표시 이름이 null 또는 blank이면 앱 잠금을 생성할 수 없다")
    void create_rejectsNullOrBlankAppDisplayName(String appDisplayName) {
        // given
        User user = User.createNew("tester");

        // when & then
        assertThatThrownBy(() -> App.create(user, appDisplayName, DAILY_LIMIT_MINUTES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("앱 표시 이름이 100자를 초과하면 앱 잠금을 생성할 수 없다")
    void create_rejectsAppDisplayNameExceeding100Characters() {
        // given
        User user = User.createNew("tester");

        // when & then
        assertThatThrownBy(() -> App.create(user, "a".repeat(101), DAILY_LIMIT_MINUTES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 1441})
    @DisplayName("일일 제한 시간이 null 또는 허용 범위 밖이면 앱 잠금을 생성할 수 없다")
    void create_rejectsInvalidDailyLimitMinutes(Integer dailyLimitMinutes) {
        // given
        User user = User.createNew("tester");

        // when & then
        assertThatThrownBy(() -> App.create(user, APP_DISPLAY_NAME, dailyLimitMinutes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1440})
    @DisplayName("일일 제한 시간 경계값 0과 1440으로 앱 잠금을 생성할 수 있다")
    void create_acceptsDailyLimitMinuteBoundaries(int dailyLimitMinutes) {
        // given
        User user = User.createNew("tester");

        // when
        App app = App.create(user, APP_DISPLAY_NAME, dailyLimitMinutes);

        // then
        assertThat(app.getAppTimeLimit().getDailyLimitMinutes()).isEqualTo(dailyLimitMinutes);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1440})
    @DisplayName("일일 제한 시간 경계값 0과 1440으로 앱 잠금을 수정할 수 있다")
    void update_acceptsDailyLimitMinuteBoundaries(int dailyLimitMinutes) {
        // given
        App app = App.create(User.createNew("tester"), APP_DISPLAY_NAME, DAILY_LIMIT_MINUTES);
        AppTimeLimit ownedTimeLimit = app.getAppTimeLimit();

        // when
        app.update("YouTube", dailyLimitMinutes);

        // then
        assertThat(app.getAppDisplayName()).isEqualTo("YouTube");
        assertThat(app.getAppTimeLimit()).isSameAs(ownedTimeLimit);
        assertThat(app.getAppTimeLimit().getDailyLimitMinutes()).isEqualTo(dailyLimitMinutes);
    }

    @Test
    @DisplayName("앱 잠금을 수정하면 표시 이름과 기존 소유 제한 시간이 변경된다")
    void update_changesNameAndOwnedDailyLimit() {
        // given
        App app = App.create(User.createNew("tester"), APP_DISPLAY_NAME, DAILY_LIMIT_MINUTES);
        AppTimeLimit ownedTimeLimit = app.getAppTimeLimit();

        // when
        app.update("YouTube", 120);

        // then
        assertThat(app.getAppDisplayName()).isEqualTo("YouTube");
        assertThat(app.getAppTimeLimit()).isSameAs(ownedTimeLimit);
        assertThat(app.getAppTimeLimit().getDailyLimitMinutes()).isEqualTo(120);
    }

    @ParameterizedTest(name = "[{index}] appDisplayName={0}, dailyLimitMinutes={1}")
    @MethodSource("invalidUpdates")
    @DisplayName("유효하지 않은 수정은 기존 표시 이름과 제한 시간을 보존한다")
    void update_rejectsInvalidValuesAndPreservesPriorState(
            String appDisplayName,
            Integer dailyLimitMinutes
    ) {
        // given
        App app = App.create(User.createNew("tester"), APP_DISPLAY_NAME, DAILY_LIMIT_MINUTES);
        AppTimeLimit ownedTimeLimit = app.getAppTimeLimit();

        // when & then
        assertThatThrownBy(() -> app.update(appDisplayName, dailyLimitMinutes))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(app.getAppDisplayName()).isEqualTo(APP_DISPLAY_NAME);
        assertThat(app.getAppTimeLimit()).isSameAs(ownedTimeLimit);
        assertThat(app.getAppTimeLimit().getDailyLimitMinutes()).isEqualTo(DAILY_LIMIT_MINUTES);
    }

    private static Stream<Arguments> invalidUpdates() {
        return Stream.of(
                arguments(null, 120),
                arguments("", 120),
                arguments("   ", 120),
                arguments("a".repeat(101), 120),
                arguments("YouTube", null),
                arguments("YouTube", -1),
                arguments("YouTube", 1441)
        );
    }
}

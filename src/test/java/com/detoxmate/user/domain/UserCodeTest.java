package com.detoxmate.user.domain;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.user.UserCodeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCodeTest {

    @Test
    @DisplayName("허용 문자로 구성된 5자리 사용자 코드를 파싱한다")
    void parse_acceptsFiveCharacters() {
        // when
        UserCode code = UserCode.parse("ABC23");

        // then
        assertThat(code.value()).isEqualTo("ABC23");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc23", " \tabc23\n "})
    @DisplayName("소문자와 앞뒤 공백을 정규화한 5자리 사용자 코드를 반환한다")
    void parse_normalizesCaseAndSurroundingWhitespace(String input) {
        // when
        UserCode code = UserCode.parse(input);

        // then
        assertThat(code.value()).isEqualTo("ABC23");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "ABC2", "ABC234", "ABCDE23456", "ABCDE-23456",
            "AB-C23", "AB C23", "ABC2I", "ABC2O", "ABC20", "ABC21"
    })
    @DisplayName("5자리 형식에 맞지 않는 길이와 문자 및 하이픈을 거부한다")
    void parse_rejectsInvalidCode(String input) {
        // when & then
        assertThatThrownBy(() -> UserCode.parse(input))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserCodeException.INVALID_USER_CODE);
    }

    @Test
    @DisplayName("사용자 코드가 누락되면 입력 필수 오류를 반환한다")
    void parse_rejectsMissingCode() {
        // when & then
        assertThatThrownBy(() -> UserCode.parse(null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserCodeException.USER_CODE_REQUIRED);
    }

    @Test
    @DisplayName("5자리 대문자 사용자 코드를 값 객체로 생성한다")
    void constructor_preservesCanonicalFiveCharacterCode() {
        // when
        UserCode code = new UserCode("ABC23");

        // then
        assertThat(code.value()).isEqualTo("ABC23");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"abc23", " ABC23 ", "ABCDE23456"})
    @DisplayName("정규화되지 않았거나 5자리가 아닌 값으로 직접 생성할 수 없다")
    void constructor_rejectsNonCanonicalCode(String input) {
        // when & then
        assertThatThrownBy(() -> new UserCode(input))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserCodeException.INVALID_USER_CODE);
    }

    @Test
    @DisplayName("5자리 사용자 코드를 표시할 때 하이픈을 추가하지 않는다")
    void formatted_returnsFiveCharactersWithoutHyphen() {
        // given
        UserCode code = new UserCode("ABC23");

        // when & then
        assertThat(code.formatted()).isEqualTo("ABC23");
    }
}

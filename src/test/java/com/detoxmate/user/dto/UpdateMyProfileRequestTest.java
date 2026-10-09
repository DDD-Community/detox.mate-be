package com.detoxmate.user.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateMyProfileRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @ParameterizedTest
    @MethodSource("validDisplayNames")
    @DisplayName("프로필 이름은 이모지와 특수문자를 포함해 유니코드 코드 포인트 5개까지 허용한다")
    void displayName_acceptsUpToFiveCodePoints(String displayName) {
        // given
        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setDisplayName(displayName);

        // when & then
        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("overlongDisplayNames")
    @DisplayName("프로필 이름이 유니코드 코드 포인트 5개를 초과하면 거절한다")
    void displayName_rejectsMoreThanFiveCodePoints(String displayName) {
        // given
        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setDisplayName(displayName);

        // when & then
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", " 의진", "의 진", "의진 ", "의\t진", "의진\n", "의\u00a0진", "의\u2003진", "의\u3000진"})
    @DisplayName("빈 프로필 이름과 유니코드 공백이 포함된 이름은 거절한다")
    void displayName_rejectsEmptyOrWhitespace(String displayName) {
        // given
        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setDisplayName(displayName);

        // when & then
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("프로필 이름의 null 값은 이름을 유지하는 부분 수정 요청으로 허용한다")
    void displayName_acceptsNullForPartialUpdate() {
        // given
        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setDisplayName(null);

        // when & then
        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    private static Stream<String> validDisplayNames() {
        return Stream.of("의진", "12345", "😀😁😂😃😄", "!@#$%", "👍🏽!@#", "\u1100\u1161\u1100\u1161!");
    }

    private static Stream<String> overlongDisplayNames() {
        return Stream.of("123456", "😀😁😂😃😄😅", "👍🏽".repeat(3), "👨‍👩‍👧‍👦", "\u1100\u1161".repeat(3));
    }
}

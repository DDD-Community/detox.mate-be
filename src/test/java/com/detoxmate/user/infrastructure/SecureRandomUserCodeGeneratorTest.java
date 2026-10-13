package com.detoxmate.user.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecureRandomUserCodeGeneratorTest {

    @Test
    @DisplayName("새 사용자 코드는 허용된 대문자와 숫자로 구성된 5자리다")
    void generate_returnsFiveCharacterCanonicalCode() {
        // given
        SecureRandomUserCodeGenerator generator = new SecureRandomUserCodeGenerator();

        // when
        String code = generator.generate().value();

        // then
        assertThat(code).hasSize(5).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{5}");
    }
}

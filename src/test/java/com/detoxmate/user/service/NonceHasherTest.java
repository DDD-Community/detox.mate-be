package com.detoxmate.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NonceHasherTest {

    @Test
    @DisplayName("nonce를 SHA-256 UTF-8 lowercase hex 규칙으로 해시한다")
    void sha256Hex_returnsLowercaseHexHash() {
        assertThat(NonceHasher.sha256Hex("apple-login-nonce-test"))
                .isEqualTo("a945dc4edee66edc03ce5d5b19f04cac8f01a5a034663d3be93b1201481121aa");
    }
}

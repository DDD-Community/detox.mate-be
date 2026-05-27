package com.detoxmate.user.service;

import com.detoxmate.user.config.ProviderTokenEncryptionProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderTokenCipherTest {

    @Test
    void provider_refresh_token을_암호화하고_복호화한다() {
        // given
        String key = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));
        ProviderTokenCipher providerTokenCipher = new ProviderTokenCipher(
                new ProviderTokenEncryptionProperties(key)
        );

        // when
        String encrypted = providerTokenCipher.encrypt("apple-refresh-token");
        String decrypted = providerTokenCipher.decrypt(encrypted);

        // then
        assertThat(encrypted).doesNotContain("apple-refresh-token");
        assertThat(decrypted).isEqualTo("apple-refresh-token");
    }
}

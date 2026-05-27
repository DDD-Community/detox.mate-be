package com.detoxmate.user.service;

import com.detoxmate.user.config.ProviderTokenEncryptionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class ProviderTokenCipher {

    private final ProviderTokenEncryptionProperties properties;

    @Autowired
    public ProviderTokenCipher(ProviderTokenEncryptionProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Provider refresh token response is invalid");
        }

        try {
            byte[] encrypted = bytesEncryptor().encrypt(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Provider token encryption failed", exception);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Provider refresh token is missing");
        }

        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedText);
            return new String(bytesEncryptor().decrypt(encrypted), StandardCharsets.UTF_8);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Provider token decryption failed", exception);
        }
    }

    private BytesEncryptor bytesEncryptor() {
        return new AesBytesEncryptor(
                secretKey(),
                KeyGenerators.secureRandom(16),
                AesBytesEncryptor.CipherAlgorithm.GCM
        );
    }

    private SecretKey secretKey() {
        if (properties.key() == null || properties.key().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Provider token encryption key is not configured");
        }

        byte[] key;
        try {
            key = Base64.getDecoder().decode(properties.key());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Provider token encryption key is invalid", exception);
        }

        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Provider token encryption key length is invalid");
        }

        return new SecretKeySpec(key, "AES");
    }
}

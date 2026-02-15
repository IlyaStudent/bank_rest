package com.example.bankcards.util;

import com.example.bankcards.exception.EncryptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;

@DisplayName("KeyGeneratorUtil unit tests")
class KeyGeneratorUtilTest {

    @Nested
    @DisplayName("generateKey")
    class GenerateKey {

        @Test
        @DisplayName("Should return a non‑null Base64 string")
        void shouldReturnNonNullBase64String() {
            String key = KeyGeneratorUtil.generateKey();
            assertThat(key).isNotNull();
        }

        @Test
        @DisplayName("Should generate a 32‑byte key encoded in Base64")
        void shouldGenerate32ByteKey() {
            String key = KeyGeneratorUtil.generateKey();
            byte[] decoded = Base64.getDecoder().decode(key);
            assertThat(decoded).hasSize(32);
        }

        @Test
        @DisplayName("Should throw EncryptionException when key generation fails")
        void shouldThrowException_whenKeyGenerationFails() {
            try (MockedStatic<KeyGenerator> mocked = Mockito.mockStatic(KeyGenerator.class)) {
                mocked.when(() -> KeyGenerator.getInstance(anyString()))
                        .thenThrow(new NoSuchAlgorithmException("Test exception"));

                assertThatThrownBy(KeyGeneratorUtil::generateKey)
                        .isInstanceOf(EncryptionException.class);
            }
        }
    }
}
package com.example.bankcards.util;

import com.example.bankcards.exception.EncryptionException;
import org.junit.jupiter.api.*;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EncryptionUtil unit tests")
class EncryptionUtilTest {

    private static final String VALID_BASE64_KEY = EncryptionUtil.generateKey();

    @Nested
    @DisplayName("generateKey")
    class GenerateKey {

        @Test
        @DisplayName("Should return a non‑null Base64 string")
        void shouldReturnNonNullBase64String() {
            String key = EncryptionUtil.generateKey();
            assertThat(key).isNotNull();
        }

        @Test
        @DisplayName("Should generate a 32‑byte key encoded in Base64")
        void shouldGenerate32ByteKey() {
            String key = EncryptionUtil.generateKey();
            byte[] decoded = Base64.getDecoder().decode(key);
            assertThat(decoded).hasSize(32);
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("Should create instance with valid 32‑byte Base64 key")
        void shouldCreateInstanceWithValidKey() {
            String validKey = EncryptionUtil.generateKey();
            EncryptionUtil util = new EncryptionUtil(validKey);
            assertThat(util).isNotNull();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when key is not 32 bytes after decoding")
        void shouldThrowException_whenKeyHasInvalidLength() {
            String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
            assertThatThrownBy(() -> new EncryptionUtil(shortKey))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when key is not valid Base64")
        void shouldThrowException_whenKeyIsInvalidBase64() {
            String invalidBase64 = "not-base64!";
            assertThatThrownBy(() -> new EncryptionUtil(invalidBase64))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("encrypt and decrypt")
    class EncryptDecrypt {

        private EncryptionUtil encryptionUtil;

        @BeforeEach
        void setUp() {
            encryptionUtil = new EncryptionUtil(VALID_BASE64_KEY);
        }

        @Test
        @DisplayName("Should encrypt and decrypt back to original string")
        void shouldEncryptAndDecrypt() {
            String original = "sensitive data";
            String encrypted = encryptionUtil.encrypt(original);
            String decrypted = encryptionUtil.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(original);
        }

        @Test
        @DisplayName("Should produce different ciphertext for same plaintext (due to random IV)")
        void shouldProduceDifferentCiphertexts() {
            String plaintext = "constant";
            String encrypted1 = encryptionUtil.encrypt(plaintext);
            String encrypted2 = encryptionUtil.encrypt(plaintext);
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            String original = "";
            String encrypted = encryptionUtil.encrypt(original);
            String decrypted = encryptionUtil.decrypt(encrypted);
            assertThat(decrypted).isEmpty();
        }

        @Test
        @DisplayName("Should handle very long string")
        void shouldHandleLongString() {
            String original = "a".repeat(10_000);
            String encrypted = encryptionUtil.encrypt(original);
            String decrypted = encryptionUtil.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(original);
        }

        @Test
        @DisplayName("Should handle special characters")
        void shouldHandleSpecialCharacters() {
            String original = "!@#$%^&*()_+{}:\"|<>?~";
            String encrypted = encryptionUtil.encrypt(original);
            String decrypted = encryptionUtil.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(original);
        }

        @Test
        @DisplayName("Should throw EncryptionException when decrypting corrupted data")
        void shouldThrowException_whenDecryptingCorruptedData() {
            String encrypted = encryptionUtil.encrypt("valid");
            String corrupted = encrypted.substring(0, encrypted.length() - 2);
            assertThatThrownBy(() -> encryptionUtil.decrypt(corrupted))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("Should throw EncryptionException when encrypting null")
        void shouldThrowException_whenEncryptingNull() {
            assertThatThrownBy(() -> encryptionUtil.encrypt(null))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("Should throw EncryptionException when decrypting null")
        void shouldThrowException_whenDecryptingNull() {
            assertThatThrownBy(() -> encryptionUtil.decrypt(null))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("Should throw EncryptionException when decrypting garbage")
        void shouldThrowException_whenDecryptingGarbage() {
            String garbage = "not an encrypted string";
            assertThatThrownBy(() -> encryptionUtil.decrypt(garbage))
                    .isInstanceOf(EncryptionException.class);
        }
    }
}
package com.example.bankcards.util;

import com.example.bankcards.exception.EncryptionException;
import lombok.NoArgsConstructor;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

@NoArgsConstructor
public class KeyGeneratorUtil {
    private static final String ALGORITHM = "AES";
    private static final int AES_KEY_SIZE = 256;

    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(AES_KEY_SIZE, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw EncryptionException.encryptionFailed(e);
        }
    }
}

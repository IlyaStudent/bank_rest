package com.example.bankcards.config;

import com.example.bankcards.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {
    @Bean
    public EncryptionUtil encryptionUtil(@Value("${app.encryption.key}") String encryptionKey) {
        return new EncryptionUtil(encryptionKey);
    }
}

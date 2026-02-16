package com.example.bankcards.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTokenServiceImpl redisTokenService;

    @BeforeEach
    void setUp() {
        redisTokenService = new RedisTokenServiceImpl(redisTemplate);
    }

    @Nested
    @DisplayName("saveRefreshToken")
    class SaveRefreshToken {

        @Test
        @DisplayName("Should save token with correct key, value and TTL")
        void shouldSaveTokenWithCorrectParams() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            redisTokenService.saveRefreshToken("token-123", 1L, 604800000L);

            verify(valueOperations).set(
                    "refresh:token-123",
                    "1",
                    604800000L,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    @Nested
    @DisplayName("getUserIdByRefreshToken")
    class GetUserIdByRefreshToken {

        @Test
        @DisplayName("Should return userId when token found")
        void shouldReturnUserIdWhenTokenFound() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("refresh:token-123")).thenReturn("42");

            Long result = redisTokenService.getUserIdByRefreshToken("token-123");

            assertThat(result).isEqualTo(42L);
        }

        @Test
        @DisplayName("Should return null when token not found")
        void shouldReturnNullWhenTokenNotFound() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("refresh:unknown")).thenReturn(null);

            Long result = redisTokenService.getUserIdByRefreshToken("unknown");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("deleteRefreshToken")
    class DeleteRefreshToken {

        @Test
        @DisplayName("Should delete token with correct key")
        void shouldDeleteTokenWithCorrectKey() {
            redisTokenService.deleteRefreshToken("token-123");

            verify(redisTemplate).delete("refresh:token-123");
        }
    }

    @Nested
    @DisplayName("blacklistAccessToken")
    class BlacklistAccessToken {

        @Test
        @DisplayName("Should save to blacklist when TTL is positive")
        void shouldSaveToBlacklistWhenTtlPositive() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            redisTokenService.blacklistAccessToken("jti-123", 300000L);

            verify(valueOperations).set(
                    "blacklist:jti-123",
                    "1",
                    300000L,
                    TimeUnit.MILLISECONDS
            );
        }

        @Test
        @DisplayName("Should not save to blacklist when TTL is zero or negative")
        void shouldNotSaveWhenTtlNotPositive() {
            redisTokenService.blacklistAccessToken("jti-123", 0L);
            redisTokenService.blacklistAccessToken("jti-456", -1000L);

            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Nested
    @DisplayName("isAccessTokenBlackListed")
    class IsAccessTokenBlackListed {

        @Test
        @DisplayName("Should delegate to hasKey with correct blacklist key")
        void shouldDelegateToHasKey() {
            when(redisTemplate.hasKey("blacklist:jti-123")).thenReturn(true);

            Boolean result = redisTokenService.isAccessTokenBlackListed("jti-123");

            assertThat(result).isTrue();
            verify(redisTemplate).hasKey("blacklist:jti-123");
        }
    }
}

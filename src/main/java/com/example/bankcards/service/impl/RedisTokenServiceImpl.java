package com.example.bankcards.service.impl;

import com.example.bankcards.service.RedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisTokenServiceImpl implements RedisTokenService {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void saveRefreshToken(String token, Long userId, Long ttlMillis) {
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + token,
                userId.toString(),
                ttlMillis,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public Long getUserIdByRefreshToken(String token) {
        String value = redisTemplate.opsForValue().get(REFRESH_PREFIX  + token);
        return value != null ? Long.parseLong(value) : null;
    }

    @Override
    public void deleteRefreshToken(String token) {
        redisTemplate.delete(REFRESH_PREFIX + token);
    }

    @Override
    public void blacklistAccessToken(String jti, Long ttlMillis) {
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "1",
                    ttlMillis,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    @Override
    public Boolean isAccessTokenBlackListed(String jti) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
    }
}

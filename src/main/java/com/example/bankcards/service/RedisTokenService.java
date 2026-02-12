package com.example.bankcards.service;

public interface RedisTokenService {
    void saveRefreshToken(String token, Long userId, Long ttlMillis);

    Long getUserIdByRefreshToken(String token);

    void deleteRefreshToken(String token);

    void blacklistAccessToken(String jti, Long ttlMillis);

    Boolean isAccessTokenBlackListed(String jti);
}

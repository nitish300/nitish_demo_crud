package com.example.crud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String REFRESH_PREFIX = "jwt:refresh:";

    /**
     * Blacklists an access token in Redis with a TTL matching its remaining validity.
     *
     * @param token the JWT access token to blacklist
     * @param remainingTtlMillis remaining lifetime in milliseconds
     */
    public void blacklistAccessToken(String token, long remainingTtlMillis) {
        if (remainingTtlMillis <= 0) {
            return;
        }
        try {
            String key = BLACKLIST_PREFIX + token;
            stringRedisTemplate.opsForValue().set(key, "revoked", remainingTtlMillis, TimeUnit.MILLISECONDS);
            log.info("Token successfully blacklisted in Redis for {} ms", remainingTtlMillis);
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis: {}", e.getMessage());
        }
    }

    /**
     * Checks if an access token is in the Redis blacklist.
     *
     * @param token the JWT access token
     * @return true if blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.error("Failed to check token blacklist in Redis: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Saves a user's refresh token in Redis with a TTL.
     *
     * @param email the user's email
     * @param refreshToken the refresh token string
     * @param ttlMillis time-to-live in milliseconds
     */
    public void saveRefreshToken(String email, String refreshToken, long ttlMillis) {
        try {
            String key = REFRESH_PREFIX + email;
            stringRedisTemplate.opsForValue().set(key, refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
            log.info("Refresh token stored in Redis for user '{}'", email);
        } catch (Exception e) {
            log.error("Failed to save refresh token in Redis for user '{}': {}", email, e.getMessage());
        }
    }

    /**
     * Retrieves the stored refresh token for a user from Redis.
     *
     * @param email the user's email
     * @return stored refresh token or null
     */
    public String getRefreshToken(String email) {
        try {
            String key = REFRESH_PREFIX + email;
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to fetch refresh token from Redis for user '{}': {}", email, e.getMessage());
            return null;
        }
    }

    /**
     * Deletes the user's refresh token from Redis upon logout.
     *
     * @param email the user's email
     */
    public void deleteRefreshToken(String email) {
        try {
            String key = REFRESH_PREFIX + email;
            Boolean deleted = stringRedisTemplate.delete(key);
            log.info("Refresh token for user '{}' removed from Redis: {}", email, deleted);
        } catch (Exception e) {
            log.error("Failed to delete refresh token from Redis for user '{}': {}", email, e.getMessage());
        }
    }
}

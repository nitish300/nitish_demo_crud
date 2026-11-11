package com.example.crud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisTokenService redisTokenService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void blacklistAccessToken_Success() {
        String token = "sample-jwt-token";
        long remainingTtl = 50000L;

        redisTokenService.blacklistAccessToken(token, remainingTtl);

        verify(valueOperations).set("jwt:blacklist:" + token, "revoked", remainingTtl, TimeUnit.MILLISECONDS);
    }

    @Test
    void isTokenBlacklisted_ReturnsTrueWhenKeyExists() {
        String token = "sample-jwt-token";
        when(stringRedisTemplate.hasKey("jwt:blacklist:" + token)).thenReturn(true);

        boolean blacklisted = redisTokenService.isTokenBlacklisted(token);

        assertTrue(blacklisted);
    }

    @Test
    void isTokenBlacklisted_ReturnsFalseWhenKeyDoesNotExist() {
        String token = "sample-jwt-token";
        when(stringRedisTemplate.hasKey("jwt:blacklist:" + token)).thenReturn(false);

        boolean blacklisted = redisTokenService.isTokenBlacklisted(token);

        assertFalse(blacklisted);
    }

    @Test
    void saveRefreshToken_Success() {
        String email = "nitish@example.com";
        String refreshToken = "sample-refresh-token";
        long ttl = 604800000L;

        redisTokenService.saveRefreshToken(email, refreshToken, ttl);

        verify(valueOperations).set("jwt:refresh:" + email, refreshToken, ttl, TimeUnit.MILLISECONDS);
    }

    @Test
    void getRefreshToken_Success() {
        String email = "nitish@example.com";
        when(valueOperations.get("jwt:refresh:" + email)).thenReturn("saved-token");

        String result = redisTokenService.getRefreshToken(email);

        assertEquals("saved-token", result);
    }

    @Test
    void deleteRefreshToken_Success() {
        String email = "nitish@example.com";

        redisTokenService.deleteRefreshToken(email);

        verify(stringRedisTemplate).delete("jwt:refresh:" + email);
    }
}

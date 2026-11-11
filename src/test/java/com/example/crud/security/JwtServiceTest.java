package com.example.crud.security;

import com.example.crud.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set properties via ReflectionTestUtils
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 900000L); // 15 mins
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 604800000L); // 7 days

        User user = new User();
        user.setId(1L);
        user.setEmail("nitish@example.com");
        user.setPassword("encodedPassword");
        user.setRole("ROLE_USER");
        userDetails = new CustomUserDetails(user);
    }

    @Test
    void generateAccessToken_ValidToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "ROLE_USER");

        String token = jwtService.generateAccessToken(userDetails, claims);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("nitish@example.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertTrue(jwtService.getRemainingTtl(token) > 0);
    }

    @Test
    void generateRefreshToken_ValidToken() {
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        assertNotNull(refreshToken);
        assertEquals("nitish@example.com", jwtService.extractUsername(refreshToken));
        assertTrue(jwtService.isTokenValid(refreshToken, userDetails));
    }

    @Test
    void isTokenValid_InvalidUser_ReturnsFalse() {
        String token = jwtService.generateAccessToken(userDetails, new HashMap<>());

        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        CustomUserDetails otherDetails = new CustomUserDetails(otherUser);

        assertFalse(jwtService.isTokenValid(token, otherDetails));
    }
}

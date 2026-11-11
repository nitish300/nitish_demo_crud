package com.example.crud.service;

import com.example.crud.dto.AuthResponseDTO;
import com.example.crud.dto.LoginRequestDTO;
import com.example.crud.dto.RefreshTokenRequestDTO;
import com.example.crud.dto.RegisterRequestDTO;
import com.example.crud.entity.User;
import com.example.crud.repository.UserRepository;
import com.example.crud.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RedisTokenService redisTokenService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("nitish@example.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setFirstName("Nitish");
        sampleUser.setLastName("Kumar");
        sampleUser.setRole("ROLE_USER");

        registerRequest = new RegisterRequestDTO();
        registerRequest.setFirstName("Nitish");
        registerRequest.setLastName("Kumar");
        registerRequest.setEmail("nitish@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAge(25);

        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("nitish@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail("nitish@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("mockAccessToken");
        when(jwtService.generateRefreshToken(any())).thenReturn("mockRefreshToken");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponseDTO response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
        assertEquals("nitish@example.com", response.getEmail());

        verify(redisTokenService).saveRefreshToken("nitish@example.com", "mockRefreshToken", 604800000L);
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail("nitish@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("nitish@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("mockAccessToken");
        when(jwtService.generateRefreshToken(any())).thenReturn("mockRefreshToken");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponseDTO response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
        verify(redisTokenService).saveRefreshToken("nitish@example.com", "mockRefreshToken", 604800000L);
    }

    @Test
    void refreshToken_Success() {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("validRefreshToken");
        when(jwtService.extractUsername("validRefreshToken")).thenReturn("nitish@example.com");
        when(redisTokenService.getRefreshToken("nitish@example.com")).thenReturn("validRefreshToken");
        when(userRepository.findByEmail("nitish@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.isTokenValid(eq("validRefreshToken"), any())).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("newAccessToken");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponseDTO response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
    }

    @Test
    void logout_RevokesTokensInRedis() {
        String authHeader = "Bearer mockAccessToken";
        when(jwtService.extractUsername("mockAccessToken")).thenReturn("nitish@example.com");
        when(jwtService.getRemainingTtl("mockAccessToken")).thenReturn(300000L);

        authService.logout(authHeader);

        verify(redisTokenService).blacklistAccessToken("mockAccessToken", 300000L);
        verify(redisTokenService).deleteRefreshToken("nitish@example.com");
    }
}

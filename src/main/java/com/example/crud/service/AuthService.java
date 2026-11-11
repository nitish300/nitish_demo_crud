package com.example.crud.service;

import com.example.crud.dto.*;
import com.example.crud.entity.User;
import com.example.crud.exception.ResourceNotFoundException;
import com.example.crud.repository.UserRepository;
import com.example.crud.security.CustomUserDetails;
import com.example.crud.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTokenService redisTokenService;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered: " + request.getEmail());
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAge(request.getAge());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCity(request.getCity());
        user.setCountry(request.getCountry());
        user.setAddress(request.getAddress());
        user.setDateOfBirth(request.getDateOfBirth());

        String role = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole()
                : "ROLE_USER";
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        user.setRole(role);

        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", savedUser.getId());
        extraClaims.put("role", savedUser.getRole());

        String accessToken = jwtService.generateAccessToken(userDetails, extraClaims);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Store refresh token in Redis with TTL
        redisTokenService.saveRefreshToken(savedUser.getEmail(), refreshToken, jwtService.getRefreshTokenExpirationMs());

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole());

        String accessToken = jwtService.generateAccessToken(userDetails, extraClaims);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Store / update refresh token in Redis
        redisTokenService.saveRefreshToken(user.getEmail(), refreshToken, jwtService.getRefreshTokenExpirationMs());

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String token = request.getRefreshToken();
        String userEmail = jwtService.extractUsername(token);

        if (userEmail == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Validate refresh token against Redis storage
        String storedRefreshToken = redisTokenService.getRefreshToken(userEmail);
        if (storedRefreshToken == null || !storedRefreshToken.equals(token)) {
            throw new RuntimeException("Refresh token is expired, revoked, or not found in Redis");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new RuntimeException("Refresh token is invalid or expired");
        }

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole());

        String newAccessToken = jwtService.generateAccessToken(userDetails, extraClaims);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String accessToken = authHeader.substring(7);
        try {
            String userEmail = jwtService.extractUsername(accessToken);
            long remainingTtl = jwtService.getRemainingTtl(accessToken);

            // Blacklist the access token in Redis
            redisTokenService.blacklistAccessToken(accessToken, remainingTtl);

            // Delete the refresh token from Redis
            if (userEmail != null) {
                redisTokenService.deleteRefreshToken(userEmail);
            }
            log.info("User '{}' successfully logged out, tokens revoked in Redis", userEmail);
        } catch (Exception e) {
            log.warn("Error during logout token revocation: {}", e.getMessage());
        }
    }

    public UserDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setAge(user.getAge());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setCity(user.getCity());
        dto.setCountry(user.getCountry());
        dto.setAddress(user.getAddress());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}

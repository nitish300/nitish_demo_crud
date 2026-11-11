package com.example.crud.controller;

import com.example.crud.dto.AuthResponseDTO;
import com.example.crud.dto.LoginRequestDTO;
import com.example.crud.dto.RefreshTokenRequestDTO;
import com.example.crud.dto.RegisterRequestDTO;
import com.example.crud.exception.GlobalExceptionHandler;
import com.example.crud.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private AuthResponseDTO sampleAuthResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleAuthResponse = AuthResponseDTO.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .userId(1L)
                .email("nitish@example.com")
                .firstName("Nitish")
                .lastName("Kumar")
                .role("ROLE_USER")
                .build();
    }

    @Test
    void register_Returns201() throws Exception {
        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(sampleAuthResponse);

        String jsonPayload = """
                {
                    "firstName": "Nitish",
                    "lastName": "Kumar",
                    "email": "nitish@example.com",
                    "password": "password123",
                    "age": 25
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.email").value("nitish@example.com"));
    }

    @Test
    void login_Returns200() throws Exception {
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(sampleAuthResponse);

        String jsonPayload = """
                {
                    "email": "nitish@example.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"));
    }

    @Test
    void refreshToken_Returns200() throws Exception {
        when(authService.refreshToken(any(RefreshTokenRequestDTO.class))).thenReturn(sampleAuthResponse);

        String jsonPayload = """
                {
                    "refreshToken": "mock-refresh-token"
                }
                """;

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"));
    }

    @Test
    void logout_Returns200() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer mock-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User successfully logged out and token invalidated in Redis"));

        verify(authService).logout("Bearer mock-access-token");
    }

    @Test
    void test_Returns200() throws Exception {
        mockMvc.perform(get("/api/auth/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth public endpoint is working!"));
    }
}

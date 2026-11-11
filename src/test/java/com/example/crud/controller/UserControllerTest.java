package com.example.crud.controller;

import com.example.crud.dto.UserDTO;
import com.example.crud.dto.UserRequestDTO;
import com.example.crud.exception.GlobalExceptionHandler;
import com.example.crud.exception.ResourceNotFoundException;
import com.example.crud.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserDTO sampleUserDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleUserDTO = new UserDTO();
        sampleUserDTO.setId(1L);
        sampleUserDTO.setFirstName("Nitish");
        sampleUserDTO.setLastName("Kumar");
        sampleUserDTO.setEmail("nitish@example.com");
        sampleUserDTO.setAge(25);
        sampleUserDTO.setCity("Delhi");
    }

    @Test
    void createUser_Returns201() throws Exception {
        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(sampleUserDTO);

        String jsonPayload = """
                {
                    "firstName": "Nitish",
                    "lastName": "Kumar",
                    "email": "nitish@example.com",
                    "age": 25,
                    "city": "Delhi"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("Nitish"));
    }

    @Test
    void getAllUsers_Returns200() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sampleUserDTO));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Nitish"));
    }

    @Test
    void getUserById_Found_Returns200() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleUserDTO);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getUserById_NotFound_Returns404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void updateUser_Returns200() throws Exception {
        when(userService.updateUser(eq(1L), any(UserRequestDTO.class))).thenReturn(sampleUserDTO);

        String jsonPayload = """
                {
                    "firstName": "Nitish",
                    "lastName": "Kumar",
                    "email": "nitish@example.com",
                    "age": 25,
                    "city": "Delhi"
                }
                """;

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nitish@example.com"));
    }

    @Test
    void deleteUser_Returns200() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/users/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Controller is working!"));
    }
}

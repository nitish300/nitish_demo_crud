package com.example.crud.service;

import com.example.crud.dto.UserDTO;
import com.example.crud.dto.UserRequestDTO;
import com.example.crud.entity.User;
import com.example.crud.exception.ResourceNotFoundException;
import com.example.crud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private UserRequestDTO sampleRequestDTO;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setFirstName("Nitish");
        sampleUser.setLastName("Kumar");
        sampleUser.setEmail("nitish@example.com");
        sampleUser.setAge(25);
        sampleUser.setCity("Delhi");

        sampleRequestDTO = new UserRequestDTO();
        sampleRequestDTO.setFirstName("Nitish");
        sampleRequestDTO.setLastName("Kumar");
        sampleRequestDTO.setEmail("nitish@example.com");
        sampleRequestDTO.setAge(25);
        sampleRequestDTO.setCity("Delhi");
    }

    @Test
    void createUser_Success() {
        when(userRepository.existsByEmail("nitish@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserDTO result = userService.createUser(sampleRequestDTO);

        assertNotNull(result);
        assertEquals("Nitish", result.getFirstName());
        assertEquals("nitish@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail("nitish@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.createUser(sampleRequestDTO));

        assertTrue(exception.getMessage().contains("Email already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserDTO result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("nitish@example.com", result.getEmail());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void getAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserDTO> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("Nitish", result.get(0).getFirstName());
    }

    @Test
    void updateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmailAndIdNot("nitish@example.com", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserDTO result = userService.updateUser(1L, sampleRequestDTO);

        assertNotNull(result);
        assertEquals("nitish@example.com", result.getEmail());
    }

    @Test
    void updateUser_DuplicateEmail_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmailAndIdNot("nitish@example.com", 1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.updateUser(1L, sampleRequestDTO));
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        assertDoesNotThrow(() -> userService.deleteUser(1L));
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_NotFound_ThrowsException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void searchUsersByName_Success() {
        when(userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("Nitish", "Nitish"))
                .thenReturn(List.of(sampleUser));

        List<UserDTO> result = userService.searchUsersByName("Nitish");

        assertEquals(1, result.size());
        assertEquals("Nitish", result.get(0).getFirstName());
    }
}

package com.example.crud.service;

import com.example.crud.dto.UserDTO;
import com.example.crud.dto.UserRequestDTO;
import com.example.crud.entity.User;
import com.example.crud.exception.ResourceNotFoundException;
import com.example.crud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO createUser(UserRequestDTO userRequestDTO) {
        if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
            throw new RuntimeException("Email already exists: " + userRequestDTO.getEmail());
        }

        User user = new User();
        mapDtoToEntity(userRequestDTO, user);

        if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        }

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return convertToDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserRequestDTO userRequestDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if another user already has the updated email
        if (userRepository.existsByEmailAndIdNot(userRequestDTO.getEmail(), id)) {
            throw new RuntimeException("Email is already taken by another user: " + userRequestDTO.getEmail());
        }

        mapDtoToEntity(userRequestDTO, existingUser);

        if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        }

        User updatedUser = userRepository.save(existingUser);
        return convertToDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByCity(String city) {
        return userRepository.findByCityIgnoreCase(city)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> searchUsersByName(String query) {
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private void mapDtoToEntity(UserRequestDTO dto, User user) {
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setAge(dto.getAge());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setCity(dto.getCity());
        user.setCountry(dto.getCountry());
        user.setAddress(dto.getAddress());
        user.setDateOfBirth(dto.getDateOfBirth());

        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            String role = dto.getRole();
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }
            user.setRole(role);
        } else if (user.getRole() == null) {
            user.setRole("ROLE_USER");
        }
    }

    private UserDTO convertToDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setEmail(user.getEmail());
        userDTO.setRole(user.getRole());
        userDTO.setAge(user.getAge());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setCity(user.getCity());
        userDTO.setCountry(user.getCountry());
        userDTO.setAddress(user.getAddress());
        userDTO.setDateOfBirth(user.getDateOfBirth());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setUpdatedAt(user.getUpdatedAt());
        return userDTO;
    }
}
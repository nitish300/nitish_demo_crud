package com.example.crud.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be greater than 0")
    private Integer age;

    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;

    @Size(max = 50)
    private String city;

    @Size(max = 50)
    private String country;

    @Size(max = 255)
    private String address;

    private LocalDate dateOfBirth;

    private String role; // Optional, defaults to "ROLE_USER"
}

package com.fennixs.auth.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        String setupToken,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, max = 128) String password) {}
